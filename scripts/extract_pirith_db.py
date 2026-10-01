#!/usr/bin/env python3
"""
scripts/extract_pirith_db.py
Pre-populated SQLite asset database generator for Pirith application.
Integrates verified Pali texts and Sinhala meanings from:
1. pitaka.lk (canonical JS data bundle from https://pitaka.lk/pirith/index.html#/)
2. tripitaka.online (canonical API endpoint https://www.tripitaka.online/api/sutta/240)
3. Verified namo.lk text where pitaka.lk data is supplementary, with strict noise/story filtering.

Outputs to: app/src/main/assets/databases/pirith.db
"""

import os
import re
import json
import sqlite3
import requests
from bs4 import BeautifulSoup

DB_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases")
DB_PATH = os.path.join(DB_DIR, "pirith.db")

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
}

PITAKA_JS_URL = "https://pitaka.lk/pirith/assets/index-C6RtjHj_.js"
TRIPITAKA_ATANATIYA_API = "https://www.tripitaka.online/api/sutta/240"

def clean_sinhala_text(text: str) -> str:
    """
    Cleans Unicode text:
    - Removes zero-width spaces (\\u200B) and byte-order-marks (\\uFEFF)
    - Replaces non-breaking space (\\u00A0) with normal space
    - Preserves Sinhala Zero-Width Joiner (\\u200D)
    - Normalizes multiple spaces and trims lines
    """
    if not text:
        return ""
    text = text.replace("\u200b", "").replace("\ufeff", "").replace("\r", "")
    text = text.replace("\u00a0", " ")
    text = re.sub(r"[ \t]+", " ", text)
    lines = [line.strip() for line in text.split("\n")]
    return "\n".join([line for line in lines if line]).strip()


def clean_verse_text(text: str) -> str:
    """
    Cleans up leading numbering like '1.', '2.', '6 – viii.' while retaining Pali/Sinhala content.
    """
    text = clean_sinhala_text(text)
    text = re.sub(r"^\d+\s*–\s*[ivxIVXLC]+\.\s*", "", text)
    text = re.sub(r"^\d+\.\s*", "", text)
    return text.strip()


def is_english_module(text: str) -> bool:
    eng = len(re.findall(r"[a-zA-Z]", text))
    sin = len(re.findall(r"[\u0D80-\u0DFF]", text))
    return eng > 10 and sin < 5


def is_noise_module(text: str) -> bool:
    noise_keywords = [
        "Related Resources", "Leave a Reply", "මූලාශ්‍ර", "Sources",
        "පිරුවානා පොත් වහන්සේ", "සිංහල අර්ථය මූලාශ්‍රය", "සිංහල තේරුම:",
        "සිංහල පරිවර්ථනය:"
    ]
    for n in noise_keywords:
        if n in text:
            if len(text) < 250 or text.startswith("මූලාශ්‍ර") or text.startswith("සිංහල") or "Leave a Reply" in text or "Related Resources" in text:
                return True
    return False


def is_likely_sinhala_translation(text: str) -> bool:
    sinh_markers = [
        'වන සේක', 'ඇතිවන්නේ ය', 'වන්නේ ය', 'නැත', 'සිටියේ ය',
        'කරව්', 'ගනිව්', 'නමුදු', 'කෙරෙහි', 'පිණිස', 'එකල්හී',
        'මහණෙනි', 'පිළිතුරු', 'වළකින්නාහ'
    ]
    return any(marker in text for marker in sinh_markers)


def fetch_pitaka_js() -> str:
    print(f"Fetching pitaka.lk JS bundle from {PITAKA_JS_URL}...")
    resp = requests.get(PITAKA_JS_URL, headers=HEADERS, timeout=30)
    resp.raise_for_status()
    return resp.text


def parse_pitaka_raw_blocks(key: str, js_content: str):
    m = re.search(r'\"' + key + r'\":\{(.*?)\}*,\s*\"[0-9]{2}-', js_content, re.DOTALL)
    if not m:
        # Fallback search if at end of dict
        m = re.search(r'\"' + key + r'\":\{(.*?)\}\};', js_content, re.DOTALL)
    if not m:
        return []
    raw_m = re.search(r'text:(\[\[.*?\]\])', m.group(1))
    if not raw_m:
        return []
    raw_blocks = raw_m.group(1).split('],[')
    res = []
    for b in raw_blocks:
        pairs = re.findall(r'\{pali:[\`\"\'](.*?)[\`\"\'],sinh:[\`\"\'](.*?)[\`\"\']', b)
        p = ' '.join([x[0].strip() for x in pairs if x[0].strip()])
        s = ' '.join([x[1].strip() for x in pairs if x[1].strip()])
        if p or s:
            res.append((clean_sinhala_text(p), clean_sinhala_text(s)))
    return res


# ==============================================================================
# PITAKA.LK SUTTA EXTRACTORS
# ==============================================================================

def get_pitaka_saranagamanam(js_content: str):
    """Sutta ID 1: Namaskara & Saranagamanam (4 verses)"""
    bl = parse_pitaka_raw_blocks("02-saranagamanam", js_content)
    if len(bl) < 12:
        return []
    v1 = (bl[0][0] + "\n" + bl[1][0] + "\n" + bl[2][0], bl[0][1])
    v2 = ("\n".join([bl[i][0] for i in [3, 4, 5]]), " ".join([bl[i][1] for i in [3, 4, 5]]))
    v3 = ("\n".join([bl[i][0] for i in [6, 7, 8]]), " ".join([bl[i][1] for i in [6, 7, 8]]))
    v4 = ("\n".join([bl[i][0] for i in [9, 10, 11]]), " ".join([bl[i][1] for i in [9, 10, 11]]))
    return [v1, v2, v3, v4]


def get_pitaka_theruwan(js_content: str):
    """Sutta ID 2: Ratanattaya Vandana (3 verses: Buddhanussati, Dhammanussati, Sanghanussati)"""
    dhajagga = parse_pitaka_raw_blocks("17-dhajagga-parittam", js_content)
    v1_p = dhajagga[15][0]
    v1_s = dhajagga[15][1]
    v2_p = dhajagga[18][0]
    v2_s = dhajagga[18][1]
    v3_p = dhajagga[21][0] + "\n" + dhajagga[22][0]
    v3_s = dhajagga[21][1] + " " + dhajagga[22][1]
    return [
        (f"[බුද්ධානුස්සතිය]\n{v1_p}", v1_s),
        (f"[ධම්මානුස්සතිය]\n{v2_p}", v2_s),
        (f"[සංඝානුස්සතිය]\n{v3_p}", v3_s),
    ]


def get_pitaka_dasasikkhapadani(js_content: str):
    """Sutta ID 3: Dasa Sila (10 verses)"""
    return parse_pitaka_raw_blocks("03-dasasikkhapadani", js_content)


def get_pitaka_mahamangala(js_content: str):
    """Sutta ID 4: Maha Mangala Sutta (13 clean verses: intro, devata, 11 stanzas)"""
    bl = parse_pitaka_raw_blocks("08-mahamangala", js_content)
    if len(bl) < 32:
        return []
    intro_p = "\n".join([b[0] for b in bl[1:8]])
    intro_s = " ".join([b[1] for b in bl[1:8]])
    dev_p = "\n".join([b[0] for b in bl[8:10]])
    dev_s = " ".join([b[1] for b in bl[8:10]])
    gathas = []
    for i in range(10, len(bl), 2):
        p = bl[i][0] + "\n" + bl[i+1][0]
        s = bl[i][1] + " " + bl[i+1][1]
        gathas.append((p, s))
    return [(intro_p, intro_s), (dev_p, dev_s)] + gathas


def get_pitaka_rathana(js_content: str):
    """Sutta ID 5: Ratana Sutta (17 canonical verses: 2 intro + 12 jewel + 3 sakka)"""
    bl = parse_pitaka_raw_blocks("09-rathana", js_content)
    if len(bl) < 47:
        return []
    v_list = []
    # v1
    v_list.append((bl[1][0] + "\n" + bl[2][0], bl[1][1] + " " + bl[2][1]))
    # v2
    v_list.append((bl[3][0] + "\n" + bl[4][0], bl[3][1] + " " + bl[4][1]))
    # v3 - v14
    cur_p, cur_s = [], []
    for i in range(5, 41):
        cur_p.append(bl[i][0])
        cur_s.append(bl[i][1])
        if "සුවත්ථි හොතු" in bl[i][0] or "සුවත්ථි හෝතු" in bl[i][0]:
            v_list.append(("\n".join(cur_p), " ".join(cur_s)))
            cur_p, cur_s = [], []
    # v15 - v17 (Sakka verses)
    for i in [41, 43, 45]:
        v_list.append((bl[i][0] + "\n" + bl[i+1][0], bl[i][1] + " " + bl[i+1][1]))
    return v_list


def get_pitaka_karaniya(js_content: str):
    """Sutta ID 6: Karaniya Metta Sutta (10 verses)"""
    bl = parse_pitaka_raw_blocks("10-karaniya", js_content)
    verses = []
    for i in range(1, len(bl), 2):
        p = bl[i][0] + "\n" + bl[i+1][0]
        s = bl[i][1] + " " + bl[i+1][1]
        verses.append((p, s))
    return verses


def get_pitaka_standard(key: str, js_content: str):
    """Generic extractor for pitaka suttas, dropping block 0 if title"""
    bl = parse_pitaka_raw_blocks(key, js_content)
    if not bl:
        return []
    p0 = bl[0][0]
    if "සුත්ත" in p0 or "පරිත්ත" in p0 or "සුතං" in p0:
        return bl[1:]
    return bl


# ==============================================================================
# TRIPITAKA.ONLINE SUTTA EXTRACTOR
# ==============================================================================

def fetch_tripitaka_atanatiya():
    """Sutta ID 102: Atanatiya Sutta canonical 53 protective chanting verses"""
    print(f"Fetching Atanatiya Sutta from {TRIPITAKA_ATANATIYA_API}...")
    resp = requests.get(TRIPITAKA_ATANATIYA_API, headers=HEADERS, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    content = data.get("content", {}).get("data", [])
    
    gathas = []
    for i, item in enumerate(content):
        cls = item.get("class", "")
        if "gatha" in cls and "pali-text" in cls:
            pali = item.get("content", "")
            sinh = ""
            if i + 1 < len(content) and "sinhala-text" in content[i+1].get("class", ""):
                sinh = content[i+1].get("content", "")
            gathas.append((pali, sinh))
            
    clean_atanatiya = []
    for p, s in gathas[:53]:
        p_clean = clean_verse_text(p)
        s_clean = clean_verse_text(s)
        clean_atanatiya.append((p_clean, s_clean))
        
    print(f"  -> Successfully extracted {len(clean_atanatiya)} verses for Atanatiya.")
    return clean_atanatiya


# ==============================================================================
# NAMO.LK SCRAPER (FOR NON-PITAKA SUTTAS)
# ==============================================================================

def scrape_namo_page(slug: str, session: requests.Session):
    url = f"https://namo.lk/{slug}/"
    resp = None
    for attempt in range(3):
        try:
            resp = session.get(url, headers=HEADERS, timeout=20)
            if resp.status_code == 200:
                break
        except Exception as e:
            if attempt == 2:
                print(f"  [ERROR] Scraping {url}: {e}")
                return []
            time.sleep(2)
    
    if not resp or resp.status_code != 200:
        print(f"  [WARN] Failed to fetch {url}")
        return []

    try:
        
        soup = BeautifulSoup(resp.text, "html.parser")
        body = soup.find("div", class_="et-l--body")
        if not body:
            body = soup.find("div", class_="entry-content") or soup.find("article")
            
        if not body:
            print(f"  [WARN] No body container found in {url}")
            return []
        
        modules = body.find_all("div", class_="et_pb_text_inner")
        cleaned_modules = []
        for m in modules:
            txt = clean_sinhala_text(m.get_text(separator="\n"))
            if not txt or is_english_module(txt) or is_noise_module(txt):
                continue
            cleaned_modules.append(txt)
            
        verses = []
        
        # Guna & Meditation modules:
        if slug in ["dhamma-guna", "sanga-guna", "buddha-meditation"]:
            for item in cleaned_modules:
                if item.startswith("මාගේ බුදුරජාණන් වහන්සේ විසින් දේශනා කරන ලද"):
                    continue
                lines = [l.strip() for l in item.split("\n") if l.strip()]
                if len(lines) >= 2:
                    pali = clean_verse_text(lines[0])
                    sinhala = clean_verse_text("\n".join(lines[1:]))
                    verses.append((pali, sinhala))
                else:
                    verses.append((clean_verse_text(item), clean_verse_text(item)))
            return verses

        # Cakkavatti Sihanada Sutta: drop initial 2 titles, fix Pali/Sinhala alignment
        if slug == "chakkavatti-seehanada-sutra":
            if cleaned_modules and "චක්කවත්ති" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]
            if cleaned_modules and "සිහනදක්" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]
            i = 0
            while i < len(cleaned_modules):
                item = cleaned_modules[i]
                if i + 1 < len(cleaned_modules):
                    nxt = cleaned_modules[i+1]
                    if is_likely_sinhala_translation(item) and not is_likely_sinhala_translation(nxt):
                        verses.append((clean_verse_text(nxt), clean_verse_text(item)))
                    else:
                        verses.append((clean_verse_text(item), clean_verse_text(nxt)))
                    i += 2
                else:
                    verses.append((clean_verse_text(item), clean_verse_text(item)))
                    i += 1
            return verses

        # Budhu Guna Piritha: drop initial instruction note
        if slug == "budhu-guna-piritha":
            if cleaned_modules and "රෝගී වූ විට" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]

        # Drop trailing standalone 'සාධූ!'
        if cleaned_modules and cleaned_modules[-1].startswith("සාධූ"):
            cleaned_modules = cleaned_modules[:-1]

        i = 0
        while i < len(cleaned_modules):
            item = cleaned_modules[i]
            
            # Heading triplet check: e.g. '1. නමස්කාරය'
            if len(item) < 35 and "\n" not in item and i + 2 < len(cleaned_modules) and not item.endswith("."):
                heading = item
                pali = f"[{heading}]\n" + clean_verse_text(cleaned_modules[i + 1])
                sinhala = clean_verse_text(cleaned_modules[i + 2])
                verses.append((pali, sinhala))
                i += 3
            elif i + 1 < len(cleaned_modules):
                pali = clean_verse_text(item)
                sinhala = clean_verse_text(cleaned_modules[i + 1])
                verses.append((pali, sinhala))
                i += 2
            else:
                lines = [l.strip() for l in item.split("\n") if l.strip()]
                if len(lines) >= 2:
                    verses.append((lines[0], "\n".join(lines[1:])))
                else:
                    verses.append((item, item))
                i += 1
                
        return verses
    except Exception as e:
        print(f"  [ERROR] Scraping {url}: {e}")
        return []


# ==============================================================================
# SUTTAS METADATA
# ==============================================================================

SUTTAS_CONFIG = [
    # --- 1. වන්දනා (Vandana) ---
    {"id": 1, "source": "pitaka", "key": "02-saranagamanam", "titlePali": "Namaskara & Saranagamanam", "titleSinhala": "නමස්කාරය සහ තිසරණය", "category": "වන්දනා", "orderIndex": 1, "isFavorite": 0},
    {"id": 2, "source": "pitaka_theruwan", "key": "theruwan", "titlePali": "Ratanattaya Vandana", "titleSinhala": "තෙරුවන් නැමදීම (රත්නත්‍රය වන්දනා)", "category": "වන්දනා", "orderIndex": 2, "isFavorite": 0},
    {"id": 32, "source": "namo_puja_slice", "slug": "buddha-puja", "slice_start": 0, "slice_end": 3, "titlePali": "Pancha Sila Samadanaya", "titleSinhala": "පංචශීලය (පන්සිල් සමාදන් වීම)", "category": "වන්දනා", "orderIndex": 3, "isFavorite": 0},
    {"id": 3, "source": "pitaka", "key": "03-dasasikkhapadani", "titlePali": "Dasa Sila Vandana", "titleSinhala": "දස සිල් වන්දනාව", "category": "වන්දනා", "orderIndex": 4, "isFavorite": 0},
    {"id": 23, "source": "namo_puja_slice", "slug": "buddha-puja", "slice_start": 10, "slice_end": 21, "titlePali": "Buddha Puja Gatha", "titleSinhala": "බුද්ධ පූජා ගාථා", "category": "වන්දනා", "orderIndex": 5, "isFavorite": 0},
    {"id": 25, "source": "namo", "slug": "dhamma-guna", "titlePali": "Dhamma Vandana", "titleSinhala": "දහම් ගුණ වන්දනාව (ධර්ම රත්නයේ ගුණ)", "category": "වන්දනා", "orderIndex": 6, "isFavorite": 0},
    {"id": 26, "source": "namo", "slug": "sanga-guna", "titlePali": "Sangha Vandana", "titleSinhala": "සඟ ගුණ වන්දනාව (සංඝ රත්නයේ ගුණ)", "category": "වන්දනා", "orderIndex": 7, "isFavorite": 0},
    {"id": 27, "source": "namo", "slug": "buddha-meditation", "titlePali": "Buddhanussati Bhavana", "titleSinhala": "බුද්ධානුස්සති භාවනාව", "category": "වන්දනා", "orderIndex": 8, "isFavorite": 0},

    # --- 2. බෝධි පූජා (Bodhi Puja) ---
    {"id": 33, "source": "namo_puja_slice", "slug": "buddha-puja", "slice_start": 6, "slice_end": 10, "titlePali": "Bodhi Vandanawa saha Gatha", "titleSinhala": "බෝධි වන්දනාව සහ ගාථා", "category": "බෝධි පූජා", "orderIndex": 9, "isFavorite": 0},

    # --- 3. මහා පිරිත් (Maha Pirith) ---
    {"id": 4, "source": "pitaka_mahamangala", "key": "08-mahamangala", "titlePali": "Maha Mangala Suttam", "titleSinhala": "මහා මංගල සූත්‍රය", "category": "මහා පිරිත්", "orderIndex": 10, "isFavorite": 0},
    {"id": 5, "source": "pitaka_rathana", "key": "09-rathana", "titlePali": "Ratana Suttam", "titleSinhala": "රතන සූත්‍රය", "category": "මහා පිරිත්", "orderIndex": 11, "isFavorite": 0},
    {"id": 6, "source": "pitaka_karaniya", "key": "10-karaniya", "titlePali": "Karaniya Metta Suttam", "titleSinhala": "කරණීය මෙත්ත සූත්‍රය", "category": "මහා පිරිත්", "orderIndex": 12, "isFavorite": 0},

    # --- 4. සෙත් පිරිත් (Seth Pirith) ---
    {"id": 7, "source": "pitaka_std", "key": "11-khandha-parittam", "titlePali": "Khandha Parittam", "titleSinhala": "ඛන්ධ පිරිත (කඳ පිරිත)", "category": "සෙත් පිරිත්", "orderIndex": 13, "isFavorite": 0},
    {"id": 8, "source": "pitaka_std", "key": "14-mora-parittam", "titlePali": "Mora Parittam", "titleSinhala": "මෝර පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 14, "isFavorite": 0},
    {"id": 9, "source": "pitaka_std", "key": "17-dhajagga-parittam", "titlePali": "Dhajagga Suttam", "titleSinhala": "ධජග්ග සූත්‍රය (ධජග්ග පිරිත)", "category": "සෙත් පිරිත්", "orderIndex": 15, "isFavorite": 0},
    {"id": 101, "source": "pitaka_std", "key": "52-wattaka", "titlePali": "Vattaka Parittam", "titleSinhala": "වට්ටක පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 16, "isFavorite": 0},
    {"id": 10, "source": "pitaka_std", "key": "50-angulimala", "titlePali": "Angulimala Parittam", "titleSinhala": "අංගුලිමාල පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 17, "isFavorite": 0},
    {"id": 11, "source": "pitaka_std", "key": "61-bojjhanga-parittam", "titlePali": "Bojjhanga Parittam", "titleSinhala": "බොජ්ඣංග පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 18, "isFavorite": 0},
    {"id": 12, "source": "pitaka_std", "key": "56-jaya-parittam", "titlePali": "Jaya Parittam", "titleSinhala": "ජය පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 19, "isFavorite": 0},
    {"id": 16, "source": "pitaka_std", "key": "16-suriya-parittam", "titlePali": "Suriya Parittam", "titleSinhala": "සූරිය පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 20, "isFavorite": 0},
    {"id": 17, "source": "pitaka_std", "key": "15-canda-parittam", "titlePali": "Chanda Parittam", "titleSinhala": "චන්ද පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 21, "isFavorite": 0},
    {"id": 24, "source": "namo", "slug": "budhu-guna-piritha", "titlePali": "Buddha Guna Parittam", "titleSinhala": "බුදු ගුණ පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 22, "isFavorite": 0},
    {"id": 103, "source": "pitaka_std", "key": "51-atavisi", "titlePali": "Atavisi Parittam", "titleSinhala": "අටවිසි පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 23, "isFavorite": 0},
    {"id": 104, "source": "pitaka_std", "key": "59-jinapanjara-parittam", "titlePali": "Jinapanjara Parittam", "titleSinhala": "ජිනපඤ්ජර පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 24, "isFavorite": 0},
    {"id": 18, "source": "namo", "slug": "rathnamali-yantra", "titlePali": "Rathnamali Gatha", "titleSinhala": "රත්නමාලී ගාථා (රත්නමාලී යන්ත්‍රය)", "category": "සෙත් පිරිත්", "orderIndex": 25, "isFavorite": 0},
    {"id": 107, "source": "static", "titlePali": "Abhisambhidhana Parittam", "titleSinhala": "අභිසම්භිධාන පිරිත", "category": "සෙත් පිරිත්", "orderIndex": 26, "isFavorite": 0, "verses": [
        ("නමෝ අභිසම්භිදානේ යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "සකල විධ සත්‍යාවබෝධ ඥාන සම්භාරයන්ගෙන් පරිපූර්ණ වූ උතුම් බුද්ධ ගුණ බලයෙන් සියලු අන්තරායෝ සහ දෝෂයෝ දුරුවී ආලෝකමත්ව දැල්වෙත්වා, නිරුද්‍රව සෙතක්ම වේවා."),
        ("නමෝ සම්මා සම්බුද්ධෝ යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "චතුරාර්ය සත්‍ය ධර්මයන් ස්වයංභූ ඥානයෙන් අවබෝධ කොට වදාළා වූ ලොව්තුරා සම්මා සම්බුදුරජාණන් වහන්සේගේ අනන්ත තේජෝ බලයෙන් සියලු අඳුරු බිය දුරු වී ආශීර්වාදය පැතිරේවා."),
        ("නමෝ පච්චේක සම්බුද්ධෝ යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "තමන් වහන්සේ විසින්ම ධර්මය අවබෝධ කරගත් සියලු පසේබුදුරජාණන් වහන්සේලාගේ ශාන්ත ගුණ තේජසින් සියලු රෝග උපද්‍රවයෝ පහව යෙත්වා."),
        ("නමෝ ආනන්ද මහා ථේරෝ මහා ධාරණෝ මහා වීරෝ, යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "බහුශ්‍රැතයන් අතර අග්‍ර වූ, ධර්ම භාණ්ඩාගාරික, මහාවීරිය සම්පන්න ආනන්ද මහා ථේරයන් වහන්සේගේ ගුණ තේජසින් සියලු බිය උපද්‍රව පහව යේවා."),
        ("නමෝ සාරිපුත්ත මොග්ගල්ලානේ මහා ප්‍රාඥෝ, මහා ඎද්ධිමන්තෝ, යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "මහා ප්‍රඥාවන්ත වූ අග්‍රශ්‍රාවක ධර්ම සේනාධිපති සාරිපුත්ත මහරහතන් වහන්සේගේ ද, මහා සෘද්ධිමත් වූ දෙවන අග්‍රශ්‍රාවක මහා මොග්ගල්ලාන මහරහතන් වහන්සේගේ ද අසමසම තේජෝ බලයෙන් නිරන්තර සෙත සැලසේවා."),
        ("නමෝ බක්කුල මහා ථේරෝ මහා නීරෝගෝ යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "නීරෝගී සම්පත්තියෙන් අගතැන්පත් වූ බක්කුල මහා ථේරයන් වහන්සේගේ උතුම් ආශීර්වාද බලයෙන් සියලු රෝග පීඩා දුරු වී දීර්ඝායුෂ ලැබේවා."),
        ("නමෝ සීවලී මහා ථේරෝ මහා ලාභී යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "චීවරාදී ප්‍රත්‍ය ලාභීන් අතර අගතැන්පත් මහා සීවලී මහරහතන් වහන්සේගේ පාරමිතා බලයෙන් සකල සම්පත් ලාභය සහ සෞභාග්‍යය උදාවේවා."),
        ("නමෝ වක්කලී මහා ථේරෝ මහා ශ්‍රද්ධා යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "ශ්‍රද්ධාධික භික්ෂූන් අතර අගතැන්පත් වූ වක්කලී මහා ථේරයන් වහන්සේගේ උතුම් ශ්‍රද්ධා බලයෙන් සියලු ශෝක උපද්‍රව විනාශ වේවා."),
        ("නමෝ නන්ද මහා ථේරෝ මහා ඉන්ද්‍රිය සංවරෝ යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "ඉන්ද්‍රිය සංවරයෙහි අගතැන්පත් වූ නන්ද මහා ථේරයන් වහන්සේගේ උදාර සංවර සීල තේජසින් සියලු දෝෂ දුරුවේවා."),
        ("නමෝ රාහුල මහා ථේරෝ මහා ශික්ෂාකාමී යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:", "ශික්ෂාකාමීන් අතර අගතැන්පත් වූ රාහුල මහා ථේරයන් වහන්සේගේ සීල ගුණ බලයෙන් සියලු මංගල්‍ය සිරි අත්වේවා."),
        ("නමෝ සත්තතිංස බෝධි පක්ඛියා ධම්මා, චත්තාරෝ සතිපට්ඨානා, චත්තාරෝ ඉද්ධි පාදා, චත්තාරෝ සම්මප්පධානා, චත්තාරි අරිය සච්චානි, පංචින්ද්‍රියානි / පංචබලානි, සප්ත බොජ්ඣංගානි, අරිය අට්ඨංගිකෝ මග්ගො බුද්ධ මන්තංච මන්තං, ධම්ම මන්තංච මන්තං, සංඝ මන්තංච මන්තං,\n\n• මයිහං / තුම්හාකං\n• සුජාත නාමස්ස\n• සුජාතා නාමිකාය\n\nසබ්බ රෝග/සබ්බ දෝෂ/සබ්බන්තරායා විනස්සන්තු යුන්තේ යුන්තේ පජ්ජලති පජ්ජලති තීහුම් තීහුම් ලුහු ලුහු පරමේස්වාහා:\nඑතෙන සච්ච වජ්ජේන සොත්ථි මෙ/ තේ /වෝ හෝතු සබ්බදා///", "සතර සතිපට්ඨානය, සතර ඉද්ධිපාදය, සතර සම්මප්පධානය, චතුරාර්ය සත්‍යය, පංච ඉන්ද්‍රිය, පංච බල, සප්ත බොජ්ඣංග සහ ආර්ය අෂ්ටාංගික මාර්ගය යන සත්තිස් බෝධිපාක්ෂික ධර්මයන්ගේ ද, බුද්ධ-ධම්ම-සංඝ යන උතුම් තෙරුවන් මන්ත්‍ර බලයෙන් ද, මාගේ / ඔබගේ / ආශීර්වාද ලබන්නන්ගේ සියලු රෝග, සියලු දෝෂ සහ සියලු අන්තරායෝ සහමුලින් විනාශ වෙත්වා! මේ සත්‍ය වචනයේ බලයෙන් සැමදා සෙතක්ම වේවා!")
    ]},
    {"id": 108, "source": "static", "titlePali": "Seevali Parittam", "titleSinhala": "සීවලී පිරිත (සීවලී ගාථා)", "category": "සෙත් පිරිත්", "orderIndex": 27, "isFavorite": 0, "verses": [
        ("පූරෙන්තා පාරමී සබ්බේ - සබ්බේ පච්චේක නායකා,\nසීවලී ගුණතේජේන - පරිත්තං තං භණාමහේ.", "සියලු පාරමිතාවන් සම්පූර්ණ කළා වූ සියලු පසේබුදුරජාණන් වහන්සේලාගේ ද, සීවලී මහරහතන් වහන්සේගේ ද උදාර ගුණ තේජස සිහි කරමින් අපි මේ ආරක්ෂක පිරිත සජ්ඣායනා කරමු."),
        ("න ජාලීති ජාලීති ජාලිතාම්, ආ ඊ ඌ ආම් ඉ ස්වාහා;\nනාසාසීමෙචමෝසීසං නානාජාලීති සංජලිං,\nසදේව මනුස්ස පූජිතං - සබ්බ ලාභා උපෙන්තු මේ.", "දෙව් මිනිසුන් විසින් පුදන ලද්දා වූ, ලාභ සම්පත් උදා කරන්නා වූ ආනුභාව සම්පන්න සීවලී මහරහතන් වහන්සේගේ ආශීර්වාදයෙන් සියලු ලාභ සත්කාර හා සෞභාග්‍යය මා වෙත (අප වෙත) නොඅඩුව ලැබේවා."),
        ("සීවලී ච මහාථේරෝ - සෝ රහෝ පච්චයාදිනං,\nපියෝ දේවමනුස්සානං - පියෝ බ්‍රහ්මාන මුත්තමෝ,\nපියෝ නාග සුපණ්ණානං - පීණින්ද්‍රියං නමාමහං.", "චීවරාදී සිව්පසය ලැබීමට ඉතා සුදුසු වූ, දෙවියන්ට, මිනිසුන්ට, බ්‍රහ්මයන්ට මෙන්ම නාග හා ගුරුළු ආදීන්ට ද ප්‍රිය වූ, ශාන්ත ඉන්ද්‍රියන්ගෙන් යුත් මහා සීවලී මහරහතන් වහන්සේට මම සාදරයෙන් නමස්කාර කරමි."),
        ("පදුමුත්තරෝ නාම ජිනෝ - සබ්බධම්මේසු චක්ඛුමා,\nඉතෝ සතසහස්සම්හි - කප්පේ උප්පජ්ජි නායකෝ.", "සියලු ධර්මයන් විශදව දක්නා නුවණැසින් හෙබි පදුමුත්තර නම් වූ ලෝකනායක බුදුරජාණන් වහන්සේ මීට කල්ප ලක්ෂයකට පෙර මෙලොව පහළ වූ සේක."),
        ("තස්ස දානං දදිත්වාන - පත්ථෙසිං පදුමුත්තරේ,\nඒතදග්ගං ලභිස්සාමි - ලාභීනං උත්තමෝ අහං.", "ඒ පදුමුත්තර බුදුරජාණන් වහන්සේ ප්‍රමුඛ මහා සංඝරත්නයට සතියක් මුළුල්ලේ මහදන් දී, 'මමද අනාගතයේ ලාභීන් අතුරින් අගතැන්පත් වෙම්වා' යි දැඩිව ප්‍රාර්ථනා කළෙමි."),
        ("සත්තාහං ද්වාරමුළ්හෝ හං - මහාදුක්ඛ සමප්පිතෝ,\nමාතා මේ ඡන්දදානේන - ඒවමාසි සුදුක්ඛිතා.", "මව්කුස සත් අවුරුද්දක් වැඩෙමින් ද, දරු ප්‍රසූතියේ දී සත් දිනක් මුළුල්ලේ දොරටුවේ රැඳෙමින් මහත් දුකට පත් වූයෙමි. මාගේ මව් වූ සුප්පවාසාව ද ඒ හේතුවෙන් ඉමහත් දුක් වින්දා ය."),
        ("කේසේසු ඡිජ්ජමානේසු - අරහත්තම පාපුණිං,\nදේවා නාගා මනුස්සා ච - පච්චයානුපනෙන්ති මං.", "පැවිදි බිමට පත්ව මගේ හිසකෙස් බාන අවස්ථාවේදීම උතුම් අරහත්වයට පත් වූයෙමි. එතැන් පටන් දෙවියෝ ද, නාගයෝ ද, මිනිස්සු ද නොකඩවා මට සිව්පසයෙන් උපස්ථාන කරති."),
        ("සීවලීසීවලී නාමං - සබ්බ ලෝකේසු විස්සුතං,\nතස්ස තේජේන සීලේන - සබ්බත්ථ සුඛිතෝ භව.", "මුළු ලොව පුරා ප්‍රකට වූ උතුම් සීවලී මහ රහතන් වහන්සේගේ ආනුභාවයෙන් හා සීල තේජසින් සෑම තන්හිම සැපවත් බව හා සෞභාග්‍යය උදාවේවා!")
    ]},

    # --- 5. විශේෂ සූත්‍ර (Vishesha Sutta) ---
    {"id": 13, "source": "pitaka_std", "key": "21-girimananda-suttam", "titlePali": "Girimananda Suttam", "titleSinhala": "ගිරිමානන්ද සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 28, "isFavorite": 0},
    {"id": 102, "source": "tripitaka_online", "titlePali": "Atanatiya Suttam", "titleSinhala": "ආටානාටිය සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 29, "isFavorite": 0},
    {"id": 14, "source": "pitaka_std", "key": "22-isigili-suttam", "titlePali": "Isigili Suttam", "titleSinhala": "ඉසිගිලි සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 30, "isFavorite": 0},
    {"id": 15, "source": "pitaka_std", "key": "23-dhammacakkappavattana", "titlePali": "Dhammacakkappavattana Suttam", "titleSinhala": "ධම්මචක්කප්පවත්තන සූත්‍රය (දම්සක් පැවතුම් සූත්‍රය)", "category": "විශේෂ සූත්‍ර", "orderIndex": 31, "isFavorite": 0},
    {"id": 105, "source": "static", "titlePali": "Anattalakkhana Suttam", "titleSinhala": "අනත්තලක්ඛණ සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 32, "isFavorite": 0, "verses": [
        ("ඒවං මේ සුතං. ඒකං සමයං භගවා බාරාණසියං විහරති ඉසිපතනේ මිගදායේ.\nතත්‍ර ඛෝ භගවා පඤ්චවග්ගියේ භික්ඛූ ආමන්තේසි භික්ඛවෝති.", "මා විසින් මෙසේ අසන ලදී. එක් සමයෙක භාග්‍යවතුන් වහන්සේ බරණැස ඉසිපතන මිගදායෙහි වැඩවසන සේක. එහි දී භාග්‍යවතුන් වහන්සේ පස්වග මහණුන් අමතා 'මහණෙනි' යි වදාළ සේක."),
        ("රූපං භික්ඛවේ අනත්තා. රූපඤ්ච හිදං භික්ඛවේ අත්තා අභවිස්ස, නයිදං රූපං ආබාධාය සංවත්තේථ, ලබ්භේථ ච රූපේ: ඒවං මේ රූපං හෝතු, ඒවං මේ රූපං මා අහෝසීති.", "මහණෙනි, රූපය අනාත්ම ය. මහණෙනි, ඉදින් මේ රූපය ආත්මය වී නම්, මේ රූපය පෙළීමට හේතු නොවන්නේ ය. තවද 'මාගේ රූපය මෙසේ වේවා, මාගේ රූපය මෙසේ නොවේවා' යි රූපය කෙරෙහි කැමැත්ත පරිදි පැවැත්විය හැකි වන්නේ ය."),
        ("යස්මා ච ඛෝ භික්ඛවේ රූපං අනත්තා, තස්මා රූපං ආබාධාය සංවත්තති, න ච ලබ්භති රූපේ: ඒවං මේ රූපං හෝතු, ඒවං මේ රූපං මා අහෝසීති.", "මහණෙනි, යම් හෙයකින් රූපය අනාත්ම වේ ද, එහෙයින් රූපය පෙළීමට හේතු වෙයි. 'මාගේ රූපය මෙසේ වේවා, මෙසේ නොවේවා' යි කැමැත්ත පරිදි ද නොලැබෙයි."),
        ("වේදනා අනත්තා... සඤ්ඤා අනත්තා... සංඛාරා අනත්තා... විඤ්ඤාණං අනත්තා.", "වේදනාව ද අනාත්ම ය... සංඥාව ද අනාත්ම ය... සංස්කාරයෝ ද අනාත්ම ය... විඤ්ඤාණය ද අනාත්ම ය."),
        ("තං කිං මඤ්ඤථ භික්ඛවේ, රූපං නිච්චං වා අනිච්චං වාති? අනිච්චං භන්තේ. යං පනානිච්චං දුක්ඛං වා තං සුඛං වාති? දුක්ඛං භන්තේ.", "මහණෙනි, ඒ කුමකැයි සිතන්නහු ද? රූපය නිත්‍ය ද අනිත්‍ය ද? 'ස්වාමීනි, අනිත්‍ය ය.' යමක් අනිත්‍ය නම් එය දුක් ද සැප ද? 'ස්වාමීනි, දුකකි.'"),
        ("යං පනානිච්චං දුක්ඛං විපරිණාමධම්මං, කල්ලං නු තං සමනුපස්සිතුං: ඒතං මම, ඒසෝහමස්මි, ඒසෝ මේ අත්තාති? නෝ හේතං භන්තේ.", "යමක් අනිත්‍ය නම්, දුක් නම්, පෙරළෙන ස්වභාව ඇත්නම්, එය 'තෙල මාගේ ය, තෙල මම වෙමි, තෙල මාගේ ආත්මය' යි දැකීම සුදුසු ද? 'ස්වාමීනි, එය නොවේ ම ය.'"),
        ("ඒවං පස්සං භික්ඛවේ සුතවා අරියසාවකෝ රූපස්මිම්පි නිබ්බින්දති, වේදනායපි නිබ්බින්දති, සඤ්ඤායපි නිබ්බින්දති, සංඛාරේසුපි නිබ්බින්දති, විඤ්ඤාණස්මිම්පි නිබ්බින්දති.", "මහණෙනි, මෙසේ දක්නා ශ්‍රැතවත් ආර්ය ශ්‍රාවක තෙම රූපය කෙරෙහි ද කලකිරෙයි. වේදනාව කෙරෙහි ද කලකිරෙයි. සංඥාව කෙරෙහි ද කලකිරෙයි. සංස්කාරයන් කෙරෙහි ද කලකිරෙයි. විඤ්ඤාණය කෙරෙහි ද කලකිරෙයි."),
        ("නිබ්බින්දං විරජ්ජති, විරාගා විමුච්චති, විමුත්තස්මිං විමුත්තමිති ඤාණං හෝති: ඛීණා ජාති, වුසිතං බ්‍රහ්මචරියං, කතං කරණීයං, නාපරං ඉත්ථත්තායාති පජානාතීති.", "කලකිරෙන්නේ නොඇලෙයි. නොඇල්මෙන් මිදෙයි. මිදුණු කල්හි 'මිදුණේ ය' යන ඥානය වෙයි. 'ජාතිය ක්ෂය විය. බඹසර වැස නිමවන ලදී. කළ යුතු දේ කරන ලදී. මෙයින් මතු කළ යුතු අන් කිසිවක් නැතැ'යි දැනගනියි."),
        ("ඉදමවෝච භගවා. අත්තමනා පඤ්චවග්ගියා භික්ඛූ භගවතෝ භාසිතං අභිනන්දුන්ති. ඉමස්මිඤ්ච පන වෙය්‍යාකරණස්මිං භඤ්ඤමානේ පඤ්චවග්ගියානං භික්ඛූනං අනුපාදාය ආසවේහි චිත්තානි විමුච්චිංසූති.", "භාග්‍යවතුන් වහන්සේ මෙය වදාළ සේක. සතුටු සිත් ඇති පස්වග මහණහු භාග්‍යවතුන් වහන්සේගේ වචනය සාදරයෙන් පිළිගත්හ. මේ ධර්ම දේශනාව පවත්වන කල්හි පස්වග තෙරවරුන්ගේ සිත් දැඩිව නොගෙන ආශ්‍රවයන්ගෙන් සපුරා මිදුණාහු ය.")
    ]},
    {"id": 106, "source": "static", "titlePali": "Adittapariyaya Suttam", "titleSinhala": "ආදිත්තපරියාය සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 33, "isFavorite": 0, "verses": [
        ("ඒවං මේ සුතං. ඒකං සමයං භගවා ගයායං විහරති ගයාසීසේ සද්ධිං භික්ඛුසහස්සේන. තත්‍ර ඛෝ භගවා භික්ඛූ ආමන්තේසි:", "මා විසින් මෙසේ අසන ලදී. එක් කලෙක භාග්‍යවතුන් වහන්සේ දහසක් භික්ෂූන් සමඟ ගයාවෙහි ගයාසීස නම් පර්වතයෙහි වැඩවසන සේක. එහි දී භාග්‍යවතුන් වහන්සේ භික්ෂූන් අමතා මෙසේ වදාළ සේක:"),
        ("සබ්බං භික්ඛවේ ආදිත්තං. කිඤ්ච භික්ඛවේ සබ්බං ආදිත්තං? චක්ඛුං භික්ඛවේ ආදිත්තං, රූපා ආදිත්තා, චක්ඛුවිඤ්ඤාණං ආදිත්තං, චක්ඛුසම්ඵස්සෝ ආදිත්තෝ.", "මහණෙනි, සියල්ල ගිනි ගෙන ඇවිළෙන්නේ ය. මහණෙනි, ගිනි ගෙන ඇවිළෙන සියල්ල කුමක් ද? මහණෙනි, ඇස ගිනි ගෙන ඇත, රූප ගිනි ගෙන ඇත, ඇසේ විඤ්ඤාණය ගිනි ගෙන ඇත, ඇසේ ස්පර්ශය ගිනි ගෙන ඇත."),
        ("කේන ආදිත්තං? රාගග්ගිනා, දෝසග්ගිනා, මෝහග්ගිනා ආදිත්තං, ජාතියා ජරාමරණේන සෝකේහි පරිදේවේහි දුක්ඛේහි දෝමනස්සේහි උපායාසේහි ආදිත්තන්ති වදාමි.", "කුමකින් ඇවිළී තිබේ ද? රාග ගින්නෙන්, ද්වේෂ ගින්නෙන්, මෝහ ගින්නෙන් ඇවිළී තිබේ. ජාතියෙන්, ජරාවෙන්, මරණයෙන්, ශෝකයෙන්, වැළපීමෙන්, දුකෙන්, දොම්නසින් සහ සුසුම්ලෑමෙන් ඇවිළී ඇතැයි මම කියමි."),
        ("සෝතං ආදිත්තං... ඝානං ආදිත්තං... ජිව්හා ආදිත්තා... කායෝ ආදිත්තෝ... මනෝ ආදිත්තෝ, ධම්මා ආදිත්තා, මනෝවිඤ්ඤාණං ආදිත්තං.", "කන ද ඇවිළී ඇත... නාසය ද ඇවිළී ඇත... දිව ද ඇවිළී ඇත... කය ද ඇවිළී ඇත... මනස ද ඇවිළී ඇත, ධර්මයෝ ද ඇවිළී ඇත, මනෝ විඤ්ඤාණය ද ඇවිළී ඇත."),
        ("ඒවං පස්සං භික්ඛවේ සුතවා අරියසාවකෝ චක්ඛුස්මිම්පි නිබ්බින්දති, රූපේසුපි නිබ්බින්දති, සෝතස්මිම්පි නිබ්බින්දති... මනස්මිම්පි නිබ්බින්දති.", "මහණෙනි, මෙසේ දක්නා ශ්‍රැතවත් ආර්ය ශ්‍රාවක තෙමේ ඇසෙහි ද කලකිරෙයි, රූපයන්හි ද කලකිරෙයි, කනෙහි ද කලකිරෙයි... මනසෙහි ද කලකිරෙයි."),
        ("නිබ්බින්දං විරජ්ජති, විරාගා විමුච්චති. විමුත්තස්මිං විමුත්තමිති ඤාණං හෝති. ඛීණා ජාති, වුසිතං බ්‍රහ්මචරියං, කතං කරණීයං, නාපරං ඉත්ථත්තායාති පජානාතීති.", "කලකිරුණේ නොඇලෙයි, නොඇල්මෙන් කෙලෙසුන්ගෙන් මිදෙයි. මිදුණු කල්හි මිදුණේ යයි නුවණ පහළ වෙයි. ජාතිය ක්ෂය විය, බඹසර වැස නිමවන ලදී, කළ යුත්ත කරන ලදී, නිවන පිණිස මින් මතු කළ යුතු කිසිවක් නැතැයි ප්‍රත්‍යක්ෂයෙන් ම දැනගනියි."),
        ("ඉදමවෝච භගවා. අත්තමනා තේ භික්ඛූ භගවතෝ භාසිතං අභිනන්දුන්ති. ඉමස්මිඤ්ච පන වෙය්‍යාකරණස්මිං භඤ්ඤමානේ තස්ස භික්ඛුසහස්සස්ස අනුපාදාය ආසවේහි චිත්තානි විමුච්චිංසූති.", "භාග්‍යවතුන් වහන්සේ මේ උතුම් දේශනාව වදාළ සේක. සතුටු සිත් ඇති ඒ දහසක් භික්ෂූහු භාග්‍යවතුන් වහන්සේගේ වචනය සාදරයෙන් පිළිගත්හ. මේ ධර්ම දේශනාව පවත්වන කල්හි ඒ දහසක් භික්ෂූන්ගේ සිත් සියලු ආශ්‍රවයන්ගෙන් සහමුලින් ම මිදී අරහත්වයට පත් වූහ.")
    ]},
    {"id": 19, "source": "pitaka_std", "key": "07-dasadhamma", "titlePali": "Dasa Dhamma Suttam", "titleSinhala": "දසධම්ම සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 34, "isFavorite": 0},
    {"id": 20, "source": "namo", "slug": "vasala-sutta", "titlePali": "Vasala Suttam", "titleSinhala": "වසල සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 35, "isFavorite": 0},
    {"id": 21, "source": "pitaka_std", "key": "27-parabhava-suttam", "titlePali": "Parabhava Suttam", "titleSinhala": "පරාභව සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 36, "isFavorite": 0},
    {"id": 22, "source": "pitaka_std", "key": "25-alavaka-suttam", "titlePali": "Alavaka Suttam", "titleSinhala": "ආලවක සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 37, "isFavorite": 0},
    {"id": 28, "source": "pitaka_std", "key": "12-mettanisamsa", "titlePali": "Mettanisansa Suttam", "titleSinhala": "මෙත්තානිසංස සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 38, "isFavorite": 0},
    {"id": 29, "source": "pitaka_std", "key": "13-mittanisamsa", "titlePali": "Mittanisansa Suttam", "titleSinhala": "මිත්තානිසංස සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 39, "isFavorite": 0},
    {"id": 30, "source": "namo", "slug": "chakkavatti-seehanada-sutra", "titlePali": "Cakkavatti Sihanada Suttam", "titleSinhala": "චක්කවත්ති සීහනාද සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 40, "isFavorite": 0},
    {"id": 31, "source": "namo", "slug": "nivapa-sutta", "titlePali": "Nivapa Suttam", "titleSinhala": "නිවාප සූත්‍රය", "category": "විශේෂ සූත්‍ර", "orderIndex": 41, "isFavorite": 0},
]


def init_database(cursor: sqlite3.Cursor):
    cursor.execute("DROP TABLE IF EXISTS android_metadata;")
    cursor.execute("DROP TABLE IF EXISTS verses;")
    cursor.execute("DROP TABLE IF EXISTS suttas;")
    cursor.execute("DROP TABLE IF EXISTS room_master_table;")
    
    cursor.execute("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT DEFAULT 'en_US');")
    cursor.execute("INSERT INTO android_metadata VALUES ('en_US');")
    
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS `suttas` (
        `id` INTEGER NOT NULL,
        `titlePali` TEXT NOT NULL,
        `titleSinhala` TEXT NOT NULL,
        `category` TEXT NOT NULL,
        `orderIndex` INTEGER NOT NULL,
        `isFavorite` INTEGER NOT NULL,
        PRIMARY KEY(`id`)
    );
    """)
    
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS `verses` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `suttaId` INTEGER NOT NULL,
        `verseNumber` INTEGER NOT NULL,
        `paliText` TEXT NOT NULL,
        `sinhalaMeaning` TEXT NOT NULL,
        FOREIGN KEY(`suttaId`) REFERENCES `suttas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
    );
    """)
    
    cursor.execute("CREATE INDEX IF NOT EXISTS `index_verses_suttaId` ON `verses` (`suttaId`);")
    cursor.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);")
    cursor.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, 'bb17240cd8218313209c94227e01012d');")


def main():
    print("==================================================================")
    print(" PIRITH DATABASE GENERATOR v2.0.0 (pitaka.lk & tripitaka.online)")
    print("==================================================================")
    
    os.makedirs(DB_DIR, exist_ok=True)
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print(f"Cleaned previous database at: {DB_PATH}")
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    init_database(cursor)
    
    js_content = fetch_pitaka_js()
    session = requests.Session()
    
    # Pre-fetch Atanatiya
    atanatiya_verses = fetch_tripitaka_atanatiya()
    
    total_suttas = 0
    total_verses = 0
    
    for s_info in SUTTAS_CONFIG:
        s_id = s_info["id"]
        source = s_info["source"]
        title_sin = s_info["titleSinhala"]
        print(f"\nProcessing Sutta [{s_id}] {title_sin} (source: {source})...")
        
        extracted_verses = []
        if source == "pitaka":
            if s_id == 1:
                extracted_verses = get_pitaka_saranagamanam(js_content)
            elif s_id == 3:
                extracted_verses = get_pitaka_dasasikkhapadani(js_content)
        elif source == "pitaka_theruwan":
            extracted_verses = get_pitaka_theruwan(js_content)
        elif source == "pitaka_mahamangala":
            extracted_verses = get_pitaka_mahamangala(js_content)
        elif source == "pitaka_rathana":
            extracted_verses = get_pitaka_rathana(js_content)
        elif source == "pitaka_karaniya":
            extracted_verses = get_pitaka_karaniya(js_content)
        elif source == "pitaka_std":
            key = s_info["key"]
            extracted_verses = get_pitaka_standard(key, js_content)
        elif source == "tripitaka_online":
            extracted_verses = atanatiya_verses
        elif source == "namo":
            slug = s_info["slug"]
            extracted_verses = scrape_namo_page(slug, session)
        elif source == "namo_puja_slice":
            slug = s_info["slug"]
            all_puja = scrape_namo_page(slug, session)
            s_start = s_info["slice_start"]
            s_end = s_info["slice_end"]
            extracted_verses = all_puja[s_start:s_end]
        elif source == "static":
            extracted_verses = s_info.get("verses", [])
            
        if not extracted_verses:
            print(f"  [ERROR] No verses extracted for [{s_id}] {title_sin}!")
            continue
            
        cursor.execute("""
            INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (s_id, s_info["titlePali"], s_info["titleSinhala"], s_info["category"], s_info["orderIndex"], s_info["isFavorite"]))
        total_suttas += 1
        
        v_num = 1
        for p, s in extracted_verses:
            p_clean = clean_verse_text(p)
            s_clean = clean_verse_text(s)
            cursor.execute("""
                INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning)
                VALUES (?, ?, ?, ?)
            """, (s_id, v_num, p_clean, s_clean))
            v_num += 1
            total_verses += 1
            
        print(f"  -> Successfully stored {len(extracted_verses)} verses.")
        
    conn.commit()
    
    # Audit verification
    cursor.execute("SELECT count(*) FROM suttas;")
    sutta_cnt = cursor.fetchone()[0]
    cursor.execute("SELECT count(*) FROM verses;")
    verse_cnt = cursor.fetchone()[0]
    
    print("\n==================================================================")
    print(" VERIFICATION & STATS")
    print("==================================================================")
    print(f"Database File: {DB_PATH}")
    print(f"File Size: {os.path.getsize(DB_PATH) / 1024:.2f} KB")
    print(f"Total Suttas Extracted: {sutta_cnt}")
    print(f"Total Verses Extracted: {verse_cnt}")
    
    cursor.execute("SELECT category, count(*) FROM suttas GROUP BY category;")
    categories = cursor.fetchall()
    for cat, cnt in categories:
        print(f"  - {cat}: {cnt} suttas")
        
    conn.close()
    print("==================================================================\n")

if __name__ == "__main__":
    main()
