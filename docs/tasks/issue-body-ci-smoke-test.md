# Issue: CI/CD スモークテスト（PR 時の Actions 動作確認）

## 背景

PR #7 マージ後、main 上で Mobile / iOS / Backend CI と Auto open PR が期待どおり動くか再確認が必要。

## 目的

- **Backend — CI** / **Mobile — CI** / **iOS — CI** が PR で green になること
- **Auto open PR** が Issue 起票 + 説明文自動生成 + `Fixes #n` を行うこと

## スコープ（本イシュー）

- [ ] 各 workflow に触れる最小差分のみ
- [ ] PR 本文に Issue URL と issue-body リンクが含まれる
- [ ] iOS Xcode ビルド: `Compile Kotlin Framework` で JAVA_HOME 未設定時に Gradle が失敗する問題を修正

## 受け入れ条件

- [ ] PR の Checks タブで Backend / Mobile / iOS CI が success
- [ ] Auto open PR workflow が success
- [ ] マージ後に Issue が自動クローズ
