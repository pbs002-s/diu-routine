import re
import json

with open("extracted_data/routine_rsc.txt", "r", encoding="utf-8") as f:
    text = f.read()

print("routine_rsc.txt total length:", len(text))

# Search for line 22 or JSON payload where RoutinePageClient props are passed
for line in text.splitlines():
    if "RoutinePageClient" in line or '"departments":' in line:
        print("Found matching line prefix:", line[:150])
        # Find where the json object begins: 'a:[...'
        idx = line.find('{"teachers":')
        if idx == -1:
            idx = line.find('{"')
        if idx != -1:
            # Let's save the rest of this line as JSON
            payload_str = line[idx:]
            # Clean up trailing RSC artifacts if any
            with open("extracted_data/parsed_routine_page_payload.json", "w", encoding="utf-8") as out:
                out.write(payload_str)
            print("Successfully extracted raw client payload to extracted_data/parsed_routine_page_payload.json")
            break
