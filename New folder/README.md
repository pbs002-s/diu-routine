# DIU Routine API & Extractor

This project extracts routine, course, department, batch, and section schedule data from `routine.diuhub.bd`.

## Project Structure

```
diu_routine_extractor/
│
├── extracted_data/
│   ├── clean_data/
│   │   ├── routine_CSE_68_D.json      # Exactly matching the screen in your DevTools (CSE 68_D)
│   │   ├── routines_by_section.json   # All 739 sections mapped to their complete routine
│   │   ├── routines_by_room.json      # All 132 classroom schedules
│   │   ├── all_routine_entries.json   # All 3,265 routine slots
│   │   ├── departments.json           # 27 DIU Departments (CSE, SWE, EEE, etc.)
│   │   ├── batches.json               # All Batches (68, 67, 66, etc.)
│   │   └── sections.json              # All Sections (A, B, C, D, etc.) with internal IDs
│   ├── session.json                   # Auth / Session status
│   └── rooms_rsc.txt                  # Full Next.js RSC payload from server
│
├── extract_all_routines.py            # Complete parser and JSON exporter
├── extract_routine.py                 # Network request fetcher
└── README.md
```

## How the Website API Architecture Works

1. **Next.js App Router & React Server Components (RSC)**:
   - The application does not use traditional REST API endpoints like `/api/routine`.
   - Instead, the server renders the full schedule state directly via **Next.js Server Component streaming** at:
     ```http
     GET https://routine.diuhub.bd/rooms?_rsc=1pb18
     Header: RSC: 1
     ```
2. **Data Structure**:
   - The payload contains relational arrays including `rooms`, `routineEntries`, `course`, `teacher`, `batch`, and `section`.
   - The script `extract_all_routines.py` parses these objects and organizes them cleanly by section, teacher, and room.

## Re-running the Extractor

```bash
python extract_all_routines.py
```
