## Issues

- Refs #62

## Why

学習リマインダー通知はリリース後の継続利用に効くが、初回起動直後に通知許可を求めると拒否されやすい。Android / iOS の通知 API 差も大きいため、実装前に要件・UX・保存データ・platform 実装方針を整理しておく。

## Summary

#62 の要件定義と設計を `docs/tasks/` に追加した。MVP ではサーバー Push ではなく、設定画面から ON/OFF・時刻設定できる端末ローカル通知として実装する方針にした。

## Changes

- `docs/tasks/issue-body-study-reminders-notifications.md` に Issue 本文を保存
- `docs/tasks/20260607-study-reminders-notifications.md` に要件定義・設計を追加
- 通知許可タイミング、設定 UI、shared 保存モデル、Android / iOS platform 実装方針、QA 観点を整理

## Verification

- `./scripts/validate-pr.sh` - passed（docs のみの変更のため自動チェック対象なし）
