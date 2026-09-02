import os
import json
import requests

# Base configuration
BASE_URL = "https://routine.diuhub.bd"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "extracted_data")
os.makedirs(OUTPUT_DIR, exist_ok=True)

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/125.0.0.0 Safari/537.36"
    ),
    "Accept": "application/json, text/plain, */*",
    "Referer": "https://routine.diuhub.bd/",
    "Origin": "https://routine.diuhub.bd",
}

def save_json(data, filename):
    filepath = os.path.join(OUTPUT_DIR, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"[+] Saved: {filepath}")

def save_text(text, filename):
    filepath = os.path.join(OUTPUT_DIR, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(text)
    print(f"[+] Saved text/html: {filepath}")

def fetch_endpoint(url, filename_prefix, extra_headers=None):
    headers = dict(HEADERS)
    if extra_headers:
        headers.update(extra_headers)

    print(f"\n--> Fetching: {url}")
    try:
        response = requests.get(url, headers=headers, timeout=15)
        print(f"    Status: {response.status_code}")
        print(f"    Content-Type: {response.headers.get('content-type')}")

        if response.status_code == 200:
            try:
                data = response.json()
                save_json(data, f"{filename_prefix}.json")
                return data
            except ValueError:
                # Response is HTML, text or Next.js RSC payload
                save_text(response.text, f"{filename_prefix}.txt")
                return response.text
        else:
            print(f"    Failed with code: {response.status_code}")
            save_text(response.text, f"{filename_prefix}_error_{response.status_code}.txt")
    except Exception as e:
        print(f"    Error requesting {url}: {e}")
    return None

def main():
    print(f"Extracting routine data from: {BASE_URL}")
    print(f"Target Output Directory: {OUTPUT_DIR}\n" + "="*50)

    # 1. Auth / Session Check
    fetch_endpoint(f"{BASE_URL}/api/auth/session", "session")

    # 2. Main target paths identified from DevTools Network activity
    endpoints = [
        # Routine queries (Batch: 68, Section: D, Dept: CSE)
        (f"{BASE_URL}/api/routine?dept=CSE&batch=68&section=D", "api_routine_68_d"),
        (f"{BASE_URL}/api/routine?section=68_D", "api_routine_section_68_d"),
        (f"{BASE_URL}/routine?_rsc=1pb18", "routine_rsc", {"RSC": "1"}),
        (f"{BASE_URL}/courses?_rsc=1pb18", "courses_rsc", {"RSC": "1"}),
        (f"{BASE_URL}/teachers?_rsc=1pb18", "teachers_rsc", {"RSC": "1"}),
        (f"{BASE_URL}/rooms?_rsc=1pb18", "rooms_rsc", {"RSC": "1"}),
        (f"{BASE_URL}/advanced-search?_rsc=1pb18", "advanced_search_rsc", {"RSC": "1"}),
    ]

    for item in endpoints:
        url = item[0]
        prefix = item[1]
        extra_h = item[2] if len(item) > 2 else None
        fetch_endpoint(url, prefix, extra_headers=extra_h)

    print("\n" + "="*50)
    print(f"Finished. Check the '{OUTPUT_DIR}' folder for extracted files.")

if __name__ == "__main__":
    main()
