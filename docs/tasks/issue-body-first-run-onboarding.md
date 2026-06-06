# Issue: 初回オンボーディングとコアループ説明導線

## 背景

現状の Android 実装は `App()` からすぐ `HomeScreenView()` に入り、初回ユーザーに対して「勉強すると冒険が進む」「報酬でガチャ/強化ができる」「オフライン時は後で同期される」といったコアループを説明する導線がありません。

既存 Issue は認証・バトル・編成・報酬などの機能実装が中心で、初回体験そのものの Issue は見当たりませんでした。

## 目的

初めて起動したユーザーが、最初の1分でアプリの遊び方と次に押すべき行動を理解できる状態にする。

## スコープ

### Mobile

- [ ] 初回起動フラグをローカルに保持する
- [ ] 初回オンボーディング画面を追加する
- [ ] 「勉強開始」「冒険」「報酬」「編成/ガチャ」の関係を短く説明する
- [ ] 最後にホームの勉強開始 CTA へ誘導する
- [ ] 設定画面からオンボーディングを再表示できるようにする

### Docs / QA

- [ ] 初回起動時・再表示時・スキップ時の確認観点を docs/tasks に残す
- [ ] Android / iOS の表示差分を確認する

## 受け入れ条件

- 初回起動時にオンボーディングが表示される
- スキップまたは完了後は通常ホームに遷移する
- 2回目以降の起動では自動表示されない
- 設定画面から再表示できる
- ユーザーが「まず勉強を開始する」導線を迷わず見つけられる

## 参照

- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/App.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/home/HomeScreenView.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/settings/SettingsScreenDialog.kt`
- `docs/features/01_HomeScreen_Design.md`
