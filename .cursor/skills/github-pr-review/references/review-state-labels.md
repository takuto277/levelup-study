# Review State Labels

## ラベルの意味

PR レビュー状態を GitHub のラベル一覧から見えるようにするため、次の 3 ラベルを排他的に扱う。

| Label | 意味 |
|-------|------|
| `PRレビュー/再レビュー待ち` | 初回レビュー、またはレビュー指摘対応後の再レビュー待ち |
| `PRレビュー修正待ち` | PRレビュー済みで、修正・回答・追加対応が必要 |
| `review:まーじOK` | AIレビューとして未解決の指摘がない |

これらは GitHub の approval ではないため、`APPROVE` review event や `gh pr review --approve` は使わない。

## 状態遷移

- PR 作成直後: `PRレビュー/再レビュー待ち`
- 通常レビューで指摘コメントを 1 件以上投稿: `PRレビュー修正待ち`
- 通常レビューで指摘なし: `review:まーじOK`
- FB対応で対象指摘を修正し、commit / push / 返信まで完了: `PRレビュー/再レビュー待ち`
- 再チェックで未解決指摘がすべて解消: `review:まーじOK`
- 再チェックで `partial` / `unresolved` / `unknown` が残る: `PRレビュー修正待ち`

3 ラベルは排他的に扱う。どれか 1 つを付ける時は、他 2 つを外す。該当しない状態では、該当しないラベルを外す。

## 操作手順

1. ラベル名を固定する。

        REVIEW_WAIT_LABEL='PRレビュー/再レビュー待ち'
        REVIEW_FIX_WAIT_LABEL='PRレビュー修正待ち'
        REVIEW_OK_LABEL='review:まーじOK'

2. 付与前に、ラベルが存在しなければ作成する。

        gh label list -R "$OWNER/$REPO" --json name --jq '.[].name' | grep -Fx "$REVIEW_WAIT_LABEL" >/dev/null ||
          gh label create "$REVIEW_WAIT_LABEL" -R "$OWNER/$REPO" --description 'PRレビューまたは再レビュー待ち' --color '5319E7'

        gh label list -R "$OWNER/$REPO" --json name --jq '.[].name' | grep -Fx "$REVIEW_FIX_WAIT_LABEL" >/dev/null ||
          gh label create "$REVIEW_FIX_WAIT_LABEL" -R "$OWNER/$REPO" --description 'PRレビュー指摘への修正待ち' --color 'D93F0B'

        gh label list -R "$OWNER/$REPO" --json name --jq '.[].name' | grep -Fx "$REVIEW_OK_LABEL" >/dev/null ||
          gh label create "$REVIEW_OK_LABEL" -R "$OWNER/$REPO" --description 'AIレビューで未解決の指摘がない状態' --color '0E8A16'

3. `PRレビュー/再レビュー待ち` にする。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" \
          --add-label "$REVIEW_WAIT_LABEL" \
          --remove-label "$REVIEW_FIX_WAIT_LABEL" \
          --remove-label "$REVIEW_OK_LABEL"

4. `PRレビュー修正待ち` にする。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" \
          --add-label "$REVIEW_FIX_WAIT_LABEL" \
          --remove-label "$REVIEW_WAIT_LABEL" \
          --remove-label "$REVIEW_OK_LABEL"

5. `review:まーじOK` にする。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" \
          --add-label "$REVIEW_OK_LABEL" \
          --remove-label "$REVIEW_WAIT_LABEL" \
          --remove-label "$REVIEW_FIX_WAIT_LABEL"

6. どの状態にも該当しない時は 3 ラベルすべてを外す。

        gh issue edit "$PR_NUMBER" -R "$OWNER/$REPO" \
          --remove-label "$REVIEW_WAIT_LABEL" \
          --remove-label "$REVIEW_FIX_WAIT_LABEL" \
          --remove-label "$REVIEW_OK_LABEL"

## 失敗時

- `gh label create` が `already_exists` で失敗した場合は、直後に `gh issue edit` を再実行する。
- `gh issue edit --remove-label` が「label does not exist」「not found」で失敗した場合は、既に外れているものとして扱う。
- ラベル作成・付与・除去に失敗しても、レビューコメント投稿や feedback 対応は巻き戻さない。最終報告でラベル未更新と理由を伝える。
- セルフレビューでは PRレビュー状態ラベルを更新しない。
