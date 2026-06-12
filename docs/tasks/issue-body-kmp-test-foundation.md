# Issue: KMP shared テスト基盤と主要ロジックの単体テスト追加

## 背景

現在のモバイル CI は KMP shared の Android compile と iOS framework link、iOS Swift build が中心で、`commonTest` / `androidUnitTest` はサンプルテストのみになっている。SwiftUI 側は画面実装が中心で SwiftTesting / XCTest のテストターゲットはまだ無く、まずは Android/iOS 両方に効く KMP shared のロジックを `kotlin.test` で守るのが費用対効果が高い。

直近の PR レビューでも、Android composeApp 側のコンパイルやテストが CI の検証範囲から漏れやすいことが見えた。テストコードを追加するだけでなく、PR 前 validate と CI でテストが実行される導線まで整える。

## 要件定義

- KMP のテストは `kotlin.test` を標準にする
- SwiftTesting / XCTest は今回のスコープに含めない
  - iOS 固有ロジックが増えるまでは SwiftUI 画面は iOS build / SwiftLint で守る
  - shared ロジックは KMP commonTest で Android/iOS 共通に守る
- 既存のサンプルテストを、実アプリの主要ロジックを検証するテストに置き換える
- PR 前の `./scripts/validate-pr.sh` と CI の mobile job で KMP/Android unit test が実行される
- テスト対象は UI スナップショットではなく、まず副作用の少ない純粋ロジックを優先する

## 設計

### テスト対象

- `StageDropTable`
  - `drop_table` から UI 報酬へ変換するロジック
  - 確定報酬と低確率ボーナス石の扱い
  - 敵構成サマリーの並び順と fallback 表示
- `GachaBanner.primaryFeaturedForHero`
  - 画像ありピックアップ優先
  - 画像がなければキャラ優先
  - キャラがなければ先頭、空なら `null`
- `gachaBannerPeriodLabel`
  - ISO 日付の日本語表示
  - 空文字、未知形式の fallback

### テスト配置

- `apps/mobile/shared/src/commonTest/kotlin/...`
  - OS 非依存の shared ロジック
- `apps/mobile/composeApp/src/androidUnitTest/kotlin/...`
  - Android アプリモジュールの unit test 起動確認

### 実行コマンド

- `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest`
- `./scripts/validate-pr.sh`
- CI mobile job でも同じ unit test を実行する

## スコープ

- `apps/mobile/shared/src/commonTest/...` の実テスト追加
- `apps/mobile/composeApp/src/androidUnitTest/...` のサンプルテスト整理
- `.github/workflows/ci.yml` の mobile unit test step 追加
- `scripts/validate-pr.sh` / `AGENTS.md` の検証コマンド更新
- `docs/tasks/` に実行計画・PR本文を残す

## スコープ外

- SwiftTesting / XCTest の導入
- Compose UI screenshot / instrumentation test
- Ktor MockEngine を使った API gateway テスト
- ローカル永続化 `KeyValueStore` の platform actual を含む統合テスト

## 受け入れ条件

- [ ] KMP shared のサンプルテストが実ロジックの単体テストへ置き換わっている
- [ ] `StageDropTable` / `primaryFeaturedForHero` / `gachaBannerPeriodLabel` に回帰テストがある
- [ ] `:shared:testDebugUnitTest` と `:composeApp:testDebugUnitTest` が PR 前 validate と CI で実行される
- [ ] `./scripts/validate-pr.sh` が mobile 変更時に compile と unit test を実行する
- [ ] SwiftTesting / XCTest を今回は導入しない理由が設計に明記されている
