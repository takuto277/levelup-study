# 同梱バトルスプライトの WebP 化 & パイプライン拡張子対応

作成日: 2026-10-11
ブランチ: `feat/character-webp-assets`

## 目的

アプリに同梱しているバトルスプライト（プレイヤー・敵）の PNG を WebP へ切り替え、アプリサイズを削減する。あわせて同期パイプライン（`sync` / `validate`）を **PNG/WebP 両対応（拡張子許容）** にする。

## 背景

- 同梱スプライトは 1254×1254（プレイヤー）／1376×768（敵）の PNG で、合計 **71.3MB**。
- WebP は両 OS で標準対応（iOS deployment target 18.2 / Android minSdk 24）。
- 既存の `scripts/assets/*` は PNG 前提（`*.png` glob、iOS imageset の filename、`--clean` の orphan 判定など）。

## 方針

1. 同期・検証を **拡張子に依存しない**実装へ変更（`resolve_source_file` / `find_image_with_ext` / `collect_managed_stems`）。
2. ソースの PNG を WebP(q=90) に一括変換し、PNG を削除。
3. `sync` で Android drawable / iOS xcassets へ反映。拡張子変更時に旧拡張子の残骸を自動削除。

## 実装詳細（理由付き）

- **stem ベースの管理名（`collect_managed_stems`）**: OS 側の実ファイルは `.webp`/`.png` のいずれかになるため、ファイル名（拡張子込み）比較では orphan 判定・同期判定が壊れる。拡張子を除いた stem で比較することで、移行期も安定的に扱える。
- **`resolve_source_file`**: manifest は `.png` のままでも、実体が `.webp` なら解決できる（後方互換）。移行を段階的に進められる。
- **`_remove_stale_siblings`**: 拡張子を変えると旧ファイルが残る（例: iOS imageset 内の旧 PNG）。sync 時に同一 stem の別拡張子を削除して二重管理を防ぐ。`--clean` が imageset ごと消してしまう事故も避ける。
- **q=90（lossy）**: 透過キャラ/敵アートの輪郭品質を確保しつつ十分縮小。実測 **71.3MB → 5.6MB（約 92% 削減）**。
- **同梱 = character 00001**: 既存の汎用プレイヤー `sprite_player_*` を character 00001 のフレームとして扱う（`_old` は対象外）。DL 不要でオフラインでもそのまま表示できる。

## 手順

```bash
# 1. PNG -> WebP 変換（player 12 + enemy 60 = 72枚）
#    apps/mobile/assets/source/ の sprite_player_*.png / sprite_enemy_*.png
# 2. 同期・検証
./scripts/assets/run.sh scripts/assets/generate_enemy_sprite_assets.py --check
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

## 完了条件

- [x] `source/` のプレイヤー・敵を WebP 化（72枚, 71.3MB → 5.6MB）
- [x] Android drawable / iOS xcassets を同期し、残骸 PNG を削除
- [x] `generate --check` / `sync --check` / `validate` が通る

## 影響範囲

- `scripts/assets/lib/manifest.py`, `scripts/assets/sync_battle_assets.py`, `scripts/assets/validate_assets.py`
- `apps/mobile/assets/manifest.yaml`, `apps/mobile/assets/source/**`, `apps/mobile/composeApp/src/androidMain/res/**`, `apps/mobile/iosApp/iosApp/Assets.xcassets/**`
- docs: `docs/assets/01_Asset_Ingestion_Workflow.md`, `apps/mobile/assets/README.md`

## 後続（未対応）

- 背景・小物・ガチャバナーの WebP 化（今回はプレイヤー・敵のみ）。
- キャラ別フレーム（character_00002 以降）のフレーム解決とアプリ領域キャッシュ（別タスク）。
