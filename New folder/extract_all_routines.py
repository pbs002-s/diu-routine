import json
import os
from collections import defaultdict

ROUTINE_FILE = "extracted_data/rooms_rsc.txt"
CLEAN_DIR = "extracted_data/clean_data"
os.makedirs(CLEAN_DIR, exist_ok=True)

print("[*] Reading rooms and routine entries from RSC data...")
with open(ROUTINE_FILE, "r", encoding="utf-8") as f:
    text = f.read()

# Locate rooms payload line
target_line = None
for line in text.splitlines():
    if '"rooms":[' in line:
        target_line = line
        break

if not target_line:
    print("[-] Could not find rooms line")
    exit(1)

start_idx = target_line.find('{"rooms":')
raw_json = target_line[start_idx:]

# Parse JSON by balancing braces
brace_count = 0
in_string = False
escape = False
end_idx = -1

for i, ch in enumerate(raw_json):
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

if end_idx == -1:
    print("[-] Failed to find end of JSON object")
    exit(1)

cleaned_str = raw_json[:end_idx].replace('"$D', '"')
rooms_data = json.loads(cleaned_str).get("rooms", [])
print(f"[+] Successfully loaded {len(rooms_data)} rooms.")

# Load sections to map sectionId -> Section Name & Batch & Dept
with open(f"{CLEAN_DIR}/sections.json", "r", encoding="utf-8") as f:
    sections_list = json.load(f)

section_map = {}
for s in sections_list:
    b_name = s.get("batch", {}).get("name", "UnknownBatch")
    d_code = s.get("department", {}).get("code", "UnknownDept")
    s_name = s.get("name", "")
    full_tag = f"{d_code}_{b_name}_{s_name}"
    section_map[s["id"]] = {
        "section_name": s_name,
        "batch": b_name,
        "department": d_code,
        "full_tag": full_tag
    }

# Extract all routine entries
all_routine_entries = []
by_section = defaultdict(list)
by_teacher = defaultdict(list)
by_room = defaultdict(list)

# Map weekdays: 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
weekday_names = {
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

for room in rooms_data:
    r_name = room.get("name")
    r_building = room.get("building")
    for entry in room.get("routineEntries", []):
        sec_info = section_map.get(entry.get("sectionId"), {})
        formatted_entry = {
            "id": entry.get("id"),
            "weekday": weekday_names.get(entry.get("weekday"), entry.get("weekday")),
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
        all_routine_entries.append(formatted_entry)
        if formatted_entry["sectionTag"]:
            by_section[formatted_entry["sectionTag"]].append(formatted_entry)
        if formatted_entry["teacherInitials"]:
            by_teacher[formatted_entry["teacherInitials"]].append(formatted_entry)
        by_room[r_name].append(formatted_entry)

print(f"[+] Total Routine Entries Extracted: {len(all_routine_entries)}")

# Save outputs
with open(f"{CLEAN_DIR}/all_routine_entries.json", "w", encoding="utf-8") as f:
    json.dump(all_routine_entries, f, indent=2, ensure_ascii=False)

with open(f"{CLEAN_DIR}/routines_by_section.json", "w", encoding="utf-8") as f:
    json.dump(by_section, f, indent=2, ensure_ascii=False)

with open(f"{CLEAN_DIR}/routines_by_room.json", "w", encoding="utf-8") as f:
    json.dump(by_room, f, indent=2, ensure_ascii=False)

# Look specifically for CSE 68_D
cse_68_d = by_section.get("CSE_68_D", [])
print(f"[+] CSE_68_D Entries Count: {len(cse_68_d)}")
if cse_68_d:
    with open(f"{CLEAN_DIR}/routine_CSE_68_D.json", "w", encoding="utf-8") as f:
        json.dump(cse_68_d, f, indent=2, ensure_ascii=False)
    print("[+] Saved routine_CSE_68_D.json!")
else:
    # Check what sections exist for CSE batch 68
    cse_68_keys = [k for k in by_section.keys() if "68" in k]
    print("Available Batch 68 sections found:", cse_68_keys[:10])

print("[*] All routines and indexes exported to clean_data folder.")
