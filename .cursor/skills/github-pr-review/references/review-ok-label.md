# Review OK Label

## ラベルの意味

`review:まーじOK` は、AIレビューとして未解決の指摘がないことを PR 一覧で見えるようにするラベル。
GitHub の approval ではないため、`APPROVE` review event や `gh pr review --approve` は引き続き使わない。

## 付与条件

- 通常レビューで指摘候補が 0 件で、指摘なしの `COMMENT` review を投稿・確認できた時
- 再チェックで、このスキルが投稿した未解決 review thread がすべて `resolved` または既に `isResolved: true` になり、新しい指摘コメントが 0 件の時
- ラベル操作権限がある時。権限不足の場合はレビュー結果を優先し、ラベル未更新として報告する

## 除去条件

- 通常レビューで `[must]` / `[should]` / `[nit]` / `[ask]` / `[imo]` の指摘コメントを 1 件以上投稿した時
- 再チェック結果に `partial` / `unresolved` / `unknown` が 1 件以上ある時
- 差分取得失敗や権限不足により、OK と判断できない時

## 操作手順

1. ラベル名を固定する。

        REVIEW_OK_LABEL='review:まーじOK'

2. 付与前に、ラベルが存在しなければ作成する。

        gh label list -R "$OWNER/$REPO" --search "$REVIEW_OK_LABEL" --json name --jq '.[].name' |
          grep -Fx "$REVIEW_OK_LABEL" >/dev/null ||
          gh label create "$REVIEW_OK_LABEL" -R "$OWNER/$REPO" \
            --description 'AIレビューで未解決の指摘がない状態' \
            --color '0E8A16'

3. OK 条件を満たした PR へラベルを付与する。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" --add-label "$REVIEW_OK_LABEL"

4. 指摘投稿または再チェック未完了の場合は、古い OK ラベルを外す。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" --remove-label "$REVIEW_OK_LABEL"

## 失敗時

- `gh label create` が「already_exists」で失敗した場合は、直後に `gh issue edit --add-label` を再実行する
- `gh issue edit --remove-label` が「label does not exist」「not found」で失敗した場合は、既に外れているものとして扱う
- ラベル作成・付与・除去に失敗しても、レビューコメント投稿を巻き戻さない。最終報告でラベル未更新と理由を伝える
- セルフレビューではこのラベルを付けない。詳しくは `../../github-pr-self-review/SKILL.md` の固有ルールに従う
