#!/usr/bin/env python3
"""
scripts/extract_pirith_db.py
Automated web scraping from https://namo.lk/ and pre-populated SQLite asset database generation.
Outputs to: app/src/main/assets/databases/pirith.db
"""

import os
import re
import sqlite3
import requests
from bs4 import BeautifulSoup

DB_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases")
DB_PATH = os.path.join(DB_DIR, "pirith.db")

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
}

def clean_sinhala_text(text: str) -> str:
    """
    Cleans Unicode text:
    - Removes zero-width spaces (\u200B) and byte-order-marks (\uFEFF)
    - Replaces non-breaking space (\u00A0) with normal space
    - Preserves Sinhala Zero-Width Joiner (\u200D) which is essential for Yansaya, Rakaransaya, and Bandi Akuru
    - Normalizes spaces and trims lines
    """
    if not text:
        return ""
    text = text.replace("\u200b", "").replace("\ufeff", "").replace("\r", "")
    text = text.replace("\u00a0", " ")
    text = re.sub(r"[ \t]+", " ", text)
    lines = [line.strip() for line in text.split("\n")]
    return "\n".join([line for line in lines if line]).strip()


def is_english_module(text: str) -> bool:
    """
    Checks if a block is mostly English text to filter out namo.lk English translations.
    """
    eng = len(re.findall(r"[a-zA-Z]", text))
    sin = len(re.findall(r"[\u0D80-\u0DFF]", text))
    return eng > 10 and sin < 5


def is_noise_module(text: str) -> bool:
    """
    Filters out comments, related posts, website footers, and source attributions.
    """
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


def clean_verse_text(text: str) -> str:
    """
    Cleans up stray Roman numeral section markings (e.g. '6 – viii.', '1.', etc.) while retaining Pali/Sinhala content.
    """
    text = clean_sinhala_text(text)
    # Remove leading numbering like '6 – viii.' or '6 – iv.'
    text = re.sub(r"^\d+\s*–\s*[ivxIVXLC]+\.\s*", "", text)
    return text.strip()


def scrape_namo_page(slug: str, session: requests.Session):
    """
    Scrapes a Pirith/Sutta page from namo.lk and extracts (pali, sinhala) verse pairs.
    """
    url = f"https://namo.lk/{slug}/"
    try:
        resp = session.get(url, headers=HEADERS, timeout=15)
        if resp.status_code != 200:
            print(f"  [WARN] Failed to fetch {url} (status: {resp.status_code})")
            return []
        
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
        
        # Special handling for Karaniya Metta Sutta:
        # 10 verses of (Pali, Sinhala Prose, Sinhala Kavi) + 1 blessing module
        if slug == "karaneeyametta-sutta":
            i = 0
            while i < len(cleaned_modules):
                item = cleaned_modules[i]
                if i + 2 < len(cleaned_modules) and (item.startswith(("1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.")) or "කරණීය" in item):
                    pali = clean_verse_text(item)
                    prose = clean_verse_text(cleaned_modules[i + 1])
                    kavi = clean_verse_text(cleaned_modules[i + 2])
                    sinhala = f"{prose}\n\n[කවි අර්ථය]\n{kavi}"
                    verses.append((pali, sinhala))
                    i += 3
                elif "සුවත්ථි" in item or "සෙත් වේවා" in item:
                    lines = [l.strip() for l in item.split("\n") if l.strip()]
                    if len(lines) >= 2:
                        verses.append((lines[0], "\n".join(lines[1:])))
                    else:
                        verses.append((item, item))
                    i += 1
                else:
                    if i + 1 < len(cleaned_modules):
                        verses.append((clean_verse_text(item), clean_verse_text(cleaned_modules[i + 1])))
                        i += 2
                    else:
                        i += 1
            return verses

        # Special handling for Guna & Meditation modules where each module has Pali on line 1 and Sinhala on subsequent lines:
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

        # Special handling for Cakkavatti Sihanada Sutta: drop initial 2 title/description modules
        if slug == "chakkavatti-seehanada-sutra":
            if cleaned_modules and "චක්කවත්ති" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]
            if cleaned_modules and "සිහනදක්" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]

        # Special handling for Budhu Guna Piritha: drop initial instruction note
        if slug == "budhu-guna-piritha":
            if cleaned_modules and "රෝගී වූ විට" in cleaned_modules[0]:
                cleaned_modules = cleaned_modules[1:]

        # Drop known introductory modules in certain suttas
        if slug in ["alavaka-sutta", "parabhava-sutta"]:
            if cleaned_modules and ("සැවැත් නුවර" in cleaned_modules[0] or "හැඳින්වීම" in cleaned_modules[0]):
                cleaned_modules = cleaned_modules[1:]
                
        # Drop trailing standalone 'සාධූ!'
        if cleaned_modules and cleaned_modules[-1].startswith("සාධූ"):
            cleaned_modules = cleaned_modules[:-1]

        i = 0
        while i < len(cleaned_modules):
            item = cleaned_modules[i]
            
            # Heading triplet check: e.g. '1. නමස්කාරය' followed by pali stanza and sinhala meaning
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
                # Single trailing module: check if it contains both pali and sinhala separated by newline
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


# Canonical supplementary verses for required suttas not present on namo.lk or for comprehensive coverage
SUPPLEMENTARY_SUTTAS = [
    {
        "id": 101,
        "titlePali": "Vattaka Parittam",
        "titleSinhala": "වට්ටක පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 7,
        "isFavorite": 0,
        "verses": [
            (
                "පූරෙන්තං බෝධිසම්භාරේ – නිබ්බත්තං වට්ටජාතියං\nයස්ස තේජේන දාවග්ගි – මහාසත්තං විවජ්ජයි",
                "බෝධි සම්භාරයන් පුරන්නා වූ, වටු කුලයේ උපත ලැබුවා වූ බෝසතුන්ගේ යම් ආනුභාවයකින් ලැව්ගින්න මහාසත්වයන් මඟහැර පලා ගියේ ද,"
            ),
            (
                "ථේරස්ස සාරිපුත්තස්ස – ලෝකනාථේන භාසිතං\nකප්පට්ඨායිං මහාතේජං – පරිත්තං තං භණාම හේ",
                "ලෝකනාථ බුදුරජාණන් වහන්සේ විසින් සාරිපුත්ත මහ තෙරුන් වහන්සේට දේශනා කරන ලද්දා වූ, කල්පයක් මුළුල්ලේ බලපවත්වන මහා තේජස් ඇති ඒ වට්ටක පිරිත අපි දැන් සජ්ඣායනා කරමු."
            ),
            (
                "අත්ථි ලෝකේ සීලගුණෝ – සච්චං සෝචෙය්‍යනුද්දයා\nතේන සච්චේන කාහාමි – සච්චකිරියමනුත්තරං",
                "ලෝකයෙහි ශීල ගුණය ද, සත්‍යය ද, පිරිසිදු බව ද, දයාව ද ඇත. ඒ සත්‍ය බලයෙන් මම උතුම් වූ සත්‍යක්‍රියාවක් කරමි."
            ),
            (
                "ආවජ්ජෙත්වා ධම්මබලං – සරිත්වා පුබ්බකේ ජිනේ\nසච්චබලමවස්සාය – සච්චකිරියමකාසහං",
                "ධර්මයේ බලය සිහිපත් කොට, පෙර බුදුවරුන්ගේ ගුණ සිහිපත් කොට, සත්‍යයේ ශක්තිය පිහිට කොට ගෙන මම මේ සත්‍යක්‍රියාව කෙළෙමි."
            ),
            (
                "සන්ති පක්ඛා අපතනා – සන්ති පාදා අවඤ්චනා\nමාතා පිතා ච නික්ඛන්තා – ජාතවේද පටික්කම",
                "මට පියාපත් ඇත ද පියාසර කළ නොහැක. පාද ඇත ද ඇවිද යා නොහැක. මව්පියෝ ද මා හැර ගියහ. එම්බා ලැව්ගින්න, වහා ආපසු හැරී යව."
            ),
            (
                "සහ සච්චේ කතේ මය්හං – මහාපජ්ජලිතෝ සිඛී\nවජ්ජේසි සෝළස කරීසානි – උදකං පත්වා යථා සිඛී\nසච්චේන මේ සමෝ නත්ථි – ඒසා මේ සච්චපාරමී",
                "මා විසින් මෙසේ සත්‍යක්‍රියාව කරත් ම මහත්සේ ඇවිළෙමින් ආ ලැව්ගින්න, ජලයට පත් ගින්නක් මෙන් කරීස සොළොසක දුරක් ඈතින් මා හැර ගියේ ය. සත්‍යයෙන් මා හා සමාන කිසිවෙක් නැත. මෙය මාගේ සත්‍ය පාරමිතාවයි."
            ),
            (
                "ඒතේන සච්චවජ්ජේන – සබ්බදුක්ඛං විනස්සතු\nඒතේන සච්චවජ්ජේන – හෝතු තේ ජයමංගලං",
                "මේ සත්‍ය වචනයේ ආනුභාවයෙන් සියලු දුක් විනාශ වේවා! මේ සත්‍ය වචනයේ බලයෙන් ඔබට ජය මංගල්‍යයක් ම වේවා!"
            )
        ]
    },
    {
        "id": 102,
        "titlePali": "Atanatiya Suttam",
        "titleSinhala": "ආටානාටිය සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 15,
        "isFavorite": 0,
        "verses": [
            (
                "විපස්සිස්ස නමත්ථු – චක්ඛුමන්තස්ස සිරීමතෝ\nසිඛිස්සපි නමත්ථු – සබ්බභූතානුකම්පිනෝ",
                "පස්මරුන් දිනූ, පිරිවර සිරි ඇති, සිවැස් නුවණැති විපස්සී බුදුරදුන්ට නමස්කාර වේවා. සියලු සතුන් කෙරෙහි අනුකම්පා ඇති සිඛී බුදුරදුන්ට ද නමස්කාර වේවා."
            ),
            (
                "වෙස්සභුස්ස නමත්ථු – නහාතකස්ස තපස්සිනෝ\nනමත්ථු කකුසන්ධස්ස – මාරසේනාපමද්දිනෝ",
                "කෙලෙස් සේදූ, තවුස් ගුණ ඇති වෙස්සභූ බුදුරදුන්ට නමස්කාර වේවා. මාර සේනාව මඩින ලද කකුසඳ බුදුරදුන්ට නමස්කාර වේවා."
            ),
            (
                "කෝණාගමනස්ස නමත්ථු – බ්‍රාහ්මණස්ස වුසීමතෝ\nකස්සපස්ස නමත්ථු – විප්පමුත්තස්ස සබ්බධි",
                "බාහිත පාපී බමුණෙකු වූ, වැස නිම කළ බඹසර ඇති කෝණාගමන බුදුරදුන්ට නමස්කාර වේවා. සියලු කෙලෙසුන් කෙරෙන් මිදුණු කාශ්‍යප බුදුරදුන්ට ද නමස්කාර වේවා."
            ),
            (
                "අංගීරසස්ස නමත්ථු – සක්‍යපුත්තස්ස සිරීමතෝ\nයෝ ඉමං ධම්මමදේසේසි – සබ්බදුක්ඛාපනූදනං",
                "ශ්‍රී සම්පන්න වූ, ශාක්‍ය පුත්‍ර වූ, සියලු දුක් දුරු කරන්නා වූ මේ ශ්‍රී සද්ධර්මය දේශනා කළා වූ අංගීරස නම් ගෞතම බුදුරජාණන් වහන්සේට ද නමස්කාර වේවා."
            ),
            (
                "යේ චාපි නිබ්බුතා ලෝකේ – යථාභූතං විපස්සිසුං\nතේ ජනා අපිසුණාථ – මහන්තා වීතසාරදා",
                "ලෝකයෙහි යම් කෙනෙක් චතුරාර්ය සත්‍යය තතු සේ දැක පිරිනිවුණාහු ද, පහව ගිය බිය ඇති, කේළාම් නොකියන ඒ මහා රහතන් වහන්සේලා ද,"
            ),
            (
                "හිතං දේවමනුස්සානං – යං නමස්සන්ති ගෝතමං\nවිජ්ජාචරණසම්පන්නං – මහන්තං වීතසාරදං",
                "දෙවි මිනිසුන්ට හිතසුව පිණිස විජ්ජාචරණ ගුණයෙන් යුතු වූ, අභීත වූ, මහා ශ්‍රේෂ්ඨ වූ ඒ ගෞතම බුදුරජාණන් වහන්සේට නමස්කාර කරති."
            ),
            (
                "ඒතේ චඤ්ඤේ ච සම්බුද්ධා – අනේකසතකෝටියෝ\nසබ්බේ බුද්ධා අසමසමා – සබ්බේ බුද්ධා මහිද්ධිකා",
                "මේ ගෞතම බුදුරදුන් ද අනෙක් අනේක කෝටි ගණන් බුදුවරු ද, ඒ සියලු බුදුවරු අසමසම වන සේක. සියලු බුදුවරු මහා සෘද්ධි බල ඇත්තාහ."
            ),
            (
                "සබ්බේ දසබලුපේතා – වේසාරජ්ජේහුපාගතා\nසබ්බේ තේ පටිජානන්ති – ආසභණ්ඨානමුත්තමං",
                "සියලු බුදුවරු දසබලධාරී වන සේක. චතුර්වෛශාරද්‍ය ඥානයෙන් යුක්ත වන සේක. උතුම් වූ ශ්‍රේෂ්ඨ ස්ථානය පිළිගන්නා සේක."
            ),
            (
                "සීහනාදං නදන්තේතේ – පරිසාසු විසාරදා\nබ්‍රහ්මචක්කං පවත්තෙන්ති – ලෝකේ අප්පටිවත්තියං",
                "පිරිස් මැද විශාරදව සිංහනාද පවත්වන සේක. ලෝකයෙහි කිසිවෙකුට වැළැක්විය නොහැකි උතුම් බ්‍රහ්මචක්‍රය ප්‍රවර්තනය කරන සේක."
            ),
            (
                "උපේතා බුද්ධධම්මේහි – අට්ඨාරසහි නායකා\nබත්තිංස ලක්ඛණූපේතා – සීතානුව්‍යඤ්ජනාධරා",
                "දහඅටක් බුද්ධ ධර්මයන්ගෙන් යුක්ත වූ ඒ ලෝකනායක බුදුවරු දෙතිස් මහා පුරුෂ ලක්ෂණයන්ගෙන් හා අසූවක් අනුව්‍යංජනයන්ගෙන් බබළන සේක."
            ),
            (
                "බ්‍යාමප්පභාය සුප්පභා – සබ්බේ තේ මුනිපුංගවා\nබුද්ධා සබ්බඤ්ඤුනෝ ඒතේ – සබ්බේ ඛීණාසවා ජිනා",
                "සියලු මුනිවරයෝ බඹයක් දුර විහිදෙන ශරීරාලෝකයෙන් යුක්ත වන සේක. සියල්ල දන්නා සර්වඥ වූ ඒ සියලු බුදුවරු ක්ෂීණාශ්‍රව ජයග්‍රාහකයෝ ය."
            ),
            (
                "මහාපභා මහාතේජා – මහාපඤ්ඤා මහාබලා\nමහාකාරුණිකා ධීරා – සබ්බේසානං සුඛාවහා",
                "මහත් ආලෝක ඇති, මහා තේජස් ඇති, මහා ප්‍රඥා ඇති, මහා ධෛර්ය ඇති, මහා කාරුණික වූ ඒ බුදුවරු සියලු සතුන්ට සුව සලසති."
            ),
            (
                "දීපා නාථා පතිට්ඨා ච – තාණා ලේණා ච පාණිනං\nගති බන්ධූ මහෙස්සාසා – සරණා ච ජනෙසිනෝ",
                "ප්‍රාණීන්ට පහන් වැන්න, පිහිට වැන්න, රැකවරණ වැන්න, සෙවණ වැන්න. සත්වයන්ගේ ආරක්ෂාව, නෑයා, අභයස්ථානය සහ සරණය වන සේක."
            ),
            (
                "සදේවකස්ස ලෝකස්ස – සබ්බේ තේ පවරාවරා\nතේසාහං සිරසා පාදේ – වන්දාමි පුරිසුත්තමේ",
                "දෙවියන් සහිත ලෝකයාගේ අග්‍ර වූ ශ්‍රේෂ්ඨ වූ උතුම් පුරුෂයන් වූ ඒ සියලු බුදුවරුන්ගේ පාද පද්මයන් මම සිරස නමා වන්දනා කරමි."
            ),
            (
                "වචසා මනසා චේව – වන්දාමේතේ තථාගතේ\nසයනේ ආසනේ ඨානේ – ගමනේ චාපි සබ්බදා",
                "කයින් ද, වචනයෙන් ද, සිතින් ද ඒ තථාගතයන් වහන්සේලාට නිදද්දී, හිඳිද්දී, සිටිද්දී, යද්දී සෑම කල්හි ම වන්දනා කරමි."
            ),
            (
                "සදා සුඛේන රක්ඛන්තු – බුද්ධා සන්තිකරා තුවං\nතේහි ත්වං රක්ඛිතෝ සන්තෝ – මුත්තෝ සබ්බභයේහි ච",
                "ශාන්තිය උදා කරන බුදුවරු නිරතුරුව ඔබට සැප සලසා ආරක්ෂා කෙරෙත්වා! උන්වහන්සේලාගේ ආරක්ෂාව ලබමින් සියලු භය උපද්‍රවයන්ගෙන් මිදෙත්වා!"
            ),
            (
                "සබ්බරෝගා විනීමුත්තෝ – සබ්බසන්තාපවජ්ජිතෝ\nසබ්බවේරමතික්කන්තෝ – නිබ්බුතෝ ච තුවං භව",
                "සියලු රෝගයන්ගෙන් මිදුණාහු, සියලු තැවුල් දුරු වූවාහු, සියලු වෛරයන් ඉක්මවා ගියාහු ශාන්තියට පත් වෙත්වා!"
            ),
            (
                "තේසං සච්චේන සීලේන – ඛන්තිමෙත්තාබලේන ච\nතේපි තුවං අනුරක්ඛන්තු – ආරෝග්‍යේන සුඛේන ච",
                "උන්වහන්සේලාගේ සත්‍යයෙන් ද, ශීලයෙන් ද, ඉවසීමේ හා මෛත්‍රී බලයෙන් ද, නිරෝගී සුවයෙන් ඔබව ආරක්ෂා කරත්වා!"
            )
        ]
    },
    {
        "id": 103,
        "titlePali": "Atavisi Parittam",
        "titleSinhala": "අටවිසි පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 12,
        "isFavorite": 0,
        "verses": [
            (
                "තණ්හංකරෝ මහාවීරෝ – මේධංකරෝ මහායසෝ\nසරණංකරෝ ලෝකහිතෝ – දීපංකරෝ ජුතින්ධරෝ",
                "මහා වීර වූ තණ්හංකර බුදුරදුන් ද, මහා යසස් ඇති මේධංකර බුදුරදුන් ද, ලෝක හිතසුව පිණිස වැඩ විසූ සරණංකර බුදුරදුන් ද, මහා කාන්තිමත් දීපංකර බුදුරදුන් ද,"
            ),
            (
                "කොණ්ඩඤ්ඤෝ ජනපාමොක්ඛෝ – මංගලෝ පුරිසාසභෝ\nසුමනෝ සුමනෝ ධීරෝ – රේවතෝ රතිවඩ්ඪනෝ",
                "ජනයාට ප්‍රධාන වූ කොණ්ඩඤ්ඤ බුදුරදුන් ද, පුරුෂෝත්තම වූ මංගල බුදුරදුන් ද, යහපත් සිත් ඇති ධීර වූ සුමන බුදුරදුන් ද, ප්‍රීතිය වඩන්නා වූ රේවත බුදුරදුන් ද,"
            ),
            (
                "සෝභිතෝ ගුණසම්පන්නෝ – අනෝමදස්සී ජනුත්තමෝ\nපදුමෝ ලෝකපජ්ජෝතෝ – නාරදෝ වරසාරථී",
                "ගුණ සම්පන්න ශෝභිත බුදුරදුන් ද, උතුම් වූ අනෝමදස්සී බුදුරදුන් ද, ලෝකයට පහනක් බඳු පදුම බුදුරදුන් ද, උතුම් සාරථී වූ නාරද බුදුරදුන් ද,"
            ),
            (
                "පදුමුත්තරෝ ලෝකසාරෝ – සුමේධෝ අග්ගපුග්ගලෝ\nසුජාතෝ සබ්බලෝකග්ගෝ – පියදස්සී නරාසභෝ",
                "ලෝකයට සාර වූ පදුමුත්තර බුදුරදුන් ද, අග්‍ර පුද්ගල වූ සුමේධ බුදුරදුන් ද, සියලු ලොවට අග්‍ර සුජාත බුදුරදුන් ද, මිනිසුන් අතර ශ්‍රේෂ්ඨ පියදස්සී බුදුරදුන් ද,"
            ),
            (
                "අත්ථදස්සී කාරුණිකෝ – ධම්මදස්සී තමෝනුදෝ\nසිද්ධත්ථෝ අසමෝ ලෝකේ – තිස්සෝ වරදසංවරෝ",
                "මහා කාරුණික අත්ථදස්සී බුදුරදුන් ද, අඳුර දුරු කළ ධම්මදස්සී බුදුරදුන් ද, ලොව අසමසම වූ සිද්ධත්ථ බුදුරදුන් ද, සංවර වූ තිස්ස බුදුරදුන් ද,"
            ),
            (
                "ඵුස්සෝ වරදසම්බුද්ධෝ – විපස්සී ච අනූපමෝ\nසිඛී සබ්බහිතෝ සත්ථා – වෙස්සභූ සුඛදායකෝ",
                "ශ්‍රේෂ්ඨ සම්බුද්ධ වූ ඵුස්ස බුදුරදුන් ද, උපමා රහිත විපස්සී බුදුරදුන් ද, සියලු සතුන්ට හිතැති ශාස්තෘ වූ සිඛී බුදුරදුන් ද, සුව සලසන වෙස්සභූ බුදුරදුන් ද,"
            ),
            (
                "කකුසන්ධෝ සත්ථවාහෝ – කෝණාගමනෝ රණඤ්ජහෝ\nකස්සපෝ සිරිසම්පන්නෝ – ගෝතමෝ සක්‍යපුංගවෝ",
                "ශාස්තෘ වූ කකුසඳ බුදුරදුන් ද, කෙලෙස් යුද්ධය ජයගත් කෝණාගමන බුදුරදුන් ද, සිරි සම්පන්න කාශ්‍යප බුදුරදුන් ද, ශාක්‍ය ශ්‍රේෂ්ඨ ගෞතම බුදුරදුන් ද,"
            ),
            (
                "ඒතේ සබ්බේ මහාවීරා – සබ්බලෝකහිතේසිනෝ\nතේසං සච්චේන සීලේන – ඛන්තිමෙත්තාබලේන ච\nසදා සුඛේන රක්ඛන්තු – ආරෝග්‍යේන සුඛේන ච",
                "මේ සියලු මහා වීර වූ සර්ව ලෝක හිතෛෂී අටවිසි බුදුරජාණන් වහන්සේලාගේ සත්‍යයෙන් ද, ශීලයෙන් ද, ඉවසීම හා මෙත් බලයෙන් ද ඔබ සැම කල්හි නිරෝගීව සුවපත් වෙත්වා!"
            )
        ]
    },
    {
        "id": 104,
        "titlePali": "Jinapanjara Parittam",
        "titleSinhala": "ජිනපඤ්ජර පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 13,
        "isFavorite": 0,
        "verses": [
            (
                "ජයන්තෝ බෝධියා මූලේ – සක්‍යානං නන්දිවඩ්ඪනෝ\nඒවං ත්වං විජයෝ හෝහි – ජයස්සු ජයමංගලේ",
                "බෝරුක් මුල දී මරුන් පරදා ජය ගෙන ශාක්‍යයන්ගේ සතුට වඩන්නා වූ භාග්‍යවතුන් වහන්සේ මෙන් ඔබට ද ජය වේවා! ජය මංගල්‍යයන් අත්වේවා!"
            ),
            (
                "සිරෝමේ බුද්ධසෙට්ඨෝ ච – සාරිපුත්තෝ ච දක්ඛිණේ\nවාමේ ච මොග්ගල්ලානෝ මේ – පුරතෝ ධම්මදේසනා",
                "මාගේ සිරසෙහි උතුම් බුදුරජාණන් වහන්සේ ද, දකුණු පස සාරිපුත්ත මහරහතන් වහන්සේ ද, වම් පස මොග්ගල්ලාන මහරහතන් වහන්සේ ද, ඉදිරියෙහි ධර්ම දේශනාව ද පිහිටත්වා!"
            ),
            (
                "පච්ඡිමේ ච ආනන්දෝ මේ – දක්ඛිණේ රාහුලෝ තථා\nකස්සපෝ ච මහානාමෝ – උභෝසුං වාමසෝතකේ",
                "පසුපස ආනන්ද හිමියන් ද, දකුණෙහි රාහුල කුමරුන් ද, කස්සප හා මහානාම තෙරවරු දෙපස ද වැඩ සිටිත්වා!"
            ),
            (
                "ජිනපඤ්ජරමජ්ඣට්ඨා – විහරන්තං මහීතලේ\nසදා රක්ඛන්තු මං සබ්බේ – තේ මහාවීරසාධවෝ",
                "මෙලෙස ජිනපඤ්ජරය නම් වූ රක්ෂාවරණය මැද මහපොළොවෙහි වසන මා හට ඒ සියලු මහා වීර සත්පුරුෂ උතුමෝ නිරතුරුව රැකවරණය සලසත්වා!"
            ),
            (
                "ඉච්චේවමච්චන්තකතෝ සුරක්ඛෝ – ජිනානුභාවේන ජිතූපද්දවෝ\nබුද්ධානුභාවේන හතාරිසංඝෝ – චරාහි සද්ධම්මමනුභාවපාලිතෝ",
                "මෙසේ මනා කොට කරන ලද මහා ආරක්ෂාවෙන් යුතු ව, ජින ආනුභාවයෙන් සියලු උපද්‍රවයන් මැඩපවත්වා, බුද්ධානුභාවයෙන් සතුරු බිය දුරු කර, සද්ධර්මයේ ආනුභාවයෙන් සුරැකිව හැසිරෙත්වා!"
            )
        ]
    },
    {
        "id": 105,
        "titlePali": "Anattalakkhana Suttam",
        "titleSinhala": "අනත්තලක්ඛණ සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 18,
        "isFavorite": 0,
        "verses": [
            (
                "ඒවං මේ සුතං. ඒකං සමයං භගවා බාරාණසියං විහරති ඉසිපතනේ මිගදායේ.\nතත්‍ර ඛෝ භගවා පඤ්චවග්ගියේ භික්ඛූ ආමන්තේසි භික්ඛවෝති.",
                "මා විසින් මෙසේ අසන ලදී. එක් සමයෙක භාග්‍යවතුන් වහන්සේ බරණැස ඉසිපතන මිගදායෙහි වැඩවසන සේක. එහි දී භාග්‍යවතුන් වහන්සේ පස්වග මහණුන් අමතා 'මහණෙනි' යි වදාළ සේක."
            ),
            (
                "රූපං භික්ඛවේ අනත්තා. රූපඤ්ච හිදං භික්ඛවේ අත්තා අභවිස්ස, නයිදං රූපං ආබාධාය සංවත්තේථ, ලබ්භේථ ච රූපේ: ඒවං මේ රූපං හෝතු, ඒවං මේ රූපං මා අහෝසීති.",
                "මහණෙනි, රූපය අනාත්ම ය. මහණෙනි, ඉදින් මේ රූපය ආත්මය වී නම්, මේ රූපය පෙළීමට හේතු නොවන්නේ ය. තවද 'මාගේ රූපය මෙසේ වේවා, මාගේ රූපය මෙසේ නොවේවා' යි රූපය කෙරෙහි කැමැත්ත පරිදි පැවැත්විය හැකි වන්නේ ය."
            ),
            (
                "යස්මා ච ඛෝ භික්ඛවේ රූපං අනත්තා, තස්මා රූපං ආබාධාය සංවත්තති, න ච ලබ්භති රූපේ: ඒවං මේ රූපං හෝතු, ඒවං මේ රූපං මා අහෝසීති.",
                "මහණෙනි, යම් හෙයකින් රූපය අනාත්ම වේ ද, එහෙයින් රූපය පෙළීමට හේතු වෙයි. 'මාගේ රූපය මෙසේ වේවා, මෙසේ නොවේවා' යි කැමැත්ත පරිදි ද නොලැබෙයි."
            ),
            (
                "වේදනා අනත්තා... සඤ්ඤා අනත්තා... සංඛාරා අනත්තා... විඤ්ඤාණං අනත්තා.",
                "වේදනාව ද අනාත්ම ය... සංඥාව ද අනාත්ම ය... සංස්කාරයෝ ද අනාත්ම ය... විඤ්ඤාණය ද අනාත්ම ය."
            ),
            (
                "තං කිං මඤ්ඤථ භික්ඛවේ, රූපං නිච්චං වා අනිච්චං වාති? අනිච්චං භන්තේ. යං පනානිච්චං දුක්ඛං වා තං සුඛං වාති? දුක්ඛං භන්තේ.",
                "මහණෙනි, ඒ කුමකැයි සිතන්නහු ද? රූපය නිත්‍ය ද අනිත්‍ය ද? 'ස්වාමීනි, අනිත්‍ය ය.' යමක් අනිත්‍ය නම් එය දුක් ද සැප ද? 'ස්වාමීනි, දුකකි.'"
            ),
            (
                "යං පනානිච්චං දුක්ඛං විපරිණාමධම්මං, කල්ලං නු තං සමනුපස්සිතුං: ඒතං මම, ඒසෝහමස්මි, ඒසෝ මේ අත්තාති? නෝ හේතං භන්තේ.",
                "යමක් අනිත්‍ය නම්, දුක් නම්, පෙරළෙන ස්වභාව ඇත්නම්, එය 'තෙල මාගේ ය, තෙල මම වෙමි, තෙල මාගේ ආත්මය' යි දැකීම සුදුසු ද? 'ස්වාමීනි, එය නොවේ ම ය.'"
            ),
            (
                "ඒවං පස්සං භික්ඛවේ සුතවා අරියසාවකෝ රූපස්මිම්පි නිබ්බින්දති, වේදනායපි නිබ්බින්දති, සඤ්ඤායපි නිබ්බින්දති, සංඛාරේසුපි නිබ්බින්දති, විඤ්ඤාණස්මිම්පි නිබ්බින්දති.",
                "මහණෙනි, මෙසේ දක්නා ශ්‍රැතවත් ආර්ය ශ්‍රාවක තෙම රූපය කෙරෙහි ද කලකිරෙයි. වේදනාව කෙරෙහි ද කලකිරෙයි. සංඥාව කෙරෙහි ද කලකිරෙයි. සංස්කාරයන් කෙරෙහි ද කලකිරෙයි. විඤ්ඤාණය කෙරෙහි ද කලකිරෙයි."
            ),
            (
                "නිබ්බින්දං විරජ්ජති, විරාගා විමුච්චති, විමුත්තස්මිං විමුත්තමිති ඤාණං හෝති: ඛීණා ජාති, වුසිතං බ්‍රහ්මචරියං, කතං කරණීයං, නාපරං ඉත්ථත්තායාති පජානාතීති.",
                "කලකිරෙන්නේ නොඇලෙයි. නොඇල්මෙන් මිදෙයි. මිදුණු කල්හි 'මිදුණේ ය' යන ඥානය වෙයි. 'ජාතිය ක්ෂය විය. බඹසර වැස නිමවන ලදී. කළ යුතු දේ කරන ලදී. මෙයින් මතු කළ යුතු අන් කිසිවක් නැතැ'යි දැනගනියි."
            ),
            (
                "ඉදමවෝච භගවා. අත්තමනා පඤ්චවග්ගියා භික්ඛූ භගවතෝ භාසිතං අභිනන්දුන්ති. ඉමස්මිඤ්ච පන වෙය්‍යාකරණස්මිං භඤ්ඤමානේ පඤ්චවග්ගියානං භික්ඛූනං අනුපාදාය ආසවේහි චිත්තානි විමුච්චිංසූති.",
                "භාග්‍යවතුන් වහන්සේ මෙය වදාළ සේක. සතුටු සිත් ඇති පස්වග මහණහු භාග්‍යවතුන් වහන්සේගේ වචනය සාදරයෙන් පිළිගත්හ. මේ ධර්ම දේශනාව පවත්වන කල්හි පස්වග තෙරවරුන්ගේ සිත් දැඩිව නොගෙන ආශ්‍රවයන්ගෙන් සපුරා මිදුණාහු ය."
            )
        ]
    },
    {
        "id": 106,
        "titlePali": "Adittapariyaya Suttam",
        "titleSinhala": "ආදිත්තපරියාය සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 19,
        "isFavorite": 0,
        "verses": [
            (
                "ඒවං මේ සුතං. ඒකං සමයං භගවා ගයායං විහරති ගයාසීසේ සද්ධිං භික්ඛුසහස්සේන. තත්‍ර ඛෝ භගවා භික්ඛූ ආමන්තේසි:",
                "මා විසින් මෙසේ අසන ලදී. එක් කලෙක භාග්‍යවතුන් වහන්සේ දහසක් භික්ෂූන් සමඟ ගයාවෙහි ගයාසීස නම් පර්වතයෙහි වැඩවසන සේක. එහි දී භාග්‍යවතුන් වහන්සේ භික්ෂූන් අමතා මෙසේ වදාළ සේක:"
            ),
            (
                "සබ්බං භික්ඛවේ ආදිත්තං. කිඤ්ච භික්ඛවේ සබ්බං ආදිත්තං? චක්ඛුං භික්ඛවේ ආදිත්තං, රූපා ආදිත්තා, චක්ඛුවිඤ්ඤාණං ආදිත්තං, චක්ඛුසම්ඵස්සෝ ආදිත්තෝ.",
                "මහණෙනි, සියල්ල ගිනි ගෙන ඇවිළෙන්නේ ය. මහණෙනි, ගිනි ගෙන ඇවිළෙන සියල්ල කුමක් ද? මහණෙනි, ඇස ගිනි ගෙන ඇත, රූප ගිනි ගෙන ඇත, ඇසේ විඤ්ඤාණය ගිනි ගෙන ඇත, ඇසේ ස්පර්ශය ගිනි ගෙන ඇත."
            ),
            (
                "කේන ආදිත්තං? රාගග්ගිනා, දෝසග්ගිනා, මෝහග්ගිනා ආදිත්තං, ජාතියා ජරාමරණේන සෝකේහි පරිදේවේහි දුක්ඛේහි දෝමනස්සේහි උපායාසේහි ආදිත්තන්ති වදාමි.",
                "කුමකින් ඇවිළී තිබේ ද? රාග ගින්නෙන්, ද්වේෂ ගින්නෙන්, මෝහ ගින්නෙන් ඇවිළී තිබේ. ජාතියෙන්, ජරාවෙන්, මරණයෙන්, ශෝකයෙන්, වැළපීමෙන්, දුකෙන්, දොම්නසින් සහ සුසුම්ලෑමෙන් ඇවිළී ඇතැයි මම කියමි."
            ),
            (
                "සෝතං ආදිත්තං... ඝානං ආදිත්තං... ජිව්හා ආදිත්තා... කායෝ ආදිත්තෝ... මනෝ ආදිත්තෝ, ධම්මා ආදිත්තා, මනෝවිඤ්ඤාණං ආදිත්තං.",
                "කන ද ඇවිළී ඇත... නාසය ද ඇවිළී ඇත... දිව ද ඇවිළී ඇත... කය ද ඇවිළී ඇත... මනස ද ඇවිළී ඇත, ධර්මයෝ ද ඇවිළී ඇත, මනෝ විඤ්ඤාණය ද ඇවිළී ඇත."
            ),
            (
                "ඒවං පස්සං භික්ඛවේ සුතවා අරියසාවකෝ චක්ඛුස්මිම්පි නිබ්බින්දති, රූපේසුපි නිබ්බින්දති, සෝතස්මිම්පි නිබ්බින්දති... මනස්මිම්පි නිබ්බින්දති.",
                "මහණෙනි, මෙසේ දක්නා ශ්‍රැතවත් ආර්ය ශ්‍රාවක තෙමේ ඇසෙහි ද කලකිරෙයි, රූපයන්හි ද කලකිරෙයි, කනෙහි ද කලකිරෙයි... මනසෙහි ද කලකිරෙයි."
            ),
            (
                "නිබ්බින්දං විරජ්ජති, විරාගා විමුච්චති. විමුත්තස්මිං විමුත්තමිති ඤාණං හෝති. ඛීණා ජාති, වුසිතං බ්‍රහ්මචරියං, කතං කරණීයං, නාපරං ඉත්ථත්තායාති පජානාතීති.",
                "කලකිරුණේ නොඇලෙයි, නොඇල්මෙන් කෙලෙසුන්ගෙන් මිදෙයි. මිදුණු කල්හි මිදුණේ යයි නුවණ පහළ වෙයි. ජාතිය ක්ෂය විය, බඹසර වැස නිමවන ලදී, කළ යුත්ත කරන ලදී, නිවන පිණිස මින් මතු කළ යුතු කිසිවක් නැතැයි ප්‍රත්‍යක්ෂයෙන් ම දැනගනියි."
            ),
            (
                "ඉදමවෝච භගවා. අත්තමනා තේ භික්ඛූ භගවතෝ භාසිතං අභිනන්දුන්ති. ඉමස්මිඤ්ච පන වෙය්‍යාකරණස්මිං භඤ්ඤමානේ තස්ස භික්ඛුසහස්සස්ස අනුපාදාය ආසවේහි චිත්තානි විමුච්චිංසූති.",
                "භාග්‍යවතුන් වහන්සේ මේ උතුම් දේශනාව වදාළ සේක. සතුටු සිත් ඇති ඒ දහසක් භික්ෂූහු භාග්‍යවතුන් වහන්සේගේ වචනය සාදරයෙන් පිළිගත්හ. මේ ධර්ම දේශනාව පවත්වන කල්හි ඒ දහසක් භික්ෂූන්ගේ සිත් සියලු ආශ්‍රවයන්ගෙන් සහමුලින් ම මිදී අරහත්වයට පත් වූහ."
            )
        ]
    }
]

# Suttas mapped from namo.lk with metadata
NAMO_SUTTAS = [
    {
        "id": 1,
        "slug": "saranagamanaya",
        "titlePali": "Namaskara & Saranagamanam",
        "titleSinhala": "නමස්කාරය සහ තිසරණය",
        "category": "වන්දනා",
        "orderIndex": 1,
        "isFavorite": 1
    },
    {
        "id": 2,
        "slug": "theruwan-namadima",
        "titlePali": "Ratanattaya Vandana",
        "titleSinhala": "තෙරුවන් නැමදීම (රත්නත්‍රය වන්දනා)",
        "category": "වන්දනා",
        "orderIndex": 2,
        "isFavorite": 1
    },
    {
        "id": 3,
        "slug": "ten-precepts",
        "titlePali": "Dasa Sila Vandana",
        "titleSinhala": "දස සිල් සහ පන්සිල් වන්දනාව",
        "category": "වන්දනා",
        "orderIndex": 3,
        "isFavorite": 0
    },
    {
        "id": 4,
        "slug": "maha-mangala-sutta",
        "titlePali": "Maha Mangala Suttam",
        "titleSinhala": "මහා මංගල සූත්‍රය",
        "category": "මහා පිරිත්",
        "orderIndex": 1,
        "isFavorite": 1
    },
    {
        "id": 5,
        "slug": "rathana-sutta",
        "titlePali": "Ratana Suttam",
        "titleSinhala": "රතන සූත්‍රය",
        "category": "මහා පිරිත්",
        "orderIndex": 2,
        "isFavorite": 1
    },
    {
        "id": 6,
        "slug": "karaneeyametta-sutta",
        "titlePali": "Karaniya Metta Suttam",
        "titleSinhala": "කරණීය මෙත්ත සූත්‍රය",
        "category": "මහා පිරිත්",
        "orderIndex": 3,
        "isFavorite": 1
    },
    {
        "id": 7,
        "slug": "kanda-piritha",
        "titlePali": "Khandha Parittam",
        "titleSinhala": "ඛන්ධ පිරිත (කඳ පිරිත)",
        "category": "සෙත් පිරිත්",
        "orderIndex": 4,
        "isFavorite": 0
    },
    {
        "id": 8,
        "slug": "mora-piritha",
        "titlePali": "Mora Parittam",
        "titleSinhala": "මෝර පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 5,
        "isFavorite": 0
    },
    {
        "id": 9,
        "slug": "dajagga-piritha",
        "titlePali": "Dhajagga Suttam",
        "titleSinhala": "ධජග්ග සූත්‍රය (ධජග්ග පිරිත)",
        "category": "සෙත් පිරිත්",
        "orderIndex": 6,
        "isFavorite": 0
    },
    {
        "id": 10,
        "slug": "angulimala-piritha",
        "titlePali": "Angulimala Parittam",
        "titleSinhala": "අංගුලිමාල පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 8,
        "isFavorite": 1
    },
    {
        "id": 11,
        "slug": "bojjanga-piritha",
        "titlePali": "Bojjhanga Parittam",
        "titleSinhala": "බොජ්ඣංග පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 9,
        "isFavorite": 1
    },
    {
        "id": 12,
        "slug": "jaya-piritha",
        "titlePali": "Jaya Parittam",
        "titleSinhala": "ජය පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 10,
        "isFavorite": 1
    },
    {
        "id": 13,
        "slug": "girimananda-sutta",
        "titlePali": "Girimananda Suttam",
        "titleSinhala": "ගිරිමානන්ද සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 14,
        "isFavorite": 0
    },
    {
        "id": 14,
        "slug": "isigili-sutta",
        "titlePali": "Isigili Suttam",
        "titleSinhala": "ඉසිගිලි සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 16,
        "isFavorite": 0
    },
    {
        "id": 15,
        "slug": "dhammachakkappavattana-sutta",
        "titlePali": "Dhammacakkappavattana Suttam",
        "titleSinhala": "ධම්මචක්කප්පවත්තන සූත්‍රය (දම්සක් පැවතුම් සූත්‍රය)",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 17,
        "isFavorite": 1
    },
    {
        "id": 16,
        "slug": "suriya-piritha",
        "titlePali": "Suriya Parittam",
        "titleSinhala": "සූරිය පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 11,
        "isFavorite": 0
    },
    {
        "id": 17,
        "slug": "chanda-piritha",
        "titlePali": "Chanda Parittam",
        "titleSinhala": "චන්ද පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 12,
        "isFavorite": 0
    },
    {
        "id": 18,
        "slug": "rathnamali-yantra",
        "titlePali": "Rathnamali Gatha",
        "titleSinhala": "රත්නමාලී ගාථා (රත්නමාලී යන්ත්‍රය)",
        "category": "සෙත් පිරිත්",
        "orderIndex": 14,
        "isFavorite": 0
    },
    {
        "id": 19,
        "slug": "dasa-dhamma-sutta",
        "titlePali": "Dasa Dhamma Suttam",
        "titleSinhala": "දසධම්ම සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 20,
        "isFavorite": 0
    },
    {
        "id": 20,
        "slug": "vasala-sutta",
        "titlePali": "Vasala Suttam",
        "titleSinhala": "වසල සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 21,
        "isFavorite": 0
    },
    {
        "id": 21,
        "slug": "parabhava-sutta",
        "titlePali": "Parabhava Suttam",
        "titleSinhala": "පරාභව සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 22,
        "isFavorite": 0
    },
    {
        "id": 22,
        "slug": "alavaka-sutta",
        "titlePali": "Alavaka Suttam",
        "titleSinhala": "ආලවක සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 23,
        "isFavorite": 0
    },
    {
        "id": 23,
        "slug": "buddha-puja",
        "titlePali": "Buddha Puja Gatha",
        "titleSinhala": "බුද්ධ පූජා සහ බෝධි පූජා ගාථා",
        "category": "වන්දනා",
        "orderIndex": 4,
        "isFavorite": 0
    },
    {
        "id": 24,
        "slug": "budhu-guna-piritha",
        "titlePali": "Buddha Guna Parittam",
        "titleSinhala": "බුදු ගුණ පිරිත",
        "category": "සෙත් පිරිත්",
        "orderIndex": 13,
        "isFavorite": 1
    },
    {
        "id": 25,
        "slug": "dhamma-guna",
        "titlePali": "Dhamma Vandana",
        "titleSinhala": "දහම් ගුණ වන්දනාව (ධර්ම රත්නයේ ගුණ)",
        "category": "වන්දනා",
        "orderIndex": 5,
        "isFavorite": 0
    },
    {
        "id": 26,
        "slug": "sanga-guna",
        "titlePali": "Sangha Vandana",
        "titleSinhala": "සඟ ගුණ වන්දනාව (සංඝ රත්නයේ ගුණ)",
        "category": "වන්දනා",
        "orderIndex": 6,
        "isFavorite": 0
    },
    {
        "id": 27,
        "slug": "buddha-meditation",
        "titlePali": "Buddhanussati Bhavana",
        "titleSinhala": "බුද්ධානුස්සති භාවනාව",
        "category": "වන්දනා",
        "orderIndex": 7,
        "isFavorite": 1
    },
    {
        "id": 28,
        "slug": "mettanisansa-sutta",
        "titlePali": "Mettanisansa Suttam",
        "titleSinhala": "මෙත්තානිසංස සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 24,
        "isFavorite": 0
    },
    {
        "id": 29,
        "slug": "miththanisansa-sutta",
        "titlePali": "Mittanisansa Suttam",
        "titleSinhala": "මිත්තානිසංස සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 25,
        "isFavorite": 0
    },
    {
        "id": 30,
        "slug": "chakkavatti-seehanada-sutra",
        "titlePali": "Cakkavatti Sihanada Suttam",
        "titleSinhala": "චක්කවත්ති සීහනාද සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 26,
        "isFavorite": 0
    },
    {
        "id": 31,
        "slug": "nivapa-sutta",
        "titlePali": "Nivapa Suttam",
        "titleSinhala": "නිවාප සූත්‍රය",
        "category": "විශේෂ සූත්‍ර",
        "orderIndex": 27,
        "isFavorite": 0
    }
]


def init_database(cursor: sqlite3.Cursor):
    """
    Creates strict Room asset database schema matching Room expected TableInfo.
    """
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
    print("==================================================")
    print(" PIRITH DATABASE GENERATOR (namo.lk Scraper)")
    print("==================================================")
    
    os.makedirs(DB_DIR, exist_ok=True)
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
        print(f"Cleaned previous database at: {DB_PATH}")
        
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    init_database(cursor)
    
    session = requests.Session()
    
    total_suttas = 0
    total_verses = 0
    
    print("\n--- PHASE 1: Scraping Suttas from namo.lk ---")
    for s_info in NAMO_SUTTAS:
        s_id = s_info["id"]
        slug = s_info["slug"]
        print(f"Fetching Sutta [{s_id}] {s_info['titleSinhala']} ({slug})...")
        
        extracted_verses = scrape_namo_page(slug, session)
        if not extracted_verses:
            print(f"  [WARN] No verses extracted for {slug}")
            continue
            
        cursor.execute("""
            INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (s_id, s_info["titlePali"], s_info["titleSinhala"], s_info["category"], s_info["orderIndex"], s_info["isFavorite"]))
        total_suttas += 1
        
        v_num = 1
        for pali, sinhala in extracted_verses:
            cursor.execute("""
                INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning)
                VALUES (?, ?, ?, ?)
            """, (s_id, v_num, pali, sinhala))
            v_num += 1
            total_verses += 1
            
        print(f"  -> Successfully stored {len(extracted_verses)} verses.")
        
    print("\n--- PHASE 2: Adding Canonical Supplementary Suttas ---")
    for s_info in SUPPLEMENTARY_SUTTAS:
        s_id = s_info["id"]
        print(f"Adding Supplementary Sutta [{s_id}] {s_info['titleSinhala']}...")
        cursor.execute("""
            INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (s_id, s_info["titlePali"], s_info["titleSinhala"], s_info["category"], s_info["orderIndex"], s_info["isFavorite"]))
        total_suttas += 1
        
        v_num = 1
        for pali, sinhala in s_info["verses"]:
            cursor.execute("""
                INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning)
                VALUES (?, ?, ?, ?)
            """, (s_id, v_num, clean_verse_text(pali), clean_verse_text(sinhala)))
            v_num += 1
            total_verses += 1
            
        print(f"  -> Successfully stored {len(s_info['verses'])} verses.")
        
    conn.commit()
    
    # Verification query
    cursor.execute("SELECT count(*) FROM suttas;")
    sutta_count = cursor.fetchone()[0]
    cursor.execute("SELECT count(*) FROM verses;")
    verse_count = cursor.fetchone()[0]
    cursor.execute("SELECT category, count(*) FROM suttas GROUP BY category;")
    categories = cursor.fetchall()
    
    conn.close()
    
    print("\n==================================================")
    print(" VERIFICATION & STATS")
    print("==================================================")
    print(f"Database File: {DB_PATH}")
    print(f"File Size: {os.path.getsize(DB_PATH) / 1024:.2f} KB")
    print(f"Total Suttas Extracted: {sutta_count}")
    print(f"Total Verses Extracted: {verse_count}")
    print("Categories distribution:")
    for cat, cnt in categories:
        print(f"  - {cat}: {cnt} suttas")
    print("==================================================\n")

if __name__ == "__main__":
    main()
