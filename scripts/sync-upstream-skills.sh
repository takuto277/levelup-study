#!/usr/bin/env bash
# u7chan/agent-skills から共通スキルを .cursor/skills/ へ同期（LevelUp 専用スキルは除外）
set -euo pipefail

UPSTREAM_REPO="${UPSTREAM_REPO:-https://github.com/u7chan/agent-skills.git}"
UPSTREAM_REF="${UPSTREAM_REF:-main}"
TARGET_DIR=".cursor/skills"
DRY_RUN=0

# LevelUp 専用 — upstream で上書きしない
LEVELUP_ONLY=(
  feature-delivery
  project-context
  github-pr-self-review
  skills-readme-sync
)

usage() {
  echo >&2 "Usage: $0 [--dry-run]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage ;;
    *) usage ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

git clone --depth 1 --branch "$UPSTREAM_REF" "$UPSTREAM_REPO" "$tmp_dir/upstream" >/dev/null

is_levelup_only() {
  local name="$1"
  for item in "${LEVELUP_ONLY[@]}"; do
    [[ "$item" == "$name" ]] && return 0
  done
  return 1
}

synced=0
skipped=0

for skill_path in "$tmp_dir/upstream"/*/SKILL.md; do
  [[ -f "$skill_path" ]] || continue
  skill_name="$(basename "$(dirname "$skill_path")")"

  if is_levelup_only "$skill_name"; then
    skipped=$((skipped + 1))
    continue
  fi

  src_dir="$tmp_dir/upstream/$skill_name"
  dest_dir="$TARGET_DIR/$skill_name"

  if [[ "$DRY_RUN" -eq 1 ]]; then
    if [[ -d "$dest_dir" ]]; then
      diff -qr "$src_dir" "$dest_dir" || true
    else
      echo "NEW: $dest_dir"
    fi
  else
    mkdir -p "$dest_dir"
    rsync -a --delete "$src_dir/" "$dest_dir/"
    echo "synced: $skill_name"
  fi
  synced=$((synced + 1))
done

echo "done: synced=$synced skipped(levelup-only)=${#LEVELUP_ONLY[@]} dry_run=$DRY_RUN"
echo "next: review LEVELUP.md sections in github-* skills, then ./scripts/validate-skills.sh"
