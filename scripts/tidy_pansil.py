#!/usr/bin/env python3
"""
scripts/tidy_pansil.py
Run after apply_corrections.py. Pancha Sila (id 32) keeps only the five precepts (the namaskara and
tisarana already live in id 1). Each precept is its own row, like the Dasa Sila item, followed by the
closing "tisaranena saddhim pancasilam samadiyami" line.
"""
import os
import sqlite3

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases", "pirith.db")
SUTTA_ID = 32


def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    row = cur.execute(
        "SELECT paliText, sinhalaMeaning FROM verses WHERE suttaId = ? AND paliText LIKE '[3.%'", (SUTTA_ID,)
    ).fetchone()
    if not row:
        print("Already tidied - nothing to do.")
        return
    pali = [l.strip() for l in row[0].split("\n")[1:] if l.strip()]
    sinhala = [l.strip() for l in row[1].split("\n") if l.strip()]
    assert len(pali) == len(sinhala) == 6, (len(pali), len(sinhala))

    cur.execute("DELETE FROM verses WHERE suttaId = ?", (SUTTA_ID,))
    cur.executemany(
        "INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning) VALUES (?, ?, ?, ?)",
        [(SUTTA_ID, n, p, s) for n, (p, s) in enumerate(zip(pali, sinhala), 1)],
    )
    cur.execute("UPDATE suttas SET titleSinhala = ? WHERE id = ?", ("පංචශීලය (පන්සිල්)", SUTTA_ID))
    conn.commit()
    conn.execute("VACUUM")
    print(f"Sutta {SUTTA_ID}: {len(pali)} verses")


if __name__ == "__main__":
    main()
