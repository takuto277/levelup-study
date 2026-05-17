#!/usr/bin/env bash
# GitHub にイシューを作成する（gh + GH_TOKEN / GITHUB_TOKEN 必須）
#
# 前提:
#   - gh CLI: https://cli.github.com/ （macOS: brew install gh）
#   - トークン: 環境変数 GH_TOKEN または GITHUB_TOKEN（いずれも非対話用）
#     Classic PAT: repo スコープ / Fine-grained: 当該リポジトリで Issues: Read and write
#
# 例:
#   export GH_TOKEN=ghp_xxxx
#   ./scripts/create-issue.sh --title "バグ: 〜" --body 'Markdown に `code` があるときはシングルクォートで囲む（zsh は "..." 内の ` をコマンド置換する）'
#   ./scripts/create-issue.sh --title "タスク" --body-file ./issue-body.md --label bug --label mobile
set -euo pipefail

usage() {
  echo >&2 "Usage: $0 --title <str> [--body <str> | --body-file <path>] [--label <name>] ..."
  exit 1
}

TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-}}"
if [[ -z "$TOKEN" ]]; then
  echo "GH_TOKEN または GITHUB_TOKEN を export してください。" >&2
  exit 1
fi
export GH_TOKEN="$TOKEN"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh が見つかりません。インストール: https://cli.github.com/ （macOS: brew install gh）" >&2
  exit 1
fi

title=""
body=""
body_file=""
labels=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --title)
      [[ $# -ge 2 ]] || usage
      title="$2"
      shift 2
      ;;
    --body)
      [[ $# -ge 2 ]] || usage
      body="$2"
      shift 2
      ;;
    --body-file)
      [[ $# -ge 2 ]] || usage
      body_file="$2"
      shift 2
      ;;
    --label)
      [[ $# -ge 2 ]] || usage
      labels+=("$2")
      shift 2
      ;;
    -h | --help)
      usage
      ;;
    *)
      echo "不明な引数: $1" >&2
      usage
      ;;
  esac
done

if [[ -z "$title" ]]; then
  echo "--title は必須です。" >&2
  usage
fi

if [[ -n "$body_file" && -n "$body" ]]; then
  echo "--body と --body-file は同時に指定できません。" >&2
  exit 1
fi

cmd=(gh issue create --title "$title")

if [[ -n "$body_file" ]]; then
  if [[ ! -f "$body_file" ]]; then
    echo "ファイルが見つかりません: $body_file" >&2
    exit 1
  fi
  cmd+=(--body-file "$body_file")
elif [[ -n "$body" ]]; then
  cmd+=(--body "$body")
else
  cmd+=(--body "_Created via scripts/create-issue.sh_")
fi

for lb in "${labels[@]}"; do
  cmd+=(--label "$lb")
done

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
if git -C "$repo_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  # カレントが別ディレクトリでも、リポジトリルートで gh が remote を解決できるようにする
  (cd "$repo_root" && "${cmd[@]}")
else
  "${cmd[@]}"
fi
