#!/usr/bin/env python3
"""
scripts/add_new_suttas.py
Run after add_atasil.py (and before set_titles.py). Adds the audited items in scripts/data/new_suttas/<id>.json
(ids 35-48, built from pitaka.lk and checked against tripitaka.online and other sources) and slots each one
into the list right after its anchor sutta.
"""
import glob
import json
import os
import sqlite3

BASE = os.path.dirname(__file__)
DB_PATH = os.path.join(BASE, "..", "app", "src", "main", "assets", "databases", "pirith.db")

# new id -> id of the sutta it is placed after
AFTER = {35: 11, 36: 35, 37: 36,
         43: 12, 44: 43, 45: 44, 46: 45, 47: 46, 48: 47,
         38: 31, 39: 38, 40: 39, 41: 40, 42: 41}


def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    data = {}
    for path in glob.glob(os.path.join(BASE, "data", "new_suttas", "*.json")):
        with open(path, encoding="utf-8") as f:
            d = json.load(f)
        data[d["suttaId"]] = d
    for sid in sorted(AFTER, key=lambda s: list(AFTER).index(s)):
        d = data[sid]
        if cur.execute("SELECT 1 FROM suttas WHERE id = ?", (sid,)).fetchone():
            continue
        anchor = cur.execute("SELECT orderIndex FROM suttas WHERE id = ?", (AFTER[sid],)).fetchone()[0]
        cur.execute("UPDATE suttas SET orderIndex = orderIndex + 1 WHERE orderIndex > ?", (anchor,))
        cur.execute(
            "INSERT INTO suttas (id, titlePali, titleSinhala, category, orderIndex, isFavorite) VALUES (?, ?, ?, ?, ?, 0)",
            (sid, d["titlePali"], d["titleSinhala"], d["category"], anchor + 1),
        )
        cur.executemany(
            "INSERT INTO verses (suttaId, verseNumber, paliText, sinhalaMeaning) VALUES (?, ?, ?, ?)",
            [(sid, v["verseNumber"], v["paliText"].strip(), v["sinhalaMeaning"].strip()) for v in d["verses"]],
        )
        print(f"Added {sid} {d['titleSinhala']}: {len(d['verses'])} verses")
    conn.commit()
    conn.execute("VACUUM")


if __name__ == "__main__":
    main()
