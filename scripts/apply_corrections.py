#!/usr/bin/env python3
"""
scripts/apply_corrections.py
Overlays the audited verse lists in scripts/data/corrections/<suttaId>.json onto
app/src/main/assets/databases/pirith.db (run after extract_pirith_db.py / insert_patthana.py).
Each file holds the COMPLETE corrected verse list for one sutta and replaces its rows.
"""
import glob
import json
import os
import sqlite3

BASE = os.path.dirname(__file__)
DB_PATH = os.path.join(BASE, "..", "app", "src", "main", "assets", "databases", "pirith.db")


def clean_text(text: str) -> str:
    if not text:
        return ""
    return text.replace(r"\xA0", " ").replace(r"\xa0", " ").replace("\u00a0", " ").strip()


def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    for path in sorted(glob.glob(os.path.join(BASE, "data", "corrections", "*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        sutta_id = data["suttaId"]
        if not cur.execute("SELECT 1 FROM suttas WHERE id = ?", (sutta_id,)).fetchone():
            print(f"  [SKIP] sutta {sutta_id} not in DB")
            continue
        cur.execute("DELETE FROM verses WHERE suttaId = ?", (sutta_id,))
        cur.executemany(
            "INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning) VALUES (?, ?, ?, ?)",
            [(sutta_id, v["verseNumber"], clean_text(v["paliText"]), clean_text(v["sinhalaMeaning"])) for v in data["verses"]],
        )
        print(f"Sutta {sutta_id}: {len(data['verses'])} verses")
    conn.commit()
    conn.execute("VACUUM")
    conn.close()


if __name__ == "__main__":
    main()
