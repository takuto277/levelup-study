# キャラクターアニメーション生成プロンプト決定版

levelup-study の 2D キャラクターを ChatGPT 画像生成などで作るための制作仕様。
目的は「同じキャラに見える」ことと「少ない枚数でも滑らかに見える」こと。

このゲームの標準フレーム構成は次の 14 枚。

| mode | 枚数 | 用途 |
| --- | ---: | --- |
| `idle` | 2 | 待機ループ |
| `prep` | 2 | 攻撃前のため |
| `attack` | 5 | 攻撃の本動作 |
| `rest` | 1 | 休憩表示 |
| `walk` | 4 | ホーム/冒険中の歩行 |

戦闘の気持ちよさだけなら `idle 2 + prep 2 + attack 5 = 9枚` で十分に成立する。
ただしアプリ全体では休憩と歩行も使うため、納品単位は 14 枚にそろえる。

## 最重要ルール

- **1キャラ = 1スレッド**で作る。途中で別スレッドに移すと顔・装備・等身が崩れやすい。
- 最初に `idle_1` を作り込み、以降は **必ず `idle_1` を参照画像として添付**する。
- 2枚目以降は「キャラを描き直す」のではなく、**同じキャラのポーズだけを編集する**。
- 全フレームで **キャンバス、カメラ角度、キャラの大きさ、頭頂の高さ、足元ベースライン**を固定する。
- 参照の優先順位は `idle_1` が最上位。前後コマを添付する場合も、同一性は `idle_1` に合わせる。
- 背景は単色の暗い背景にする。後処理で透過するため、背景模様・床・影・粒子を増やさない。
- 攻撃エフェクトは `attack_2` から `attack_4` までに限定し、キャラ本体を隠さない。

## 推奨ワークフロー

### 1. `idle_1` を正本として作る

まず `idle_1` だけを作る。ここでキャラデザインを確定させる。

```text
Create a 512x512 pixel-art JRPG battle sprite.

Character:
[[CHARACTER_DESCRIPTION]]

Pose:
Neutral combat idle pose, facing right, three-quarter side view.
The character stands firmly on a single invisible ground baseline.
The body is readable as a small game sprite, not a portrait.

Style:
16-bit fantasy JRPG pixel art, crisp pixels, clean silhouette, limited palette,
clear readable details, no painterly brushwork, no 3D render, no soft blur.

Canvas:
512x512 square canvas.
The character is centered and occupies about 60-70% of the canvas height.
Keep empty margin around the character so no weapon, hair, cape, or effect is cropped.
Use a solid dark plain background for later transparency removal.

Do not include text, labels, UI, watermark, frame borders, or extra characters.
```

`[[CHARACTER_DESCRIPTION]]` は、髪・顔・服・装備・色・武器・雰囲気まで固定する。

例:

```text
A young male blue-robed mage with short silver hair and calm eyes.
He wears a deep blue hooded robe with silver trim, a small leather belt,
dark boots, and a short shoulder cape. He holds a wooden staff with a small blue crystal.
The design is simple enough to read clearly as a small mobile game sprite.
```

```text
A noble female holy knight with long red hair, white-silver plate armor with gold trim,
a crimson cape, dark boots, a longsword in her right hand, and a small round shield on her left arm.
Her expression is calm and brave. The armor design is clean and consistent, not overly detailed.
```

### 2. 14枚の動き設計を先に固定する

生成前に、動きの役割を固定する。

- `idle_1`: 基準姿勢
- `idle_2`: 呼吸の差分。動きは小さい
- `prep_1`: ため始め。膝と肩を少し落とす
- `prep_2`: 最大のため。攻撃方向と逆に体をひねる
- `attack_1`: 解放開始。前足へ体重移動
- `attack_2`: 加速。武器/手/魔法が最速で動く
- `attack_3`: インパクト。もっとも強いシルエット
- `attack_4`: 振り抜き。勢いが残る
- `attack_5`: 戻り。次の idle へつながる
- `rest_1`: 休憩。戦闘外で使う単独ポーズ
- `walk_1`: 左足前で接地
- `walk_2`: 左足に体重が乗り、膝を折って少し沈む
- `walk_3`: 右足前で接地
- `walk_4`: 右足に体重が乗り、膝を折って少し沈む

### 3. 1枚ずつ生成する

毎回 `idle_1` を添付し、下の「共通ロック」と「各フレーム指定」を合わせて送る。
滑らかさを優先する場合は、すでに承認した直前/直後フレームも添付してよい。
その場合でも、顔・服・装備は `idle_1` に合わせる。

## 共通ロック

2枚目以降のすべてのプロンプトに必ず付ける。

```text
Use the attached idle_1 image as the identity reference.
Create exactly one animation frame of the same character.

Keep these exactly consistent with idle_1:
- same face, expression style, hairstyle, hair color, eye color, and skin tone
- same outfit, armor, accessories, cape, weapon, and color palette
- same body proportions, head size, limb length, and silhouette language
- same pixel-art style, pixel density, line weight, contrast, and shading style
- same camera angle, facing direction, sprite scale, and canvas size
- same invisible ground baseline and same approximate head height

Change only the pose described below.
Do not redesign the character.
Do not add or remove equipment.
Do not change weapon length or hand dominance.
Do not zoom, rotate the camera, crop the sprite, or move the character off center.

Output requirements:
- 512x512 square image
- solid dark plain background for later transparency removal
- crisp pixel art, no blur, no painterly texture, no 3D render
- no text, labels, watermark, UI, frame number, border, or extra characters
```

## ネガティブプロンプト

必要に応じて末尾に足す。

```text
Avoid: redesigned face, different hairstyle, changed clothing, changed armor,
changed weapon, duplicate weapon, missing weapon, extra limbs, extra fingers,
different body proportions, different sprite scale, different camera angle,
floating feet, changed ground baseline, cropped weapon, cropped hair, busy background,
large shadow, scene background, text, label, watermark, UI, soft blur, 3D render,
anime illustration style, painterly style, realistic lighting.
```

## 各フレーム指定

`[[ATTACK_STYLE]]` にはキャラごとの攻撃方法を入れる。
例: `a right-handed sword slash`, `a staff magic burst`, `a quick spear thrust`。

### `idle_2`

```text
Frame: idle_2 of 2.

Motion role:
Subtle breathing frame that loops naturally with idle_1.

Pose:
The character remains standing in the same place, facing right.
Lower the chest and shoulders by about one pixel compared with idle_1, as if exhaling softly.
The knees bend only slightly.
Hair, cape, sleeves, and small cloth parts move by only a tiny amount.
Weapon stays in the same hand and nearly the same position.

Keep the motion very small. This frame must not look like a new pose.
```

### `prep_1`

```text
Frame: prep_1 of 2.

Motion role:
The first anticipation frame before the attack.

Pose:
The character begins preparing [[ATTACK_STYLE]].
Lower the center of gravity slightly.
Bend the knees.
Bring the weapon, casting hand, or attack focus closer to the body.
Shoulders tighten and the torso leans forward only a little.
Feet stay planted on the same ground baseline.

The pose should clearly lead into prep_2, but still feel controlled.
```

### `prep_2`

```text
Frame: prep_2 of 2.

Motion role:
Maximum anticipation. This is the strongest wind-up before the attack.

Pose:
The character coils the body in the opposite direction of [[ATTACK_STYLE]].
Pull the weapon, casting hand, or attack focus back behind the body.
Twist the shoulders and hips.
Shift weight onto the rear foot.
Keep the front foot visible and grounded.
Cape, hair, and loose cloth lag slightly behind the body motion.

This frame should have a strong readable silhouette and make the next attack frame feel powerful.
```

### `attack_1`

```text
Frame: attack_1 of 5.

Motion role:
Release begins.

Pose:
The character starts [[ATTACK_STYLE]] from prep_2.
Drive the hips and shoulders forward.
Shift weight from the rear foot toward the front foot.
The front foot plants firmly on the baseline.
The weapon, casting hand, or attack focus starts moving forward, but the impact has not happened yet.

No large effect yet. Keep the silhouette clean.
```

### `attack_2`

```text
Frame: attack_2 of 5.

Motion role:
Fast acceleration frame.

Pose:
The attack is moving at high speed.
The weapon, hand, or attack focus crosses the body at roughly a diagonal angle.
Arms extend more than attack_1.
Torso rotation is clear.
The cape, hair, and cloth trail behind the motion.

Effect:
Add only a thin controlled motion smear or light arc that follows the attack direction.
The effect must not cover the face or hide the body silhouette.
```

### `attack_3`

```text
Frame: attack_3 of 5.

Motion role:
Impact frame. This is the hit frame and the visual peak of the attack.

Pose:
The character reaches maximum extension for [[ATTACK_STYLE]].
Weight is fully over the front foot.
The torso, arms, and weapon or casting focus form the strongest action line.
The face stays consistent with idle_1, but the expression may be slightly more intense.

Effect:
This frame may have the brightest slash, magic flash, or impact accent.
Keep it compact and readable. Do not add a full scene background.
```

### `attack_4`

```text
Frame: attack_4 of 5.

Motion role:
Follow-through after impact.

Pose:
The attack has passed through the target direction.
The weapon, hand, or attack focus continues beyond the impact point.
The torso is still rotating forward.
The cape and hair continue moving with delayed momentum.
Balance remains believable, feet still connected to the same baseline.

Effect:
The attack effect fades and becomes weaker than attack_3.
```

### `attack_5`

```text
Frame: attack_5 of 5.

Motion role:
Recovery frame that connects back to idle_1.

Pose:
The character begins returning from the follow-through to the neutral stance.
Lower the weapon, hand, or attack focus.
The torso straightens.
The cape, hair, and cloth settle.
Feet remain grounded.

This frame should not be identical to idle_1, but it must clearly lead back into idle_1.
No large attack effect remains.
```

### `rest_1`

```text
Frame: rest_1.

Motion role:
Single rest pose for non-combat scenes.

Pose:
The same character is resting calmly on the ground while still facing right.
Use a compact seated or kneeling pose that fits the same 512x512 sprite scale.
Keep the same outfit, equipment, colors, and face.
Place the weapon or tool safely near the body or across the lap.
The pose should feel peaceful and readable at small size.

Keep the same dark plain background and do not add scenery.
```

### `walk_1`

```text
Frame: walk_1 of 4.

Motion role:
Left-foot contact frame of a four-frame walk loop.

Pose:
The character walks to the right.
Left foot is forward and planted on the baseline.
Right foot is slightly behind.
Torso leans forward very slightly.
Arms and weapon counterbalance the step without changing the character design.
Cape, hair, and cloth move subtly backward.

Keep the character centered and at the same scale.
```

### `walk_2`

```text
Frame: walk_2 of 4.

Motion role:
Weight-down frame after walk_1. This is the knee-bend frame that prevents sliding.

Pose:
The character walks to the right.
The left foot remains forward and planted.
The left knee bends slightly as the body weight lands on it.
The body sinks about 1-2 pixels compared with walk_1.
The rear foot begins to lift lightly from the heel.
Shoulders and hips drop subtly with the weight.
Arms, weapon, cape, hair, and cloth follow the downward motion with a tiny delay.

Do not make this a new stride. It is the in-between compression after walk_1.
Keep the character centered and at the same scale.
```

### `walk_3`

```text
Frame: walk_3 of 4.

Motion role:
Right-foot contact frame, opposite of walk_1.

Pose:
The character walks to the right.
Right foot is forward and planted on the baseline.
Left foot is slightly behind.
Torso leans forward very slightly.
Arms and weapon counterbalance the step in the opposite direction from walk_1.
Cape, hair, and cloth move subtly backward.

This frame should feel like the opposite-foot version of walk_1, not a different character pose.
Keep the character centered and at the same scale.
```

### `walk_4`

```text
Frame: walk_4 of 4.

Motion role:
Weight-down frame after walk_3. This is the second knee-bend frame.

Pose:
The character walks to the right.
The right foot remains forward and planted.
The right knee bends slightly as the body weight lands on it.
The body sinks about 1-2 pixels compared with walk_3.
The rear foot begins to lift lightly from the heel.
Shoulders and hips drop subtly with the weight.
Arms, weapon, cape, hair, and cloth follow the downward motion with a tiny delay.

This frame must loop cleanly back into walk_1.
Keep the character centered and at the same scale.
```

## スプライトシート方式

1枚ずつ生成してブレる場合は、先に 14 コマのスプライトシートを作る。
同一生成内に全コマを並べるため、顔・服・配色がそろいやすい。
品質がよければ切り出して使う。崩れたコマだけ、上の個別プロンプトで修正する。

```text
Using the attached idle_1 as the identity reference, create a 14-frame pixel-art animation sprite sheet
of the exact same character.

Layout:
7 columns x 2 rows, equal square cells, no labels, no text.
Frame order:
row 1: idle_1, idle_2, prep_1, prep_2, attack_1, attack_2, attack_3
row 2: attack_4, attack_5, rest_1, walk_1, walk_2, walk_3, walk_4

Animation:
The motion is [[ATTACK_STYLE]].
The sequence must read smoothly as:
neutral idle -> subtle breath -> anticipation -> maximum wind-up -> release -> acceleration -> impact -> follow-through -> recovery.
The walk frames must read as:
left foot contact -> left knee bends/body sinks -> right foot contact -> right knee bends/body sinks.

Consistency:
Keep the same face, hairstyle, outfit, equipment, colors, body proportions, pixel-art style,
camera angle, character scale, and ground baseline in every cell.

Canvas:
Dark plain background in every cell for later transparency removal.
No text, frame numbers, UI, watermark, scene background, or extra characters.
No cell may crop the character, weapon, hair, cape, or attack effect.
```

## 修正用プロンプト

### キャラが別人になった

```text
Regenerate this frame.
The pose direction is correct, but the character identity drifted.
Match idle_1 much more strictly:
same face, same hair shape, same colors, same outfit, same armor/accessories,
same weapon, same body proportions, same sprite scale.
Only the pose may change.
```

### 足元や高さがズレた

```text
Regenerate this frame.
Keep the exact same invisible ground baseline as idle_1.
The feet must touch the same horizontal ground line.
Do not move the camera.
Do not zoom.
Keep the head height and character scale consistent with idle_1.
```

### 攻撃が弱い

```text
Regenerate this frame with a stronger action line.
Keep the same character design, but exaggerate the pose:
clearer torso twist, stronger shoulder direction, stronger weight shift,
more readable weapon/hand direction.
Do not add a busy background or large effect.
```

### エフェクトが邪魔

```text
Regenerate this frame with a smaller, cleaner attack effect.
The effect should support the motion, not cover the character.
Keep the face, torso, hands, weapon, and silhouette visible.
No large particles, no background scene, no full-screen flash.
```

### 2コマループがカクつく

```text
Regenerate this frame as a smaller difference from the paired frame.
Keep the same body height, same baseline, and same scale.
Only adjust shoulders, chest, cloth, hair, and limb position slightly.
The frame must loop naturally with the other frame.
```

## 滑らかに見せる考え方

少ない枚数で滑らかに見せる鍵は「中割りの多さ」よりも、各コマの役割。

- `idle` は差分を小さくする。動きすぎると落ち着かない。
- `prep` は大きくためる。攻撃が強く見える。
- `attack_1` から `attack_3` は素早く大きく動く。ここで勢いを作る。
- `attack_3` は一番強い絵にする。ダメージ発生タイミングに合う。
- `attack_4` と `attack_5` は戻りを見せる。急に idle に戻さない。
- `walk` 4枚は「接地 -> 膝を折って沈む -> 反対足で接地 -> 膝を折って沈む」にする。2枚歩行よりスライド感が減る。

アプリ側の現行タイミング目安:

| mode | 表示間隔 |
| --- | ---: |
| `idle` | 600ms |
| `walk` | 1周期約640ms（2枚なら320ms/枚、4枚なら160ms/枚） |
| `prep` | 60ms |
| `attack` | 60ms |
| `rest` | 静止 |

攻撃は短時間で流れるため、`attack_2` と `attack_3` のシルエットを特に強くする。

## 品質チェック

生成後、全コマを並べて次を確認する。

- 顔・髪・服・装備・色が同じ
- 武器や杖の長さが変わっていない
- 体格、頭身、頭の大きさが同じ
- 足元のベースラインがそろっている
- 512x512 内で切れていない
- 暗い単色背景で、透過処理しやすい
- 1枚だけ極端に寄り/引きになっていない
- `attack_3` が一番強く見える
- `attack_5` が `idle_1` に自然につながる
- 小さく表示しても何をしているか読める

## File Mapping

Storage キャラは per-character フォルダに保存する。
生成直後は PNG でもよいが、アプリは `.webp` のフレームURLを導出するため、最終成果物は WebP にする。

```text
backend/assets/master/source/characters/{code}/idle_1.webp
backend/assets/master/source/characters/{code}/idle_2.webp
backend/assets/master/source/characters/{code}/prep_1.webp
backend/assets/master/source/characters/{code}/prep_2.webp
backend/assets/master/source/characters/{code}/attack_1.webp
backend/assets/master/source/characters/{code}/attack_2.webp
backend/assets/master/source/characters/{code}/attack_3.webp
backend/assets/master/source/characters/{code}/attack_4.webp
backend/assets/master/source/characters/{code}/attack_5.webp
backend/assets/master/source/characters/{code}/rest_1.webp
backend/assets/master/source/characters/{code}/walk_1.webp
backend/assets/master/source/characters/{code}/walk_2.webp
backend/assets/master/source/characters/{code}/walk_3.webp
backend/assets/master/source/characters/{code}/walk_4.webp
```

アプリ同梱の標準プレイヤーは従来どおり `apps/mobile/assets/source/sprite_player_*`。

| # | 同梱ファイル名 | Storageファイル名 |
|---:|---|---|
| 1 | `sprite_player_idle_1.png` | `idle_1.webp` |
| 2 | `sprite_player_idle_2.png` | `idle_2.webp` |
| 3 | `sprite_player_prep_1.png` | `prep_1.webp` |
| 4 | `sprite_player_prep_2.png` | `prep_2.webp` |
| 5 | `sprite_player_attack_1.png` | `attack_1.webp` |
| 6 | `sprite_player_attack_2.png` | `attack_2.webp` |
| 7 | `sprite_player_attack_3.png` | `attack_3.webp` |
| 8 | `sprite_player_attack_4.png` | `attack_4.webp` |
| 9 | `sprite_player_attack_5.png` | `attack_5.webp` |
| 10 | `sprite_player_rest_1.png` | `rest_1.webp` |
| 11 | `sprite_player_walk_1.png` | `walk_1.webp` |
| 12 | `sprite_player_walk_2.png` | `walk_2.webp` |
| 13 | `sprite_player_walk_3.png` | `walk_3.webp` |
| 14 | `sprite_player_walk_4.png` | `walk_4.webp` |

## Post-Generation Commands

```bash
# 1) 必要なら 512x512 に縮小
sips -z 512 512 path/to/generated.png

# 2) 背景透過
#    バトル同梱スプライト
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind player

#    Storage キャラ（backend/assets/master/source/characters/{code}/...）
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind character

# 2.5) PNGで作業したStorageキャラをWebP化する例
cwebp -lossless -q 100 backend/assets/master/source/characters/00004/idle_1.png \
  -o backend/assets/master/source/characters/00004/idle_1.webp

# 3) Android/iOS へ同期（バトル同梱スプライトのみ）
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py

# 4) 検証
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

Storage キャラをアップロードする場合は、`master/characters/{code}/idle_1.webp` のように
per-character フォルダ構成を保つ。
