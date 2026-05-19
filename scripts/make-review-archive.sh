#!/usr/bin/env bash
set -euo pipefail

# OperatorRadar review archive generator.
# Usage:
#   bash scripts/make-review-archive.sh
#   bash scripts/make-review-archive.sh "perf: stream recording coverage JSON response"
#
# The optional argument is an expected substring of HEAD's commit subject.
# If it does not match, the script exits before producing an archive, which
# prevents uploading a stale review tarball.

EXPECTED_SUBJECT="${1:-}"

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

HEAD_SHA="$(git rev-parse --short=12 HEAD)"
HEAD_SUBJECT="$(git show -s --format=%s HEAD)"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [[ -n "$EXPECTED_SUBJECT" && "$HEAD_SUBJECT" != *"$EXPECTED_SUBJECT"* ]]; then
  echo "ERROR: HEAD subject does not match expected phase/commit." >&2
  echo "  expected substring: $EXPECTED_SUBJECT" >&2
  echo "  actual HEAD:        $HEAD_SHA $HEAD_SUBJECT" >&2
  exit 2
fi

OUTDIR="$ROOT/build/questadmin-review-$HEAD_SHA-$STAMP"
ARCHIVE="$ROOT/questadmin-review-$HEAD_SHA-$STAMP.tar.gz"
LATEST="$ROOT/questadmin-review-latest.tar.gz"

rm -rf "$OUTDIR"
mkdir -p "$OUTDIR"/{meta,diff,file-diffs,files,checks}

{
  echo "generatedAt=$STAMP"
  echo "repo=$ROOT"
  echo "branch=$BRANCH"
  echo "head=$HEAD_SHA"
  echo "subject=$HEAD_SUBJECT"
  echo "expectedSubject=$EXPECTED_SUBJECT"
} > "$OUTDIR/meta/review-info.txt"

git log --oneline -10 > "$OUTDIR/meta/git-log-oneline.txt"
git status --short > "$OUTDIR/meta/git-status-short.txt"
git show --stat --oneline --no-color HEAD > "$OUTDIR/meta/head-stat.txt"
git show --name-status --no-color HEAD > "$OUTDIR/meta/head-name-status.txt"
git diff-tree --no-commit-id --name-only -r HEAD > "$OUTDIR/meta/changed-files.txt"
git show --no-color HEAD > "$OUTDIR/diff/head-full-diff.txt"

# Per-file diffs and HEAD file contents for every changed file.
while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  safe="${file//\//__}"
  safe="${safe// /_}"

  git show --no-color HEAD -- "$file" > "$OUTDIR/file-diffs/${safe}.diff.txt" || true

  # Store the committed file content when it exists at HEAD.
  # Deleted files will fail here and are skipped.
  if git cat-file -e "HEAD:$file" 2>/dev/null; then
    mkdir -p "$OUTDIR/files/$(dirname "$file")"
    git show "HEAD:$file" > "$OUTDIR/files/$file" || true
  fi
done < "$OUTDIR/meta/changed-files.txt"


# These checks do not mutate the repo. Full builds are still run separately by Codex.
git diff --check > "$OUTDIR/checks/git-diff-check.txt" 2>&1 || true

tar -czf "$ARCHIVE" -C "$(dirname "$OUTDIR")" "$(basename "$OUTDIR")"
cp "$ARCHIVE" "$LATEST"

echo "Created:"
echo "  $ARCHIVE"
echo "  $LATEST"
echo
echo "HEAD:"
echo "  $HEAD_SHA $HEAD_SUBJECT"
echo
echo "Changed files:"
cat "$OUTDIR/meta/changed-files.txt"
