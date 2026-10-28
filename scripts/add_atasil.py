#!/usr/bin/env python3
"""
scripts/add_atasil.py
Run after tidy_pansil.py. Adds the Attha Sila (eight precepts, uposatha sila) item right after
Pancha Sila, in the same one-precept-per-row layout as Pancha Sila / Dasa Sila.
The Pali is the standard samadana formula; meanings re-use the Dasa Sila wording.
"""
import os
import sqlite3

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases", "pirith.db")
SUTTA_ID = 34
AFTER_SUTTA_ID = 32

VERSES = [
    ("පාණාතිපාතා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම සතුන් මැරීමෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("අදින්නාදානා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම නුදුන් දෙය ගැනීමෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("අබ්‍රහ්මචරියා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම අබ්‍රහ්මචරියාවෙන් (මෙවුන්දමෙහි හැසිරීමෙන්) වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("මුසාවාදා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම බොරු කීමෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("සුරාමේරය මජ්ජපමාදට්ඨානා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම මදයට, ප්‍රමාදයට කරුණුවන මත්කුඩු, රහ මෙර, සුරාපානය කිරීමෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("විකාලභෝජනා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම විකල් බොජුනෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("නච්චගීතවාදිතවිසූකදස්සන මාලාගන්ධවිලේපනධාරණමණ්ඩනවිභූසනට්ඨානා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම නැටීම, ගැයීම, වැයීම, විසුලු බැලීම, මල්, ගඳ, විලවුන් දැරීම, සැරසීම, වෙසෙසින් සැරසීම යන කාරණවලින් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("උච්චාසයනමහාසයනා වේරමණී සික්ඛාපදං සමාදියාමි",
     "මම උසසුන් මහ අස්නෙන් වෙන්වීමේ සිල්පදය සමාදන් වෙමි."),
    ("ඉමං අට්ඨඞ්ගසමන්නාගතං බුද්ධපඤ්ඤත්තං උපෝසථසීලං ඉමඤ්ච රත්තිං ඉමඤ්ච දිවසං සම්මදේව අභිරක්ඛිතුං සමාදියාමි",
     "අංග අටකින් යුත්, බුදුරජාණන් වහන්සේ විසින් පනවන ලද මේ උපෝසථ සීලය, මේ රාත්‍රියත් මේ දහවලත් මනාකොට රැකීම පිණිස මම සමාදන් වෙමි."),
]


def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    if cur.execute("SELECT 1 FROM suttas WHERE id = ?", (SUTTA_ID,)).fetchone():
        print("Already added - nothing to do.")
        return
    after = cur.execute("SELECT orderIndex, category FROM suttas WHERE id = ?", (AFTER_SUTTA_ID,)).fetchone()
    cur.execute("UPDATE suttas SET orderIndex = orderIndex + 1 WHERE orderIndex > ?", (after[0],))
    cur.execute(
        "INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite) VALUES (?, ?, ?, ?, ?, 0)",
        (SUTTA_ID, "Attha Sila Samadanaya", "අටසිල්", after[1], after[0] + 1),
    )
    cur.executemany(
        "INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning) VALUES (?, ?, ?, ?)",
        [(SUTTA_ID, n, p, s) for n, (p, s) in enumerate(VERSES, 1)],
    )
    conn.commit()
    conn.execute("VACUUM")
    print(f"Added sutta {SUTTA_ID}: {len(VERSES)} verses")


if __name__ == "__main__":
    main()
