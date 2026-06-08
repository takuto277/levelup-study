# KMP shared テスト基盤と主要ロジック単体テスト 実行計画

## 対象

- 関連 Issue: #71
- 対象領域:
  - `apps/mobile/shared/src/commonTest/kotlin/`
  - `apps/mobile/composeApp/src/androidUnitTest/kotlin/`
  - `.github/workflows/ci.yml`
  - `scripts/validate-pr.sh`
  - `AGENTS.md`

## 背景と目的

LevelUp Study のモバイルは KMP shared + Android Compose + iOS SwiftUI の構成。iOS 側は現時点でSwiftUI画面が中心で、SwiftTesting / XCTest を先に入れるより、Android/iOS両方に効く KMP shared のロジックを `kotlin.test` で守るほうが効果が高い。

現状は `commonTest` / `androidUnitTest` にサンプルテストだけがあり、CIも compile / build 中心。テストを書いても実行されない状態を避けるため、テスト追加と実行導線の整備を同時に行う。

## 実装方針

1. `SharedCommonTest` のサンプルを削除し、ドメイン寄りのテストクラスに分割する
2. `StageDropTableTest` を追加し、報酬変換と敵サマリーを検証する
3. `GachaBannerTest` を追加し、ヒーロー表示用ピックアップ選択を検証する
4. `GachaBannerPeriodTextTest` を追加し、期間ラベルの安定表示を検証する
5. `ComposeAppAndroidUnitTest` はサンプル演算から「Android unit test task が生きていること」を表す軽い smoke test に変える
6. CI mobile job に `:shared:testDebugUnitTest :composeApp:testDebugUnitTest` を追加する
7. `scripts/validate-pr.sh` と `AGENTS.md` に同じ検証を追加する

## テスト観点

| 対象 | 観点 | 期待 |
|------|------|------|
| StageDropTable | 確定 gold/xp/stones | UI報酬に合算される |
| StageDropTable | 低確率 stones | bonus item と drop rate として表示される |
| StageDropTable | enemy summary | sortOrder順、name/emoji/fallback が安定する |
| GachaBanner | featured priority | imageあり、character、先頭の順で選ばれる |
| GachaBannerPeriodText | date / blank / invalid | 日本語日付、`—`、元文字列 fallback |
| CI/validate | mobile変更時 | compile と unit test が同じ導線で走る |

## 検証計画

- `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest`
- `./scripts/validate-pr.sh`
- `git diff --check`

## リスクと対応

- iOS simulator test を Linux CI で直接回すのはコストが高い
  - 今回は shared common logic を Android unit test target で実行し、iOS固有は既存の macOS build で守る
- API / storage のテストは MockEngine や test actual が必要になりやすい
  - 初回は純粋ロジックを優先し、永続化・通信は別Issueで拡張する
- テスト実行時間が伸びる
  - unit test は compile より軽いため mobile job 内に追加し、instrumentation / screenshot は入れない
