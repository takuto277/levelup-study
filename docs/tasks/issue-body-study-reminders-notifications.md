# Issue: 学習リマインダー・通知設定

## 背景

学習アプリとしてリリースする場合、初回体験だけでなく継続利用を促す仕組みが必要です。現在はホーム・記録・報酬のループはありますが、勉強を忘れたユーザーへ戻ってきてもらうリマインダーや通知設定の Issue がありません。

## 目的

ユーザーが自分に合った時間に学習リマインダーを受け取り、継続・ストリーク・目標達成に戻りやすくする。

## スコープ

### Mobile

- [ ] 通知許可リクエストのタイミングを設計する
- [ ] 毎日の学習リマインダー時刻を設定できるようにする
- [ ] 通知 ON/OFF を設定画面に追加する
- [ ] 今日まだ勉強していない場合だけ通知する方針を検討する
- [ ] ストリークが切れそうな時の通知を検討する
- [ ] #49 のユーザー定義目標と連携し、目標未達時の通知を検討する

### UX

- [ ] 初回 onboarding / tutorial のどのタイミングで通知許可を求めるか決める
- [ ] 通知文言を複数パターン用意する
- [ ] 通知をしつこくしない頻度制限を決める

### 技術

- [ ] Android の通知 permission / channel を実装する
- [ ] iOS の local notification permission / scheduling を実装する
- [ ] タイムゾーン・日付境界の扱いを決める
- [ ] サーバー push が必要か、まず local notification で足りるか判断する

## 受け入れ条件

- ユーザーが通知 ON/OFF と通知時刻を設定できる
- Android / iOS で指定時刻に学習リマインダーが届く
- 初回からいきなり通知許可を求めず、文脈のあるタイミングで許可依頼できる
- 通知文言と頻度制限が docs にまとまっている
- 通知を無効にした場合は再スケジュールされない

## 関連 Issue

- #49 ユーザー定義目標・達成ボーナス機能
- #55 段階的チュートリアルと再表示導線の設計・実装

## 参照

- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/settings/SettingsScreenDialog.kt`
- `apps/mobile/iosApp/iosApp/SettingsScreenView.swift`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/record/`
