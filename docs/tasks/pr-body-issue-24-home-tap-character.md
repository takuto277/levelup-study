## Issues

- Close #24

## Why

ホーム画面の `HomeIntent.TapMainCharacter` が空実装のままで、設計どおりキャラタップ時にセリフが切り替わらない。セリフのローテーションも UI 層に分散していたため、KMP の ViewModel に集約する。

## Summary

ホーム中央キャラのセリフ管理を `HomeViewModel` に移し、タップで次のセリフへ進む `TapMainCharacter` を実装した。Android / iOS 双方でキャラエリアのタップを Intent に接続した。

## Changes

- `HomeCharacterDialogue` を追加し、待機セリフ一覧を KMP 共通定義にした
- `HomeUiState` に `characterMessageIndex` / `characterMessage` を追加
- `HomeViewModel` で 4 秒ごとの自動ローテーションとタップ時のセリフ更新を実装
- Android `HomeTabContent` / iOS `HomeScreenView` からローカルタイマーを削除し、タップで `TapMainCharacter` を発火

## Verification

- `./scripts/validate-pr.sh --all` — backend test / go vet passed; mobile compile skipped locally（JRE 未インストール）
- push 後の GitHub Actions（mobile compile）で最終確認
