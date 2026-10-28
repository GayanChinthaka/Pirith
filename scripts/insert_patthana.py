import sqlite3
import os
import json
import re

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases", "pirith.db")

PATTHANA_ARAMBHAKA_VERSES = [
    (
        1,
        "නමෝ තස්ස භගවතෝ අරහතෝ සම්මා සම්බුද්ධස්ස පට්ඨාන දේසකස්ස\nනමෝ තස්ස භගවතෝ අරහතෝ සම්මා සම්බුද්ධස්ස පට්ඨාන දේසකස්ස\nනමෝ තස්ස භගවතෝ අරහතෝ සම්මා සම්බුද්ධස්ස පට්ඨාන දේසකස්ස",
        "පට්ඨාන මහා ධර්මය දේශනා කොට වදාළා වූ ඒ භාග්‍යවත් වූ අර්හත් වූ සම්මා සම්බුදුරජාණන් වහන්සේට මාගේ නමස්කාරය වේවා! (තෙවරක්)"
    ),
    (
        2,
        "විචිත්ත මති ගම්භීරං - මනන්ත නය මණ්ඩිතං,\nපට්ඨානං සම්මසන්තස්ස - විමලාමිත බුද්ධියා.",
        "විචිත්‍ර වූ ද, අතිශයින් ගැඹුරු වූ ද, අනන්ත වූ හේතු-ඵල නය ක්‍රමයන්ගෙන් සමන්විත වූ ද මේ මහා පට්ඨාන ධර්මය තමාගේ නිර්මල වූ අප්‍රමාණ බුද්ධි මහිමයෙන් මෙනෙහි කළා වූ බුදුරජාණන් වහන්සේට නමස්කාර කරමි."
    ),
    (
        3,
        "යස්ස දේහා නික්ඛමිංසු - සුභා ඡබ්බණ්ණ රංසියෝ,\nනීල පීතා රත්ත සේතා - මඤ්ජෙට්ඨා ච පභස්සරා.",
        "මේ උතුම් පට්ඨාන ධර්මය මෙනෙහි කරන කල්හි යම් බුදුරජාණන් වහන්සේ නමකගේ ශ්‍රී ශරීරයෙන් නිල්, කහ, රතු, සුදු, මදටිය සහ ප්‍රභාස්වර යන සවණක් ගණ බුදු රැස් මාලාවන් දසත විහිදී ගියාහු ද,"
    ),
    (
        4,
        "තං ලෝකනාථං සුගතං - ධම්මඤ්ච ජින සේවිතං,\nසංඝං නිරංගණං සෙට්ඨං - නමාමි සිරසාදරං.",
        "ඒ ලෝකනාථ වූ සුගත බුදුරජාණන් වහන්සේට ද, උන්වහන්සේ විසින් පසක් කොට වදාළ උතුම් ශ්‍රී සද්ධර්මයට ද, කෙලෙස් මලින් තොර වූ ශ්‍රේෂ්ඨ මහා සංඝරත්නයට ද මම ගෞරවයෙන් හිස නමා නමස්කාර කරමි."
    )
]

# Full text of the Paccaya Uddesa and Paccaya Niddesa (Pali + Sinhala meaning) comes from
# pitaka.lk (62-samantha-pattana, Nauyane Ariyadhamma Thero), saved in data/patthana_pitaka.json.
# Every Pali sentence is kept as its own verse so that the Pali and its meaning stay side by side.
def _drop_stray_zwj(text):
    # The source puts a ZWJ after many hal-kirima (e.g. ගන්‍ධ, අත්‍ථි). Keep it only where it
    # forms a rakaransaya / yansaya (්‍ර, ්‍ය) and drop it elsewhere.
    return re.sub("\u200d(?![රය])", "", text)


def _clean(node):
    if isinstance(node, str):
        return _drop_stray_zwj(node)
    if isinstance(node, list):
        return [_clean(x) for x in node]
    if isinstance(node, dict):
        return {k: _clean(v) for k, v in node.items()}
    return node


with open(os.path.join(os.path.dirname(__file__), "data", "patthana_pitaka.json"), encoding="utf-8") as _f:
    _PITAKA = _clean(json.load(_f))

# Traditional closing vandana gatha after each paccaya: (Pali stem, Sinhala meaning).
PACCAYA_VANDANA = [
    ("හේතුපච්චය", "හේතු ප්‍රත්‍යය ඤාණය කෙරණකොට ගෙන ෂඩ්වර්ණ බුද්ධ රශ්මියෙන් බබලන්නා වූ, සියල්ල දක්නා වූ ඒ සම්මා සම්බුදුරජාණන් වහන්සේට නමස්කාර පූජා කෙරමි."),
    ("ආරම්මණපච්චය", "ආරම්මණ ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ, ෂඩ්වර්ණ රශ්මි විහිදුවාලූ ඒ සර්වඥ බුදුරජාණන් වහන්සේට මාගේ නමස්කාරය වේවා!"),
    ("අධිපතිපච්චය", "අධිපති ප්‍රත්‍යය ඤාණයෙන් යුක්ත වූ, ෂඩ්වර්ණ රශ්මියෙන් බබලන්නා වූ සම්මා සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("අනන්තරපච්චය", "අනන්තර ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සර්වඥ බුදුරජාණන් වහන්සේට මාගේ නමස්කාරය වේවා!"),
    ("සමනන්තරපච්චය", "සමනන්තර ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("සහජාතපච්චය", "සහජාත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("අඤ්ඤමඤ්ඤපච්චය", "අඤ්ඤමඤ්ඤ ප්‍රත්‍යය ඥානයෙන් හටගත් ෂඩ්වර්ණ රශ්මියෙන් බබලන්නා වූ සම්බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("නිස්සයපච්චය", "නිස්සය ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("උපනිස්සයපච්චය", "උපනිස්සය ප්‍රත්‍යය ඤාණයෙන් යුත් සර්වඥ බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("පුරේජාතපච්චය", "පුරේජාත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("පච්ඡාජාතපච්චය", "පච්ඡාජාත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("ආසේවනපච්චය", "ආසේවන ප්‍රත්‍යය ඤාණයෙන් හටගත් ෂඩ්වර්ණ රශ්මියෙන් බබලන්නා වූ බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("කම්මපච්චය", "කර්ම ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සියල්ල දක්නා බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("විපාකපච්චය", "විපාක ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්මා සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("ආහාරපච්චය", "ආහාර ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්බුදු සමිඳුන් වන්දනා කෙරමි."),
    ("ඉන්ද්‍රියපච්චය", "ඉන්ද්‍රිය ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සර්වඥ බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("ඣානපච්චය", "ඣාන ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ ලෝකනාථ බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("මග්ගපච්චය", "මග්ග ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්මා සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("සම්පයුත්තපච්චය", "සම්පයුත්ත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සර්වඥ බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("විප්පයුත්තපච්චය", "විප්පයුත්ත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්බුදුරජාණන් වහන්සේට නමස්කාර කෙරමි."),
    ("අත්ථිපච්චය", "අත්ථි ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ ඒ සම්මා සම්බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("නත්ථිපච්චය", "නත්ථි ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සම්මා සම්බුදු සමිඳුන් වන්දනා කෙරමි."),
    ("විගතපච්චය", "විගත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ ශාස්තෘ වූ බුදුරජාණන් වහන්සේ වන්දනා කෙරමි."),
    ("අවිගතපච්චය", "අවිගත ප්‍රත්‍යය ඤාණයෙන් බබලන්නා වූ සියල්ල දක්නා ඒ සම්මා සම්බුදුරජාණන් වහන්සේට නමස්කාර පූජා කෙරමි."),
]


def _build_uddesa_verse(number):
    pali = "පට්ඨාන පච්චයුද්දේසෝ\n\n" + "\n".join(_PITAKA["uddesa_pali"])
    sinhala = "පට්ඨාන මාතෘකා පද\n\n" + "\n".join(
        f"{i}. {name}" for i, name in enumerate(_PITAKA["uddesa_sinhala"], 1)
    )
    return (number, pali, sinhala)


def _build_niddesa_verses(first_number):
    verses = []
    n = first_number
    for paccaya, (stem, vandana_sinhala) in zip(_PITAKA["paccaya"], PACCAYA_VANDANA):
        for i, (pali, sinhala) in enumerate(paccaya["blocks"]):
            if i == 0:
                pali = f"[{paccaya['heading']}]\n{pali}"
            verses.append((n, pali, sinhala))
            n += 1
        verses.append((
            n,
            f"{stem} ඤාණේන - ඡබ්බණ්ණ රංසි භාසුරං\nසම්බුද්ධං සබ්බ දස්සාවිං - වන්දාමි පූජයාමහං",
            f"වන්දනාව: {vandana_sinhala}",
        ))
        n += 1
    pali, sinhala = _PITAKA["closing"]
    verses.append((n, pali, sinhala))
    return verses


PATTHANA_UDDESA_VERSES = [_build_uddesa_verse(5)]

PATTHANA_NIDDESA_AND_GNANA_VERSES = _build_niddesa_verses(6)

PATTHANA_CONCLUSION_VERSES = [
    (
        len(PATTHANA_ARAMBHAKA_VERSES) + len(PATTHANA_UDDESA_VERSES) + len(PATTHANA_NIDDESA_AND_GNANA_VERSES) + 1,
        "සාධු! සාධු! සාධු!\n\n"
        "ඉමිනා පුඤ්ඤකම්මේන - මා මේ බාලසමාගමෝ,\n"
        "සතං සමාගමෝ හෝතු - යාව නිබ්බානපත්තියා.",
        "සාධු! සාධු! සාධු!\n\n"
        "මේ උතුම් පින් බලයෙන් මට කිසි කලෙකත් අඥානයන්ගේ ඇසුරක් නොවේවා! නිවන් දකින තුරුම උතුම් සත්පුරුෂයන්ගේ ඇසුරම ලැබේවා!"
    )
]

# Combined all-in-one verses
ALL_PATTHANA_VERSES = (
    PATTHANA_ARAMBHAKA_VERSES +
    PATTHANA_UDDESA_VERSES +
    PATTHANA_NIDDESA_AND_GNANA_VERSES +
    PATTHANA_CONCLUSION_VERSES
)

def main():
    print(f"Connecting to database: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Remove existing Patthana entries if any
    cursor.execute("DELETE FROM verses WHERE suttaId IN (201, 202, 203)")
    cursor.execute("DELETE FROM suttas WHERE id IN (201, 202, 203)")

    suttas = [
        (201, "Mahā Paṭṭhāna Paccaya Sajjhāyanā", "මහා පට්ඨාන ප්‍රකරණය", "පට්ඨාන", 42, 0, ALL_PATTHANA_VERSES)
    ]

    for s_id, title_pali, title_sin, category, order_idx, is_fav, verses in suttas:
        cursor.execute("""
            INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (s_id, title_pali, title_sin, category, order_idx, is_fav))
        print(f"Inserted Sutta [{s_id}] {title_sin}")

        fallback_num = 1
        for item in verses:
            if len(item) == 3:
                v_num, pali, sinhala = item
            else:
                pali, sinhala = item
                v_num = fallback_num
                fallback_num += 1
            cursor.execute("""
                INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning)
                VALUES (?, ?, ?, ?)
            """, (s_id, v_num, pali.strip(), sinhala.strip()))
        print(f"  -> Inserted {len(verses)} verses.")

    conn.commit()

    # Verify
    cursor.execute("SELECT count(*) FROM suttas WHERE category='පට්ඨාන'")
    print("Total Patthana Suttas:", cursor.fetchone()[0])
    cursor.execute("SELECT count(*) FROM verses WHERE suttaId = 201")
    print("Total Patthana Verses:", cursor.fetchone()[0])
    cursor.execute("SELECT DISTINCT verseNumber FROM verses WHERE suttaId = 201 ORDER BY verseNumber ASC")
    print("Distinct verse numbers for Patthana in DB:", [r[0] for r in cursor.fetchall()])
    cursor.execute("SELECT count(*) FROM verses WHERE suttaId = 201 AND verseNumber = 5")
    print("Count of verses with verseNumber = 5 (Uddesa):", cursor.fetchone()[0])

    conn.close()
    print("Done!")

if __name__ == "__main__":
    main()
