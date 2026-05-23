# Issue: PR 時に SwiftLint / iOS ビルド CI と日本語 PR テンプレート

## 背景

PR 作成時に Swift の品質チェックと iOS ビルド確認がなく、自動 PR の説明も最小限だった。

## 目的

- PR 時に **SwiftLint** と **iOS Simulator ビルド** を GitHub Actions で実行
- PR 本文テンプレートを **なぜ / 何が / どうなる / ユーザーがやるべきこと** の日本語構成に統一
- 自動 PR 作成時も同テンプレートを使用

## 受け入れ条件

- [ ] `ios-ci.yml` が PR で SwiftLint を実行
- [ ] `.github/pull_request_template.md` に「ユーザーがやるべきこと」セクションがある
- [ ] `open-pr-on-push.yml` がテンプレートを読み込み、既存 PR も同期する
