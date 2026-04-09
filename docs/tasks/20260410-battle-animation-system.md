# タスク: 冒険画面バトルアニメーションシステム

| 項目 | 値 |
|------|-----|
| 作成日 | 2026-04-10 |
| ステータス | 進行中 |
| 担当 | AI / 共同 |

## 概要
勉強タイマー中の冒険画面（`StudyQuestScreenView`）を、絵文字ベースの簡素な表示から、ドット絵スプライトによるアニメーション戦闘画面にアップグレードする。プレイヤー・敵キャラのフレームアニメーション、ダンジョン別背景、斬撃エフェクトを実装する。

## 要件
- [x] プレイヤーキャラがフレームアニメーションで動く（idle/attack/walk）
- [x] 敵キャラもフレームアニメーションで動く（idle + 被弾リアクション）
- [x] ダンジョンタイプ別の背景画像切り替え
- [x] 攻撃時の斬撃エフェクト（Canvas描画）
- [x] ダメージ数字の浮遊アニメーション
- [x] 画像未配置時は現行の絵文字表示にフォールバック
- [x] Android / iOS 両対応

## 影響範囲
- `shared/.../features/study/StudyQuestUiState.kt` — enemySpriteKey 追加
- `shared/.../features/study/StudyQuestViewModel.kt` — 敵→スプライトキーマッピング
- `composeApp/.../features/study/StudyQuestScreenView.kt` — AdventureScene 全面書き換え
- `iosApp/iosApp/StudyQuestScreenView.swift` — adventureScene 全面書き換え
- 新規: `composeApp/.../components/AnimatedSprite.kt`
- 新規: `iosApp/iosApp/Components/AnimatedSpriteView.swift`
- 新規: アセットディレクトリ（Android res/drawable, iOS Assets.xcassets）

## 実装手順
1. 共有層に `enemySpriteKey` を追加し、ViewModelで敵名→キーのマッピング
2. Android用 `AnimatedSprite` コンポーネント作成
3. Android `AdventureScene` を書き換え（背景 + スプライト + エフェクト）
4. iOS用 `AnimatedSpriteView` 作成
5. iOS `adventureScene` を書き換え
6. プレースホルダー画像生成・配置
7. 画像差し替え手順の README 作成

## 実装詳細

### 1. 画像リソース命名規則
```
sprite_player_idle_1.png / sprite_player_idle_2.png  (待機 2フレーム)
sprite_player_attack_1.png / sprite_player_attack_2.png  (攻撃 2フレーム)
sprite_player_walk_1.png / sprite_player_walk_2.png  (歩行 2フレーム)
sprite_enemy_{key}_1.png / sprite_enemy_{key}_2.png  (敵 idle 2フレーム)
bg_dungeon_{type}.png  (背景)
```
- **理由**: 個別フレーム画像方式を採用。AI生成で1コマずつ作りやすく、差し替えも容易。スプライトシート方式より管理がシンプル。

### 2. ランタイムリソース検出 + 絵文字フォールバック
```kotlin
// Android: リソースが存在するか実行時チェック
val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
if (resId != 0) { /* スプライト描画 */ } else { /* 絵文字フォールバック */ }
```
```swift
// iOS: アセットが存在するか実行時チェック
if UIImage(named: name) != nil { /* スプライト描画 */ } else { /* 絵文字フォールバック */ }
```
- **理由**: 画像が未配置でもアプリが動作し、画像を入れるだけで自動的にスプライト表示に切り替わる。

### 3. フレームアニメーション
- idle: 200ms間隔で2フレームを交互表示
- attack: 150ms間隔で2フレームを交互表示 + 前方へのlunge移動
- walk: 200ms間隔で2フレームを交互表示 + X方向の移動
- **理由**: 2フレームでも交互表示+位置アニメーションで十分な動きが出る。フレーム数を増やしたい場合は画像を追加するだけ。

### 4. 背景
- ダンジョン名からタイプを判定（森/洞窟/塔/デフォルト）
- 対応する背景画像を表示、なければグラデーション背景にフォールバック
- **理由**: 背景画像1枚で雰囲気が大きく変わる。最もコスパの良い視覚改善。

## リスク・注意点
- AI生成画像のサイズ・スタイルの一貫性は手動調整が必要
- ピクセルアートは FilterQuality.None（最近傍補間）で拡大しないとぼやける
- 画像ファイルサイズに注意（ドット絵なら数KB程度で済むはず）

## テスト計画
- 画像なし状態でビルド・実行 → 絵文字フォールバックが動作
- 画像配置後にビルド・実行 → スプライトアニメーションが動作
- 各フェーズ（WALKING/ENCOUNTER/ATTACKING/ENEMY_DEFEATED/PLAYER_DEAD/FLOOR_CLEAR）の表示確認
