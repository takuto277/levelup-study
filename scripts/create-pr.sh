#!/usr/bin/env bash
# 通常 PR を作成（gh + GH_TOKEN / GITHUB_TOKEN 必須）
#
# 例:
#   ./scripts/create-pr.sh --title "feat: 〇〇" --body "Fixes #2"
set -euo pipefail

usage() {
  echo >&2 "Usage: $0 --title <str> [--body <str> | --body-file <path>] [--base main] [--head <branch>]"
  exit 1
}

TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"
if [[ -z "$TOKEN" ]]; then
  echo "GH_TOKEN または GITHUB_TOKEN を export してください。" >&2
  exit 1
fi
export GH_TOKEN="$TOKEN"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh が見つかりません。brew install gh" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

title=""
body=""
body_file=""
base="main"
head="$(git branch --show-current)"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --title) title="$2"; shift 2 ;;
    --body) body="$2"; shift 2 ;;
    --body-file) body_file="$2"; shift 2 ;;
    --base) base="$2"; shift 2 ;;
    --head) head="$2"; shift 2 ;;
    -h | --help) usage ;;
    *) echo "不明: $1" >&2; usage ;;
  esac
done

[[ -n "$title" ]] || usage

cmd=(gh pr create --base "$base" --head "$head" --title "$title")
if [[ -n "$body_file" ]]; then
  cmd+=(--body-file "$body_file")
elif [[ -n "$body" ]]; then
  cmd+=(--body "$body")
else
  cmd+=(--body "_Created via scripts/create-pr.sh_")
fi

"${cmd[@]}"
