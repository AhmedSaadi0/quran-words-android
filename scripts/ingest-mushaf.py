#!/usr/bin/env python3
"""Ingest QPC (QCF v2) page/line layout into quran_words.db.

Source of truth for layout: api.quran.com v4 `verses/by_page` (no auth needed),
fields: code_v2, location, line_number, page_number, char_type_name, position.

Tables created (supplementary — existing tables are never altered):
  mushaf_word_location(word_ayah_id NULL for ayah-end markers, page, line,
      pos_in_line, code_v2, char_type, surah, ayah, position)
  mushaf_special_lines(page, line, line_type ['surah_header'|'basmalah'], surah_id)
  mushaf_page_meta(page, surah_start/ayah_start/surah_end/ayah_end, juz,
      min_line, max_line)

Mapping invariant: every `word_ayah` row is placed exactly once;
every `word` API row must resolve to a local word_ayah id.

Usage:
  python3 scripts/ingest-mushaf.py --db /tmp/mushaf-work/quran_words.db
  python3 scripts/ingest-mushaf.py --db ... --pages 1,2,531,604   # spike subset
  python3 scripts/ingest-mushaf.py --db ... --start-page 300      # resume
"""

import argparse
import http.client
import json
import sqlite3
import sys
import time
import urllib.error
import urllib.request

API = "https://api.quran.com/api/v4/verses/by_page/{page}?words=true&per_page=100&word_fields=code_v2,location,line_number,page_number,char_type_name,position"
UA = {"User-Agent": "QuranWordsIngest/1.0 (offline-mushaf-layout)"}

DDL = """
DROP TABLE IF EXISTS mushaf_word_location;
DROP TABLE IF EXISTS mushaf_special_lines;
DROP TABLE IF EXISTS mushaf_page_meta;
CREATE TABLE mushaf_word_location (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word_ayah_id INTEGER,
    page_number INTEGER NOT NULL,
    line_number INTEGER NOT NULL,
    pos_in_line INTEGER NOT NULL,
    code_v2 TEXT NOT NULL,
    char_type TEXT NOT NULL,
    surah INTEGER NOT NULL,
    ayah INTEGER NOT NULL,
    position INTEGER NOT NULL,
    UNIQUE(page_number, line_number, pos_in_line)
);
CREATE INDEX idx_mushaf_page_line ON mushaf_word_location(page_number, line_number, pos_in_line);
CREATE INDEX idx_mushaf_wordayah ON mushaf_word_location(word_ayah_id);
CREATE INDEX idx_mushaf_ayah ON mushaf_word_location(surah, ayah);
CREATE TABLE mushaf_special_lines (
    page_number INTEGER NOT NULL,
    line_number INTEGER NOT NULL,
    line_type TEXT NOT NULL,
    surah_id INTEGER NOT NULL,
    PRIMARY KEY (page_number, line_number)
);
CREATE TABLE mushaf_page_meta (
    page_number INTEGER PRIMARY KEY,
    surah_start INTEGER NOT NULL,
    ayah_start INTEGER NOT NULL,
    surah_end INTEGER NOT NULL,
    ayah_end INTEGER NOT NULL,
    juz_number INTEGER,
    min_line INTEGER NOT NULL,
    max_line INTEGER NOT NULL
);
CREATE TABLE mushaf_fetch_log (
    page_number INTEGER PRIMARY KEY
);
"""


def fetch_page(page, retries=8):
    last = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(API.format(page=page), headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.load(r)
        except (
            urllib.error.URLError,
            TimeoutError,
            ConnectionError,
            http.client.HTTPException,
            json.JSONDecodeError,
            OSError,
        ) as e:
            last = e
            time.sleep(2 * (attempt + 1))
    raise RuntimeError("page %d failed after %d retries: %s" % (page, retries, last))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", required=True)
    ap.add_argument("--pages", default="1-604")
    ap.add_argument("--start-page", type=int, default=0)
    ap.add_argument("--delay", type=float, default=0.2)
    args = ap.parse_args()

    wanted = set()
    for part in args.pages.split(","):
        part = part.strip()
        if "-" in part:
            a, b = part.split("-")
            wanted.update(range(int(a), int(b) + 1))
        elif part:
            wanted.add(int(part))
    pages = sorted(p for p in wanted if p >= args.start_page and 1 <= p <= 604)
    if not pages:
        print("no pages to ingest")
        return 1

    con = sqlite3.connect(args.db)
    cur = con.cursor()
    ayah_id = {(s, a): i for i, s, a in cur.execute("SELECT id, surah, ayah FROM ayat")}
    wa_id = {
        (ay, p): i
        for i, ay, p in cur.execute("SELECT id, ayah_id, position FROM word_ayah")
    }
    total_wa = cur.execute("SELECT COUNT(*) FROM word_ayah").fetchone()[0]
    print(
        "local: %d ayat keys, %d word_ayah rows" % (len(ayah_id), total_wa), flush=True
    )

    has_tables = cur.execute(
        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='mushaf_word_location'"
    ).fetchone()[0]
    if not has_tables:
        cur.executescript(DDL)
        print("schema created", flush=True)

    # Resume: fetched pages are skipped (inserts are idempotent). Progress
    # is tracked per fetched response, NOT per stored page, because one
    # response can carry words printed on the neighboring page.
    if args.start_page <= 1:
        done_pages = {
            r[0] for r in cur.execute("SELECT page_number FROM mushaf_fetch_log")
        }
        pending = [p for p in pages if p not in done_pages]
        if pending and len(pending) != len(pages):
            print(
                "resuming: %d/%d pages stored, %d to fetch"
                % (len(pages) - len(pending), len(pages), len(pending)),
                flush=True,
            )
        pages = pending
    if not pages:
        print("all requested pages already stored")
        total_to_fetch = 0
    else:
        total_to_fetch = len(pages)

    WORD_SQL = (
        "INSERT OR REPLACE INTO mushaf_word_location"
        "(word_ayah_id, page_number, line_number, pos_in_line, code_v2, char_type, surah, ayah, position)"
        " VALUES (?,?,?,?,?,?,?,?,?)"
    )
    META_SQL = (
        "INSERT OR REPLACE INTO mushaf_page_meta"
        "(page_number, surah_start, ayah_start, surah_end, ayah_end, juz_number, min_line, max_line)"
        " VALUES (?,?,?,?,?,?,?,?)"
    )
    page_text_lines = {}  # page -> set(lines with API words)
    unresolved = []

    for n, page in enumerate(pages, 1):
        data = fetch_page(page)
        verses = data.get("verses", [])
        if not verses:
            raise RuntimeError("page %d returned zero verses" % page)
        for v in verses:
            s, a = (int(x) for x in v["verse_key"].split(":"))
            if (s, a) not in ayah_id:
                raise RuntimeError(
                    "page %d: ayat %s missing locally" % (page, v["verse_key"])
                )
        line_pos = {}
        rows = []
        for v in verses:
            s, a = (int(x) for x in v["verse_key"].split(":"))
            aid = ayah_id[(s, a)]
            for w in v.get("words", []):
                ctype = w.get("char_type_name")
                line = w.get("line_number")
                code = w.get("code_v2")
                pos = w.get("position")
                if ctype not in ("word", "end") or not line or not code or not pos:
                    raise RuntimeError(
                        "page %d %s:%d unexpected word payload: %s"
                        % (
                            page,
                            s,
                            a,
                            {
                                k: w.get(k)
                                for k in ("char_type_name", "line_number", "position")
                            },
                        )
                    )
                # Route by CLAIMED page: a response carries whole verses, so it
                # can hold words printed on the neighboring page (the verse
                # list itself may even omit such verses, e.g. 5:77 on p120).
                wp = w.get("page_number")
                if not wp or not 1 <= wp <= 604:
                    raise RuntimeError(
                        "page %d %s:%d word has invalid page %s"
                        % (page, s, a, w.get("page_number"))
                    )
                key = (wp, line)
                line_pos[key] = line_pos.get(key, 0) + 1
                if ctype == "word":
                    wid = wa_id.get((aid, pos))
                    if wid is None:
                        unresolved.append("%d:%d:%d" % (s, a, pos))
                        continue
                    rows.append((wid, wp, line, line_pos[key], code, ctype, s, a, pos))
                else:
                    rows.append((None, wp, line, line_pos[key], code, ctype, s, a, pos))
        with con:
            con.executemany(WORD_SQL, rows)
            con.execute(
                "INSERT OR REPLACE INTO mushaf_fetch_log(page_number) VALUES (?)",
                (page,),
            )
        if n % 50 == 0 or n == total_to_fetch:
            print("fetched %d/%d pages..." % (n, total_to_fetch), flush=True)
        time.sleep(args.delay)

    if unresolved:
        print("UNRESOLVED word rows: %d e.g. %s" % (len(unresolved), unresolved[:10]))
        return 2

    # Page meta is derived from stored rows (a page's words can arrive via a
    # neighboring page's response, so per-fetch bounds would be wrong).
    have_pages = sorted(p for p in wanted if 1 <= p <= 604)
    for mp in have_pages:
        kept = sorted(
            {
                (r[0], r[1])
                for r in cur.execute(
                    "SELECT DISTINCT surah, ayah FROM mushaf_word_location WHERE page_number = ?",
                    (mp,),
                )
            }
        )
        if not kept:
            raise RuntimeError("page %d: no stored words" % mp)
        juz = cur.execute(
            "SELECT juz FROM ayat WHERE surah=? AND ayah=?", kept[0]
        ).fetchone()
        bounds = cur.execute(
            "SELECT MIN(line_number), MAX(line_number) FROM mushaf_word_location WHERE page_number = ?",
            (mp,),
        ).fetchone()
        with con:
            con.execute(
                META_SQL,
                (
                    mp,
                    kept[0][0],
                    kept[0][1],
                    kept[-1][0],
                    kept[-1][1],
                    juz[0] if juz else None,
                    bounds[0],
                    bounds[1],
                ),
            )

    # Specials phase reads back everything stored (survives resume).
    all_rows = [
        (wid, pg, ln, p, code, ct, s, a, pos)
        for (wid, pg, ln, p, code, ct, s, a, pos) in cur.execute(
            "SELECT word_ayah_id, page_number, line_number, pos_in_line, code_v2,"
            " char_type, surah, ayah, position FROM mushaf_word_location"
            " WHERE page_number BETWEEN ? AND ?"
            " ORDER BY page_number, line_number, pos_in_line",
            (min(wanted), max(wanted)),
        )
    ]
    for wid, pg, ln, p, code, ct, s, a, pos in all_rows:
        page_text_lines.setdefault(pg, set()).add(ln)

    # ---- special lines (surah headers + basmalah) via gap analysis ----
    # Madinah layout rules encoded here (fail loud on anything else):
    # - internal gap run  -> header(+basmalah) for the surah starting next line
    # - trailing gap run  -> header for the surah starting the NEXT page
    #   (split header: basmalah prints as the next page's leading line)
    # - leading gap run   -> same-page header(+basmalah), or the split basmalah
    def need_for(surah):
        if surah in (1, 9):
            return ["surah_header"]
        return ["surah_header", "basmalah"]

    # Pass A: per-page printed shape (all requested pages, stored or fresh).
    shape = {}
    for page in have_pages:
        if page not in page_text_lines:
            raise RuntimeError("page %d has no stored rows" % page)
        text_lines = sorted(page_text_lines[page])
        lo, hi = text_lines[0], text_lines[-1]
        if lo < 1 or hi > 15:
            raise RuntimeError(
                "page %d: text lines out of 1..15 range (%d..%d)" % (page, lo, hi)
            )
        first_line = {}
        for wid, pg, ln, p, code, ct, s, a, pos in all_rows:
            if pg == page and (s, a) not in first_line:
                first_line[(s, a)] = ln
        kept_sorted = sorted(first_line)
        shape[page] = {
            "min": lo,
            "max": hi,
            "first_line": first_line,
            "first_ayah": kept_sorted[0],
        }

    special_rows = []
    pending_split = {}  # page -> surah whose header is on prev page, basmalah due
    full_run = have_pages[0] == 1 and have_pages[-1] == 604 and len(have_pages) == 604
    for page in have_pages:
        info = shape[page]
        lo, hi = info["min"], info["max"]
        first_line = info["first_line"]
        gaps = [ln for ln in range(1, hi + 1) if ln not in page_text_lines[page]]

        def runs_of(lines):
            runs, run = [], [lines[0]]
            for g in lines[1:]:
                if g == run[-1] + 1:
                    run.append(g)
                else:
                    runs.append(run)
                    run = [g]
            runs.append(run)
            return runs

        # Internal + leading gaps (anything at or below max text line).
        if gaps:
            for run in runs_of(gaps):
                nxt = [
                    k for k, ln in first_line.items() if ln == run[-1] + 1 and k[1] == 1
                ]
                same = sorted(nxt)[0][0] if nxt else None
                split = pending_split.get(page)
                if run[0] == 1 and split is not None:
                    # Split header: prev page holds the header, this line is
                    # the basmalah. The surah starting here must agree.
                    if len(run) != 1 or same != split or split in (1, 9):
                        raise RuntimeError(
                            "page %d: leading gap %s incompatible with split header for surah %s (same=%s)"
                            % (page, run, split, same)
                        )
                    special_rows.append((page, run[0], "basmalah", split))
                    del pending_split[page]
                else:
                    if split is not None:
                        raise RuntimeError(
                            "page %d: duplicate header — split pending for surah %s but gap %s"
                            % (page, split, run)
                        )
                    if same is None:
                        raise RuntimeError(
                            "page %d: gap lines %s not followed by a surah start"
                            % (page, run)
                        )
                    expect = need_for(same)
                    if len(run) != len(expect):
                        raise RuntimeError(
                            "page %d: gap %s size %d != expected %s for surah %d"
                            % (page, run, len(run), expect, same)
                        )
                    for ln, typ in zip(run, expect):
                        special_rows.append((page, ln, typ, same))
        elif full_run and page in pending_split:
            raise RuntimeError(
                "page %d: split basmalah expected for surah %s but no leading gap"
                % (page, pending_split[page])
            )

        # Trailing lines (above max text line): only a next-page header.
        # Needs the neighboring page; skipped when it was not fetched.
        nxt = shape.get(page + 1)
        if nxt is not None and hi < 15 and page != 604:
            trail = list(range(hi + 1, 16))
            s_next, a_next = nxt["first_ayah"]
            if len(trail) > 2:
                # Short page (e.g. p1 ends line 8): bottom stays empty; the
                # next page must carry its own complete header block.
                if a_next == 1 and s_next != 1:
                    need = need_for(s_next)
                    if nxt["min"] - 1 != len(need):
                        raise RuntimeError(
                            "page %d: short page trailing %s but page %d starts surah %d without a full header block"
                            % (page, trail, page + 1, s_next)
                        )
                continue
            if a_next != 1 or s_next == 1:
                raise RuntimeError(
                    "page %d: trailing lines %s but page %d continues mid-text (%d:%d)"
                    % (page, trail, page + 1, s_next, a_next)
                )
            need = need_for(s_next)
            if nxt["min"] == 1:
                if len(trail) != len(need):
                    raise RuntimeError(
                        "page %d: trailing %s size %d != expected %s for surah %d"
                        % (page, trail, len(trail), need, s_next)
                    )
                for ln, typ in zip(trail, need):
                    special_rows.append((page, ln, typ, s_next))
            elif nxt["min"] == 2 and len(trail) == 1 and len(need) == 2:
                special_rows.append((page, trail[0], "surah_header", s_next))
                pending_split[page + 1] = s_next
            else:
                raise RuntimeError(
                    "page %d: trailing %s incompatible with next page start (%d:%d at line %d)"
                    % (page, trail, s_next, a_next, nxt["min"])
                )
    if full_run and pending_split:
        raise RuntimeError("unresolved split headers: %s" % pending_split)
    with con:
        con.executemany(
            "INSERT OR REPLACE INTO mushaf_special_lines(page_number, line_number, line_type, surah_id)"
            " VALUES (?,?,?,?)",
            [r for r in special_rows if r[0] in have_pages],
        )

    # ---- validation ----
    if full_run:
        n_word = cur.execute(
            "SELECT COUNT(*) FROM mushaf_word_location WHERE char_type='word'"
        ).fetchone()[0]
        n_end = cur.execute(
            "SELECT COUNT(*) FROM mushaf_word_location WHERE char_type='end'"
        ).fetchone()[0]
        n_wa = cur.execute("SELECT COUNT(*) FROM word_ayah").fetchone()[0]
        n_ayat = cur.execute("SELECT COUNT(*) FROM ayat").fetchone()[0]
        n_meta = cur.execute("SELECT COUNT(*) FROM mushaf_page_meta").fetchone()[0]
        dup = cur.execute(
            "SELECT COUNT(*) FROM (SELECT word_ayah_id FROM mushaf_word_location WHERE char_type='word' GROUP BY 1 HAVING COUNT(*)>1)"
        ).fetchone()[0]
        missing = cur.execute(
            "SELECT COUNT(*) FROM word_ayah wa WHERE NOT EXISTS (SELECT 1 FROM mushaf_word_location m WHERE m.word_ayah_id = wa.id)"
        ).fetchone()[0]
        print(
            "VALIDATION: word_rows=%d (word_ayah=%d) end_rows=%d (ayat=%d) pages=%d dup=%d missing=%d"
            % (n_word, n_wa, n_end, n_ayat, n_meta, dup, missing)
        )
        ok = (
            n_word == n_wa == total_wa
            and n_end == n_ayat
            and n_meta == 604
            and dup == 0
            and missing == 0
        )
        print("RESULT:", "PASS" if ok else "FAIL")
        con.close()
        return 0 if ok else 3
    else:
        print("subset ingest done (%d pages); skipped full validation" % len(pages))
        con.close()
        return 0


if __name__ == "__main__":
    sys.exit(main())
