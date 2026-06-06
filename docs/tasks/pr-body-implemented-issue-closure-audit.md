## Issues

- Close #2
- Close #12
- Close #14
- Close #17
- Close #35
- Close #36
- Refs #9

## Why

`status:実装済み` を付けた open Issue のうち、本当に追加実装なしで閉じてよいものをコードと照合したうえで整理するため。

## Summary

実装済み Issue のクローズ監査メモを `docs/tasks/` に追加し、main のコードで完了を確認できた Issue だけをこの PR で close します。#9 は admin 権限相当の実装が確認できないため閉じません。

## Changes

- `docs/tasks/20260606-implemented-issue-closure-audit.md` に Issue ごとの確認結果を追加
- 追加実装不要と判断した #2 / #12 / #14 / #17 / #35 / #36 を PR merge 時に close する
- #9 を今回 close しない理由を明記

## Verification

- [x] `./scripts/validate-pr.sh`

## Notes

- コード変更はありません。Issue 整理用のドキュメント PR です。
