#!/usr/bin/env bash
# GitHub Issue 起票 → feat ブランチ作成 → 実行計画書雛形生成
#
# 前提: GH_TOKEN または GITHUB_TOKEN（Issues: Read and write）
#
# 例:
#   ./scripts/feature-start.sh \
#     --title "feat: アセット入稿パイプライン" \
#     --body-file docs/tasks/issue-body-asset-ingestion-pipeline.md \
#     --branch-slug asset-ingestion-pipeline
set -euo pipefail

usage() {
  echo >&2 "Usage: $0 --title <str> --branch-slug <slug> [--body <str> | --body-file <path>] [--label <name>] ..."
  exit 1
}

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

title=""
branch_slug=""
body_file=""
labels=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --title)
      [[ $# -ge 2 ]] || usage
      title="$2"
      shift 2
      ;;
    --branch-slug)
      [[ $# -ge 2 ]] || usage
      branch_slug="$2"
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

[[ -n "$title" && -n "$branch_slug" ]] || usage

# Issue 作成
issue_args=(--title "$title")
if [[ -n "$body_file" ]]; then
  issue_args+=(--body-file "$body_file")
fi
for lb in "${labels[@]}"; do
  issue_args+=(--label "$lb")
done

issue_url=""
issue_num=""
if issue_url="$(./scripts/create-issue.sh "${issue_args[@]}" 2>/dev/null | tail -1)"; then
  issue_num="$(echo "$issue_url" | grep -Eo '[0-9]+$' || true)"
else
  echo "警告: Issue を自動作成できませんでした（GH_TOKEN 未設定など）。" >&2
  echo "  手動: ./scripts/create-issue.sh --title \"$title\" --body-file ${body_file:-/dev/null}" >&2
  echo "  または Actions → Create issue (manual) を実行" >&2
fi

branch_name="feat/${issue_num:+$issue_num-}${branch_slug}"
branch_name="${branch_name/feaut--/feat-}"  # issue 番号なし時の feat-- 防止

if git show-ref --verify --quiet "refs/heads/$branch_name"; then
  git checkout "$branch_name"
else
  git checkout -b "$branch_name" main 2>/dev/null || git checkout -b "$branch_name"
fi

# 実行計画書雛形
today="$(date +%Y%m%d)"
task_file="docs/tasks/${today}-${branch_slug}.md"
if [[ ! -f "$task_file" ]]; then
  cat > "$task_file" <<EOF
# タスク: ${title}

| 項目 | 値 |
|------|-----|
| 作成日 | $(date +%Y-%m-%d) |
| ステータス | 進行中 |
| Issue | ${issue_url:-（未作成）} |

## 概要
${title}

## 要件
- [ ] 要件を記載

## 影響範囲
-

## 実装手順
1.

## テスト計画
- [ ] \`./scripts/assets/run.sh scripts/assets/validate_assets.py\`（アセット変更時）

## 結果・振り返り
（完了後に記入）
EOF
  echo "Created $task_file"
fi

echo ""
echo "Branch: $branch_name"
[[ -n "$issue_url" ]] && echo "Issue:  $issue_url"
echo "Next: 実装 → validate → git push -u origin HEAD（Auto open PR が Issue + 説明文付き PR を作成）"
