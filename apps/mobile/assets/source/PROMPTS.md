# キャラクター フレーム生成プロンプト（ChatGPT 画像生成）

全フレームを **ChatGPT（画像生成）** で作成する。`[CHARACTER]` を任意のキャラ説明に置き換える。

## 方針（重要）

- **1キャラ = 1スレッド**で生成する。ChatGPT は会話の文脈を保つため、同一スレッドで続けるとキャラの同一性が上がる。
- **idle_1 を「正」として最初に作り込む**。以降のフレームは毎回 `idle_1` を添付し、**「キャラは完全維持・変えるのはポーズだけ」**で編集生成する。
- 全フレームで **足元（ベースライン）・頭の高さ・カメラ・スケール・構図**を固定する。
- 生成順は動きの流れ順（idle → walk → prep → attack → rest）。**1枚ずつ確認**してから次へ。
- 参照は**常に idle_1 に固定**（前コマ参照の連鎖はズレが累積しやすい）。

## 手順

1. 下の「idle 1（基準）」で idle_1 を生成し、納得いくまで修正。→ `sprite_player_idle_1.png` として保存（これが正本）。
2. 2枚目以降: **毎回 `idle_1` を添付**し、次のテンプレで生成する。
3. 生成画像は 1024〜1254px で出るので後処理で 512 に縮小（末尾のコマンド参照）。
4. 透過 → ベースライン整列 → WebP 化。

---

## 共通ブロック（毎プロンプトの末尾に必ず付ける）

```
Keep the character EXACTLY the same as the attached reference image:
same face, hair, colors, armor/clothing, proportions, camera angle, scale,
framing, and the same ground baseline (feet at the same height).
Change ONLY the pose described above. Solid dark background (removed later).
No text, no watermark, no extra characters, no style change, no color change.
```

---

## idle 1（基準画像）

```
Pixel art game sprite, 512x512.
Character: [CHARACTER]
Pose: standing, idle battle-ready, facing right (looking rightward), sword held at side.
Style: 16-bit retro JRPG, crisp pixel edges, no anti-aliasing, 32 colors max.
Solid dark background (will be removed later). No text, no watermark, no UI.
```

**[CHARACTER] 記入例:**

```
female holy knight, long flowing red hair, elegant silver-white plate armor
with gold trim and a crimson cape. Longsword in right hand, small shield on left arm.
Calm noble expression, gentle light glowing from the armor.
```

```
male dark mage, hooded black robe with purple trim, floating dark tome in left hand,
wispy shadow energy. Pale skin, glowing red eyes, silver hair peeking from the hood.
```

---

## ポーズ表（各フレームの差分を具体的に指定）

各項目を「添付した idle_1 を編集して」に続けて指定する。関節・重心・足位置まで書くと滑らかになる。

### idle 2
```
Idle frame 2 of 2. Gentle exhale: body sinks ~1px, chest slightly down,
shoulders relax. Arms and sword unchanged from idle_1.
```

### walk 1
```
Walk frame 1 of 2. Left foot steps forward ~1 step, right foot back.
Slight forward lean, weight forward. Sword held at side, cape trailing lightly.
```

### walk 2
```
Walk frame 2 of 2. Right foot steps forward, left foot back (mirror of walk 1).
Body lifted slightly, cape swaying back. Sword at side.
```

### prep 1
```
Prepare frame 1 of 2. Grip the sword tighter, knees slightly bent,
sword rising to shoulder height, elbows drawing in.
```

### prep 2
```
Prepare frame 2 of 2. Full wind-up: sword pulled back behind the right shoulder,
torso rotated back, weight on the rear foot, body leaning slightly forward.
```

### attack 1
```
Attack frame 1 of 5. Forward lunge: right foot steps forward,
sword begins to swing from behind, torso starting to rotate forward.
```

### attack 2
```
Attack frame 2 of 5. Sword mid-swing at ~45° diagonal,
torso rotated forward, arms extended, thin white arc tracing the blade path.
```

### attack 3
```
Attack frame 3 of 5. Peak impact: sword slashing horizontally across the center,
strong dynamic pose, bright slash effect line, weight fully forward.
```

### attack 4
```
Attack frame 4 of 5. Follow-through: sword swings through to low left,
body momentum carrying forward, cape flowing around.
```

### attack 5
```
Attack frame 5 of 5. Recovery: sword lowering, body straightening,
returning toward idle_1 stance, cape settling.
```

### rest 1
```
Rest pose: sitting cross-legged on the ground, sword resting across the lap,
eyes closed, breathing deeply, calm expression.
```

---

## 滑らかにするコツ

- **差分で指示する**: 「前のコマより腕を約15°上げる」のように、変化量を小さく具体的に。
- **アンカーを固定**: 頭頂の高さ・腰の位置・両足の接地位置を毎フレーム同じにする。
- **関節を明示**: 肩/肘/膝の角度、体のひねり、重心（前足/後ろ足）を書く。
- **連続フレームは1枚ずつ**: 大きく飛ばさず、動きの連続性を確認しながら進める。
- どうしてもバラつく場合の代替: 「同じキャラで attack の5フレームを横一列に並べた
  スプライトシート（暗い背景）」を1枚で依頼し、切り出して使う（1回の生成でスタイルが揃う）。

---

## File Mapping

| # | ファイル名 | 状態 |
|---|-----------|------|
| 1 | `sprite_player_idle_1.png` | idle 1（基準） |
| 2 | `sprite_player_idle_2.png` | idle 2 |
| 3 | `sprite_player_prep_1.png` | prep 1 |
| 4 | `sprite_player_prep_2.png` | prep 2 |
| 5 | `sprite_player_attack_1.png` | attack 1 |
| 6 | `sprite_player_attack_2.png` | attack 2 |
| 7 | `sprite_player_attack_3.png` | attack 3 |
| 8 | `sprite_player_attack_4.png` | attack 4 |
| 9 | `sprite_player_attack_5.png` | attack 5 |
| 10 | `sprite_player_rest_1.png` | rest 1 |
| 11 | `sprite_player_walk_1.png` | walk 1 |
| 12 | `sprite_player_walk_2.png` | walk 2 |

## Post-Generation Commands

```bash
# 1) 生成画像は 1024〜1254px で出るので 512x512 に縮小
sips -z 512 512 <生成画像>.png

# 2) Background transparency（四隅フラッドフィル）
#    バトル同梱スプライト
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind player
#    ガチャ立ち絵（backend/assets/master/source/characters/）
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind character

# 3) Sync to Android/iOS（バトル同梱スプライトのみ）
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py

# 4) Validate
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

> 足元（ベースライン）が揃わない場合は、縮小前後に全コマの足元ラインを揃えてから透過する。
> `--kind character` は `backend/assets/master/source/characters/` 配下の PNG/WebP を一括で背景透過します。
