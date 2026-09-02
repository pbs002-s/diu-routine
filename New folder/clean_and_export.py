import json
import os

with open("extracted_data/routine_rsc.txt", "r", encoding="utf-8") as f:
    text = f.read()

# Locate the RoutinePageClient payload line
target_line = None
for line in text.splitlines():
    if '"departments":' in line and '"batches":' in line and '"sections":' in line:
        target_line = line
        break

if not target_line:
    print("Could not find payload line!")
    exit(1)

# Find the start of the object
start_idx = target_line.find('{"teachers":')
if start_idx == -1:
    start_idx = target_line.find('{')

raw_json = target_line[start_idx:]

# Next.js RSC objects are often wrapped in an array like ["$","$L13",null,{...}]
# Let's cleanly balance braces to extract the valid JSON object
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

if end_idx != -1:
    extracted_str = raw_json[:end_idx]
    # Replace Next.js date wrappers like "$D2026-..." with standard strings
    cleaned_str = extracted_str.replace('"$D', '"')
    data = json.loads(cleaned_str)

    # Save cleanly indented files for the user
    os.makedirs("extracted_data/clean_data", exist_ok=True)
    
    with open("extracted_data/clean_data/departments.json", "w", encoding="utf-8") as f:
        json.dump(data.get("departments", []), f, indent=2, ensure_ascii=False)

    with open("extracted_data/clean_data/batches.json", "w", encoding="utf-8") as f:
        json.dump(data.get("batches", []), f, indent=2, ensure_ascii=False)

    with open("extracted_data/clean_data/sections.json", "w", encoding="utf-8") as f:
        json.dump(data.get("sections", []), f, indent=2, ensure_ascii=False)

    with open("extracted_data/clean_data/full_metadata.json", "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

    print("Extraction successful!")
    print(f"Total Departments: {len(data.get('departments', []))}")
    print(f"Total Batches: {len(data.get('batches', []))}")
    print(f"Total Sections: {len(data.get('sections', []))}")
    
    # Check if section 68_D exists
    cse_batches = [b for b in data.get("batches", []) if b.get("name") == "68"]
    print(f"Batch 68 IDs: {[b['id'] for b in cse_batches]}")
    if cse_batches:
        b_id = cse_batches[0]["id"]
        sec_d = [s for s in data.get("sections", []) if s.get("batchId") == b_id and s.get("name") == "D"]
        print(f"Section 68_D details: {sec_d}")
else:
    print("Failed to balance braces")
