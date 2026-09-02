import os
import json
import time
from collections import defaultdict
from typing import Optional, List, Dict, Any
import requests
from fastapi import FastAPI, Query, HTTPException, BackgroundTasks
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="DIU Live Routine Portal", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.join(BASE_DIR, "data")
STATIC_DIR = os.path.join(BASE_DIR, "static")
os.makedirs(DATA_DIR, exist_ok=True)

DIU_BASE_URL = "https://routine.diuhub.bd"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
    "Accept": "*/*",
    "Referer": "https://routine.diuhub.bd/",
    "RSC": "1"
}

# Live in-memory cache
departments_data = []
batches_data = []
sections_data = []
all_routine_entries = []
routines_by_section = {}
routines_by_room = {}
teachers_index = {}
section_map = {}

last_sync_timestamp = 0
last_sync_status = "Not synced yet"
is_syncing = False

WEEKDAY_NAMES = {
    0: "Sunday",
    1: "Monday",
    2: "Tuesday",
    3: "Wednesday",
    4: "Thursday",
    5: "Friday",
    6: "Saturday"
}

def minutes_to_time(m):
    if m is None:
        return ""
    h = m // 60
    mins = m % 60
    period = "AM" if h < 12 else "PM"
    display_h = h if h <= 12 else h - 12
    if display_h == 0:
        display_h = 12
    return f"{display_h}:{mins:02d} {period}"

def extract_balanced_json(raw_text: str, search_key: str):
    idx = raw_text.find(search_key)
    if idx == -1:
        return None
    raw_sub = raw_text[idx:]
    brace_count = 0
    in_string = False
    escape = False
    end_idx = -1
    for i, ch in enumerate(raw_sub):
        if ch == '"' and not escape:
            in_string = not in_string
        elif not in_string:
            if ch == '{':
                brace_count += 1
            elif ch == '}':
                brace_count -= 1
                if brace_count == 0:
                    end_idx = i + 1
                    break
        escape = (ch == '\\' and not escape)

    if end_idx != -1:
        clean_json_str = raw_sub[:end_idx].replace('"$D', '"')
        return json.loads(clean_json_str)
    return None

def sync_live_diu_server():
    """Fetches real-time RSC flight streams directly from routine.diuhub.bd and refreshes data"""
    global departments_data, batches_data, sections_data
    global all_routine_entries, routines_by_section, routines_by_room, teachers_index, section_map
    global last_sync_timestamp, last_sync_status, is_syncing

    if is_syncing:
        return {"status": "Sync already in progress"}
    is_syncing = True

    try:
        print("[*] Contacting LIVE server at https://routine.diuhub.bd ...")
        # 1. Fetch Metadata (Departments, Batches, Sections)
        r_meta = requests.get(f"{DIU_BASE_URL}/routine?_rsc=1pb18", headers=HEADERS, timeout=20)
        if r_meta.status_code == 200:
            meta_json = extract_balanced_json(r_meta.text, '{"teachers":') or extract_balanced_json(r_meta.text, '{"departments":')
            if meta_json:
                departments_data = meta_json.get("departments", departments_data)
                batches_data = meta_json.get("batches", batches_data)
                sections_data = meta_json.get("sections", sections_data)
                print(f"[+] Live Metadata synced: {len(departments_data)} departments, {len(sections_data)} sections.")

        # Re-build section lookup table
        temp_sec_map = {}
        for s in sections_data:
            b_name = s.get("batch", {}).get("name", "")
            d_code = s.get("department", {}).get("code", "")
            s_name = s.get("name", "")
            full_tag = f"{d_code}_{b_name}_{s_name}"
            temp_sec_map[s["id"]] = {
                "section_name": s_name,
                "batch": b_name,
                "department": d_code,
                "full_tag": full_tag
            }
        section_map = temp_sec_map

        # 2. Fetch Live Room Schedules & Complete Routine Entries
        r_rooms = requests.get(f"{DIU_BASE_URL}/rooms?_rsc=1pb18", headers=HEADERS, timeout=25)
        if r_rooms.status_code == 200:
            rooms_json = extract_balanced_json(r_rooms.text, '{"rooms":')
            if rooms_json and "rooms" in rooms_json:
                rooms_list = rooms_json["rooms"]
                
                temp_entries = []
                temp_by_sec = defaultdict(list)
                temp_by_room = defaultdict(list)
                temp_teachers = defaultdict(lambda: {"name": "", "initials": "", "classes": []})

                for room in rooms_list:
                    r_name = room.get("name")
                    r_building = room.get("building")
                    for entry in room.get("routineEntries", []):
                        sec_info = section_map.get(entry.get("sectionId"), {})
                        fmt_entry = {
                            "id": entry.get("id"),
                            "weekday": WEEKDAY_NAMES.get(entry.get("weekday"), entry.get("weekday")),
                            "weekday_index": entry.get("weekday"),
                            "startTime": minutes_to_time(entry.get("startTime")),
                            "endTime": minutes_to_time(entry.get("endTime")),
                            "courseCode": entry.get("course", {}).get("code") if entry.get("course") else "",
                            "courseTitle": entry.get("course", {}).get("title") if entry.get("course") else "",
                            "teacherName": entry.get("teacher", {}).get("name") if entry.get("teacher") else "",
                            "teacherInitials": entry.get("teacher", {}).get("initials") if entry.get("teacher") else "",
                            "room": r_name,
                            "building": r_building,
                            "department": sec_info.get("department", ""),
                            "batch": sec_info.get("batch", ""),
                            "section": sec_info.get("section_name", ""),
                            "sectionTag": sec_info.get("full_tag", "")
                        }
                        temp_entries.append(fmt_entry)
                        if fmt_entry["sectionTag"]:
                            temp_by_sec[fmt_entry["sectionTag"]].append(fmt_entry)
                        temp_by_room[r_name].append(fmt_entry)

                        t_init = fmt_entry["teacherInitials"]
                        if t_init:
                            temp_teachers[t_init]["initials"] = t_init
                            if fmt_entry["teacherName"]:
                                temp_teachers[t_init]["name"] = fmt_entry["teacherName"]
                            temp_teachers[t_init]["classes"].append(fmt_entry)

                all_routine_entries = temp_entries
                routines_by_section = dict(temp_by_sec)
                routines_by_room = dict(temp_by_room)
                teachers_index = dict(temp_teachers)

                last_sync_timestamp = int(time.time())
                last_sync_status = "Connected Live to routine.diuhub.bd (Success)"
                print(f"[+] LIVE SYNC COMPLETE: {len(all_routine_entries)} class entries, {len(routines_by_room)} rooms.")
                
                # Persist to disk as cache backup
                with open(os.path.join(DATA_DIR, "all_routine_entries.json"), "w", encoding="utf-8") as f:
                    json.dump(all_routine_entries, f)
                with open(os.path.join(DATA_DIR, "routines_by_section.json"), "w", encoding="utf-8") as f:
                    json.dump(routines_by_section, f)
                with open(os.path.join(DATA_DIR, "departments.json"), "w", encoding="utf-8") as f:
                    json.dump(departments_data, f)
                with open(os.path.join(DATA_DIR, "batches.json"), "w", encoding="utf-8") as f:
                    json.dump(batches_data, f)
                with open(os.path.join(DATA_DIR, "sections.json"), "w", encoding="utf-8") as f:
                    json.dump(sections_data, f)

                return {"status": "success", "entries_count": len(all_routine_entries), "timestamp": last_sync_timestamp}

        last_sync_status = f"Live server replied with error status: {r_rooms.status_code}"
        return {"status": "error", "code": r_rooms.status_code}
    except Exception as e:
        last_sync_status = f"Live sync failed: {str(e)}"
        print("[-] Live Sync Error:", e)
        return {"status": "error", "message": str(e)}
    finally:
        is_syncing = False

# Fallback initial loader from local cache so server starts instantly
def initial_load():
    def read_json(filename):
        p = os.path.join(DATA_DIR, filename)
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                return []
        return []
    
    global departments_data, batches_data, sections_data, all_routine_entries, routines_by_section, routines_by_room, teachers_index
    departments_data = read_json("departments.json")
    batches_data = read_json("batches.json")
    sections_data = read_json("sections.json")
    all_routine_entries = read_json("all_routine_entries.json")
    routines_by_section = read_json("routines_by_section.json") or {}
    routines_by_room = read_json("routines_by_room.json") or {}

    t_map = defaultdict(lambda: {"name": "", "initials": "", "classes": []})
    for e in all_routine_entries:
        init = e.get("teacherInitials")
        if init:
            t_map[init]["initials"] = init
            if e.get("teacherName"):
                t_map[init]["name"] = e["teacherName"]
            t_map[init]["classes"].append(e)
    teachers_index = dict(t_map)

initial_load()

# Perform live sync immediately on startup
try:
    print("[*] Triggering automatic direct sync with routine.diuhub.bd ...")
    sync_live_diu_server()
except Exception as ex:
    print("Initial live sync warning:", ex)

# ---------------- API ENDPOINTS ----------------

@app.post("/api/sync")
@app.get("/api/sync")
def trigger_sync(background_tasks: BackgroundTasks):
    """Directly fetch fresh updates from the live DIU server"""
    background_tasks.add_task(sync_live_diu_server)
    return {
        "status": "Sync triggered with routine.diuhub.bd",
        "last_status": last_sync_status,
        "last_sync": last_sync_timestamp,
        "is_syncing": is_syncing
    }

@app.get("/api/status")
def get_status():
    return {
        "live_server": "https://routine.diuhub.bd",
        "status": last_sync_status,
        "last_sync": last_sync_timestamp,
        "entries_count": len(all_routine_entries),
        "is_syncing": is_syncing
    }

@app.get("/api/stats")
def get_stats():
    return {
        "departments_count": len(departments_data),
        "batches_count": len(batches_data),
        "sections_count": len(sections_data),
        "routine_entries_count": len(all_routine_entries),
        "teachers_count": len(teachers_index),
        "rooms_count": len(routines_by_room),
        "last_sync": last_sync_timestamp,
        "live_status": last_sync_status
    }

@app.get("/api/departments")
def get_departments():
    return departments_data

@app.get("/api/batches")
def get_batches(dept: Optional[str] = None):
    if not dept:
        return batches_data
    dept_obj = next((d for d in departments_data if d["code"].lower() == dept.lower() or d["name"].lower() == dept.lower()), None)
    if not dept_obj:
        return [b for b in batches_data if b.get("department", {}).get("code", "").lower() == dept.lower()]
    dept_id = dept_obj["id"]
    return [b for b in batches_data if b.get("departmentId") == dept_id or b.get("department", {}).get("code", "").lower() == dept.lower()]

@app.get("/api/sections")
def get_sections(dept: Optional[str] = None, batch: Optional[str] = None):
    res = sections_data
    if dept:
        res = [s for s in res if s.get("department", {}).get("code", "").lower() == dept.lower()]
    if batch:
        res = [s for s in res if s.get("batch", {}).get("name", "").lower() == batch.lower()]
    return res

@app.get("/api/routine")
def get_routine(
    dept: Optional[str] = None,
    batch: Optional[str] = None,
    section: Optional[str] = None,
    query: Optional[str] = None
):
    if query:
        q = query.strip().upper()
        if q in routines_by_section:
            return routines_by_section[q]
        for tag, entries in routines_by_section.items():
            if q == tag or tag.endswith(f"_{q}") or f"_{q}_" in tag:
                return entries

    if dept and batch and section:
        tag = f"{dept.upper()}_{batch}_{section.upper()}"
        if tag in routines_by_section:
            return routines_by_section[tag]
        matched = [
            e for e in all_routine_entries 
            if e.get("department", "").lower() == dept.lower()
            and e.get("batch", "").lower() == batch.lower()
            and e.get("section", "").lower() == section.lower()
        ]
        return matched

    if section and "_" in section:
        parts = section.split("_")
        b, s = parts[0], parts[1]
        matched = [
            e for e in all_routine_entries 
            if e.get("batch", "").lower() == b.lower() and e.get("section", "").lower() == s.lower()
        ]
        if matched:
            return matched

    return []

@app.get("/api/teachers")
def get_teachers():
    faculty = []
    for initials, info in sorted(teachers_index.items()):
        faculty.append({
            "initials": initials,
            "name": info["name"] or initials,
            "total_classes": len(info["classes"])
        })
    return faculty

@app.get("/api/teacher/{initials}")
def get_teacher_schedule(initials: str):
    init = initials.upper().strip()
    if init in teachers_index:
        return teachers_index[init]
    for k, v in teachers_index.items():
        if k.lower() == init.lower() or init.lower() in v["name"].lower():
            return v
    raise HTTPException(status_code=404, detail=f"Teacher '{initials}' not found")

@app.get("/api/rooms")
def get_rooms():
    return [
        {"room": r, "total_classes": len(entries)} 
        for r, entries in sorted(routines_by_room.items())
    ]

@app.get("/api/room/{room_name}")
def get_room_schedule(room_name: str):
    r_name = room_name.strip()
    if r_name in routines_by_room:
        return {"room": r_name, "schedule": routines_by_room[r_name]}
    for k, v in routines_by_room.items():
        if k.lower() == r_name.lower():
            return {"room": k, "schedule": v}
    raise HTTPException(status_code=404, detail=f"Room '{room_name}' not found")

@app.get("/api/free-rooms")
def find_free_rooms(weekday: str, time_slot: Optional[str] = None):
    w_clean = weekday.capitalize().strip()
    all_rooms = set(routines_by_room.keys())
    occupied_rooms = set()
    for entry in all_routine_entries:
        if entry.get("weekday", "").lower() == w_clean.lower():
            if time_slot:
                if entry.get("startTime") == time_slot:
                    occupied_rooms.add(entry.get("room"))
            else:
                occupied_rooms.add(entry.get("room"))

    free_rooms = sorted(list(all_rooms - occupied_rooms))
    return {
        "weekday": w_clean,
        "time_slot": time_slot or "All Day",
        "free_rooms_count": len(free_rooms),
        "free_rooms": free_rooms
    }

app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

@app.get("/")
def serve_index():
    return FileResponse(os.path.join(STATIC_DIR, "index.html"))

if __name__ == "__main__":
    import uvicorn
    print("\n" + "="*50)
    print("  >> DIU Live Routine Portal Starting on http://0.0.0.0:8000")
    print("="*50 + "\n")
    uvicorn.run(app, host="0.0.0.0", port=8000)
