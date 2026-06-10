## Issues

- Close #71

## 背景

モバイルは KMP shared + Android Compose + iOS SwiftUI の構成ですが、現状のテストは `commonTest` / `androidUnitTest` のサンプルだけでした。SwiftTesting / XCTest より先に、Android/iOS両方に効く shared ロジックを `kotlin.test` で守り、CIとPR前validateで実行される状態にします。

## 概要

KMP shared の主要な純粋ロジックに単体テストを追加し、mobile CI と `./scripts/validate-pr.sh` で `:shared:testDebugUnitTest` / `:composeApp:testDebugUnitTest` を実行するようにします。SwiftTesting / XCTest は今回のスコープ外として、iOS固有は既存の SwiftLint + Simulator build で確認します。

## 変更内容

- `StageDropTable` の報酬変換・敵サマリー単体テストを追加
- `GachaBanner.primaryFeaturedForHero` の優先順位テストを追加
- `gachaBannerPeriodLabel` の表示 fallback テストを追加
- Android unit test のサンプルを smoke test に整理
- CI mobile job に KMP/Android unit test step を追加
- `scripts/validate-pr.sh` と `AGENTS.md` の mobile 検証手順を更新

## 確認項目

- [ ] `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest`（ローカルは Java Runtime 未設定のため未実行。CI で検証）
- [ ] `./scripts/validate-pr.sh`（権限付きで実行。backend test / go vet / shell syntax は成功、mobile は Java Runtime 未設定、assets/master はローカル PyYAML 未導入、iOS は local.properties 未生成で失敗。CI で検証）
- [x] `git diff --check`
- [x] `bash -n scripts/validate-pr.sh`
