#!/usr/bin/env python3
"""
scripts/set_titles.py
Run last. Gives every item a single Sinhala title (no "name (other name)" pairs).
"""
import os
import sqlite3

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "databases", "pirith.db")

TITLES = {
    32: "පන්සිල්",
    7: "ඛන්ධ පිරිත",
    9: "ධජග්ග පිරිත",
    18: "රත්නමාලී ගාථා",
    108: "සීවලී පිරිත",
    15: "ධම්මචක්කප්පවත්තන සූත්‍රය",
}


def main():
    conn = sqlite3.connect(DB_PATH)
    conn.executemany("UPDATE suttas SET titleSinhala = ? WHERE id = ?", [(t, i) for i, t in TITLES.items()])
    conn.commit()
    print(conn.execute("SELECT count(*) FROM suttas WHERE titleSinhala LIKE '%(%'").fetchone()[0], "titles still contain '('")


if __name__ == "__main__":
    main()
