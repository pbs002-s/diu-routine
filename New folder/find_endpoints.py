import requests
import re

js_files = [
    'fb76edf9ae5e3539.js',
    '3221be7ca5ba8a22.js',
    '041e4ca0644e689f.js',
    '5c4b0d3ba7a84085.js',
    'ddf856f19cfce419.js',
    '285cc6f5f3db62a6.js',
    '4408f2db996b8327.js',
    'd1dd02e45b473895.js'
]

for js in js_files:
    url = f"https://routine.diuhub.bd/_next/static/chunks/{js}"
    r = requests.get(url)
    api_matches = re.findall(r'\"(/[^\"]+)\"', r.text)
    api_hits = [m for m in set(api_matches) if any(k in m for k in ['/api', 'routine', 'course', 'teacher', 'room', 'batch', 'section'])]
    print(f"\n{js}:")
    for h in sorted(api_hits)[:10]:
        print("  ", h)
