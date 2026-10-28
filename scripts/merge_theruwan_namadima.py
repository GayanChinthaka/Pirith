#!/usr/bin/env python3
"""
scripts/merge_theruwan_namadima.py
Run after apply_corrections.py. Replaces the short "Ratanattaya Vandana" (id 2) with one
"තෙරුවන් නැමදීම" item built from the detailed word-by-word vandanas:
  27 Buddhanussati Bhavana + 25 Dhamma Vandana + 26 Sangha Vandana  ->  id 2
and removes ids 25, 26, 27 (later orderIndex values are shifted down to stay contiguous).
"""
import os
import sqlite3

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases", "pirith.db")
MERGED_ID = 2
PARTS = [(27, "බුද්ධානුස්සතිය"), (25, "ධම්මානුස්සතිය"), (26, "සංඝානුස්සතිය")]


def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    if not all(cur.execute("SELECT 1 FROM suttas WHERE id = ?", (i,)).fetchone() for i, _ in PARTS):
        print("Already merged (or parts missing) - nothing to do.")
        return

    merged = []
    for sutta_id, heading in PARTS:
        rows = cur.execute(
            "SELECT paliText, sinhalaMeaning FROM verses WHERE suttaId = ? ORDER BY verseNumber", (sutta_id,)
        ).fetchall()
        for i, (pali, sinhala) in enumerate(rows):
            merged.append((f"[{heading}]\n{pali}" if i == 0 else pali, sinhala))

    removed_orders = [r[0] for r in cur.execute(
        "SELECT orderIndex FROM suttas WHERE id IN (25, 26, 27)")]
    cur.execute("DELETE FROM verses WHERE suttaId IN (2, 25, 26, 27)")
    cur.execute("DELETE FROM suttas WHERE id IN (25, 26, 27)")
    cur.executemany(
        "INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning) VALUES (?, ?, ?, ?)",
        [(MERGED_ID, n, p.strip(), s.strip()) for n, (p, s) in enumerate(merged, 1)],
    )
    cur.execute("UPDATE suttas SET titlePali = ?, titleSinhala = ? WHERE id = ?",
                ("Theruwan Namadima", "තෙරුවන් නැමදීම", MERGED_ID))
    cur.execute("UPDATE suttas SET orderIndex = orderIndex - ? WHERE orderIndex > ?",
                (len(removed_orders), max(removed_orders)))
    conn.commit()
    conn.execute("VACUUM")
    print(f"Merged into sutta {MERGED_ID}: {len(merged)} verses")


if __name__ == "__main__":
    main()
