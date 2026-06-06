# 実装済み Issue クローズ監査

## 対象

- 監査日: 2026-06-06
- 対象: open のまま残っている `status:実装済み` Issue
- 基準: `origin/main` に入っているコードとドキュメントを確認し、追加実装なしで閉じてよいものだけを PR の `Close` 対象にする

## クローズ対象

| Issue | 判定 | 根拠 |
|------:|------|------|
| #2 | クローズ可 | `apps/mobile/assets/source/`、`apps/mobile/assets/manifest.yaml`、`scripts/assets/sync_battle_assets.py`、`scripts/assets/validate_assets.py`、`docs/assets/01_Asset_Ingestion_Workflow.md`、`.cursor/rules/assets.mdc`、`.cursor/skills/feature-delivery/SKILL.md` が存在し、`docs/tasks/20260523-asset-ingestion-pipeline.md` でも完了扱い。CI は `.github/workflows/ci.yml` の assets job で generate / sync check / validate を実行している。 |
| #12 | クローズ可 | `backend/internal/router/router.go` にキャラ/武器 LvUP API、`backend/internal/handler/game_handler.go` に gold 消費つき LvUP 実装、`apps/mobile/shared/.../CharacterGateway.kt` / `WeaponGateway.kt` / `PartyViewModel.kt` / `PartyUseCase.kt` にモバイル呼び出し経路、Android/iOS 編成詳細 UI に LvUP ボタンがある。 |
| #14 | クローズ可 | Android は `PartyScreenView.kt` の `1..4` スロット、iOS は `PartyScreenView.swift` の `ForEach(1...4)` で 4 スロット表示・配置・解除導線がある。 |
| #17 | クローズ可 | `StudyQuestViewModel.kt` が `Clock.System.now().toEpochMilliseconds()` ベースの wall clock 計測を持ち、pause/resume 中の経過を `pausedAccumulatedMs` / `pauseStartedMs` で補正している。単純な coroutine tick 依存から外れており、バックグラウンド復帰後も経過時間を再計算できる。 |
| #35 | クローズ可 | #36 と同じ自動起票本文の重複。実体の #1 / #19 / #22 は PR #37 相当のコードで対応済み。冒険報酬は `QuestUseCase.kt` / `StageDropTable.kt` が stage `drop_table` から UI 報酬を作り、middleware test は `backend/internal/middleware/auth_middleware_test.go` にある。 |
| #36 | クローズ可 | PR #37 の自動起票 Issue として残っているが、PR 本文は #1 / #19 / #22 を `Fixes` しており、対応コードも main に存在する。追加実装は不要。 |

## 今回クローズしないもの

| Issue | 判定 | 理由 |
|------:|------|------|
| #9 | クローズしない | `OwnerGuard`、キャラ/武器/パーティ操作の所有者検証、master genre 書き込みの JWT 必須化までは確認できた。一方で Issue 派生本文には「マスタジャンル POST/DELETE に Admin API Key」とあり、現コードは通常の API Key + JWT で、admin 専用権限までは確認できない。追加実装不要と言い切れないため、この PR では閉じない。 |

## 参照した主なファイル

- `backend/internal/router/router.go`
- `backend/internal/handler/game_handler.go`
- `backend/internal/handler/game_auth.go`
- `backend/internal/middleware/owner_guard.go`
- `backend/internal/middleware/auth_middleware_test.go`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/study/StudyQuestViewModel.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/party/PartyViewModel.kt`
- `apps/mobile/composeApp/src/androidMain/kotlin/org/example/project/features/party/PartyScreenView.kt`
- `apps/mobile/iosApp/iosApp/PartyScreenView.swift`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/quest/QuestUseCase.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/quest/StageDropTable.kt`
- `apps/mobile/assets/manifest.yaml`
- `scripts/assets/sync_battle_assets.py`
- `scripts/assets/validate_assets.py`
