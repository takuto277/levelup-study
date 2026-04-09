# バトルスプライト画像 配置ガイド

冒険画面のキャラクター・敵・背景の画像を差し替える手順です。

## 仕組み

- **画像が配置されていない場合**: 従来の絵文字表示にフォールバック
- **画像を配置すると**: 自動的にスプライト表示に切り替わる
- ランタイムでリソースの有無を検出するため、ビルドエラーにはならない

## 命名規則

### プレイヤースプライト

| ファイル名 | 用途 | フレーム |
|-----------|------|---------|
| `sprite_player_idle_1.png` | 待機モーション 1コマ目 | 必須 |
| `sprite_player_idle_2.png` | 待機モーション 2コマ目 | 任意（あればアニメーション） |
| `sprite_player_attack_1.png` | 攻撃モーション 1コマ目 | 必須 |
| `sprite_player_attack_2.png` | 攻撃モーション 2コマ目 | 任意 |
| `sprite_player_walk_1.png` | 歩行モーション 1コマ目 | 必須 |
| `sprite_player_walk_2.png` | 歩行モーション 2コマ目 | 任意 |

フレームは最大8枚まで対応（`_1` 〜 `_8`）。多いほど滑らかなアニメーションになる。

### 敵スプライト

| ファイル名パターン | 例 |
|-------------------|-----|
| `sprite_enemy_{key}_1.png` | `sprite_enemy_slime_1.png` |
| `sprite_enemy_{key}_2.png` | `sprite_enemy_slime_2.png` |

**対応している敵キー一覧:**

| key | 敵名 | ダンジョン |
|-----|------|-----------|
| `slime` | スライム | デフォルト |
| `goblin` | ゴブリン | デフォルト |
| `bat` | コウモリ / コウモリ群 | デフォルト / 洞窟 |
| `skeleton` | スケルトン | デフォルト |
| `orc` | オーク | デフォルト |
| `golem` | ゴーレム | デフォルト |
| `dark_wizard` | ダークウィザード | デフォルト |
| `chimera` | キメラ | デフォルト |
| `dragon` | ドラゴン | デフォルト |
| `demon` | デーモン | デフォルト |
| `mushroom` | 毒キノコ | 森 |
| `wolf` | ウルフ | 森 |
| `treant` | トレント | 森 |
| `forest_spirit` | フォレストスピリット | 森 |
| `bear` | ベアー | 森 |
| `crystal_golem` | クリスタルゴーレム | 洞窟 |
| `spider` | ケーブスパイダー | 洞窟 |
| `rock_worm` | ロックワーム | 洞窟 |
| `mimic` | ミミック | 洞窟 |
| `flame_imp` | フレイムインプ | 塔 |
| `fire_elemental` | ファイアエレメンタル | 塔 |
| `salamander` | サラマンダー | 塔 |
| `phoenix` | フェニックス | 塔 |
| `ifrit` | イフリート | 塔 |

### 背景画像

| ファイル名 | 対応ダンジョン |
|-----------|--------------|
| `bg_dungeon_default.png` | デフォルト / その他 |
| `bg_dungeon_forest.png` | 森系（名前に「森」「forest」を含む） |
| `bg_dungeon_cave.png` | 洞窟系（名前に「洞窟」「水晶」「cave」を含む） |
| `bg_dungeon_tower.png` | 塔系（名前に「塔」「炎」「tower」を含む） |

## 画像の配置先

### Android
```
apps/mobile/composeApp/src/androidMain/res/drawable/
  ├── sprite_player_idle_1.png
  ├── sprite_player_attack_1.png
  ├── sprite_player_walk_1.png
  ├── sprite_enemy_slime_1.png
  ├── sprite_enemy_goblin_1.png
  ├── ...
  ├── bg_dungeon_default.png
  ├── bg_dungeon_forest.png
  ├── bg_dungeon_cave.png
  └── bg_dungeon_tower.png
```

### iOS
```
apps/mobile/iosApp/iosApp/Assets.xcassets/
  ├── sprite_player_idle_1.imageset/
  │   ├── Contents.json
  │   └── sprite_player_idle_1.png
  ├── sprite_enemy_slime_1.imageset/
  │   ├── Contents.json
  │   └── sprite_enemy_slime_1.png
  ├── bg_dungeon_default.imageset/
  │   ├── Contents.json
  │   └── bg_dungeon_default.png
  └── ...
```

iOS の `Contents.json` テンプレート:
```json
{
  "images": [
    { "filename": "YOUR_IMAGE.png", "idiom": "universal", "scale": "1x" },
    { "idiom": "universal", "scale": "2x" },
    { "idiom": "universal", "scale": "3x" }
  ],
  "info": { "author": "xcode", "version": 1 }
}
```

## 画像作成のヒント

### 推奨サイズ
- キャラクター: **64×64px** 〜 **128×128px**（ドット絵は小さいサイズで作成→コード側で拡大）
- 背景: **640×360px** 以上（横長）
- フォーマット: PNG（透過対応）

### ドット絵のコツ
- `FilterQuality.None`（Android）/ `.interpolation(.none)`（iOS）で拡大するため、ぼやけずにピクセルがくっきり表示される
- 背景は透過にする（黒背景だとダンジョン背景と馴染まない）
- 各フレーム間でキャラクターの位置・サイズを揃える

### AI生成のプロンプト例
```
Pixel art, 64x64 sprite, [キャラクター説明],
16-bit retro JRPG style, clean pixel art,
transparent background, no anti-aliasing,
limited color palette
```
