#!/usr/bin/env bash
# Generates quran_words.db.zip + sha256 + manifest snippet for a DB release.
# Usage: ./make-db-release.sh <path/to/quran_words.db> <versionCode> <versionName> [note...]
# Each extra argument becomes one entry in the releaseNotesAr array.
# Example: ./make-db-release.sh quran_words.db 2 v2 'إضافة أجزاء الآية' 'إصلاح المعاني'
set -euo pipefail

DB="${1:?usage: $0 <quran_words.db> <versionCode> <versionName> [note...]}"
CODE="${2:?missing versionCode}"
NAME="${3:?missing versionName}"
shift 3 || true

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cp "$DB" "$WORK/quran_words.db"
rm -f quran_words.db.zip
zip -9 -j quran_words.db.zip "$WORK/quran_words.db"
unzip -l quran_words.db.zip

SHA="$(sha256sum quran_words.db.zip | awk '{print $1}')"
ZSIZE="$(stat -c%s quran_words.db.zip)"
USIZE="$(stat -c%s "$DB")"
DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

json_escape() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  s="${s//$'\t'/\\t}"
  printf '%s' "$s"
}

NOTES_JSON=""
for note in "$@"; do
  [ -n "$NOTES_JSON" ] && NOTES_JSON+=", "
  NOTES_JSON+="\"$(json_escape "$note")\""
done

cat <<EOF
{
  "latestVersionCode": $CODE,
  "latestVersionName": "$NAME",
  "downloadUrl": "https://github.com/AhmedSaadi0/quran-words/releases/download/db-v$CODE/quran_words.db.zip",
  "compressedSize": $ZSIZE,
  "uncompressedSize": $USIZE,
  "sha256": "$SHA",
  "publishedAt": "$DATE",
  "minAppVersionCode": 3,
  "releaseNotesAr": [$NOTES_JSON],
  "releasePageUrl": "https://github.com/AhmedSaadi0/quran-words/releases/tag/db-v$CODE"
}
EOF
