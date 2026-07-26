# Player Sprite Generation Prompts

All prompts use ChatGPT (DALL-E). Attach `sprite_player_idle_1.png` as reference image for consistency.

`[CHARACTER]` の部分を任意のキャラクター説明に置き換える。

**重要**: 全フレームでキャラクターの足元の高さと位置を揃えること。
基準画像（idle_1）の足元ラインに全フレームを合わせる。

## idle 1（基準画像）

```
Pixel art game sprite, 96x96 pixels.
Character description: [CHARACTER]
Pose: standing, idle battle-ready, facing right (looking rightward),
sword held at side.
Style: 16-bit retro JRPG, crisp pixel edges, no anti-aliasing, 32 colors max.
Solid dark background (will be removed later).
No text, no watermark, no UI elements.
```

---

**[CHARACTER] 記入例:**

```
female holy knight, long flowing red hair,
wearing elegant silver-white plate armor with gold trim and a crimson cape.
Holding a longsword in right hand, a small shield on left arm.
Expression calm and noble, gentle light glowing from armor.
```

```
male dark mage, hooded black robe with purple trim,
floating dark tome in left hand, wispy shadow energy.
Pale skin, glowing red eyes, silver hair peeking from hood.
```

## idle 2

```
Based on the attached character sprite.
Idle animation frame 2 of 2, gentle exhale.
Body sinks 1 pixel, chest slightly down. Arms relaxed, sword unchanged.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## prep 1

```
Based on the attached character sprite.
Prepare frame 1 of 2, gripping sword tighter.
Knees slightly bent, sword rising to shoulder level.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## prep 2

```
Based on the attached character sprite.
Prepare frame 2 of 2, full wind-up.
Sword pulled back behind right shoulder, body leaning forward.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 1

```
Based on the attached character sprite.
Attack frame 1 of 5, forward lunge step.
Right foot steps forward, sword beginning to swing from behind.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 2

```
Based on the attached character sprite.
Attack frame 2 of 5, sword mid-swing at 45° diagonal slash.
Body rotated forward, thin white arc line tracing sword path.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 3

```
Based on the attached character sprite.
Attack frame 3 of 5, peak impact. Sword slashing across center horizontally.
Bright slash effect line, dynamic action pose.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 4

```
Based on the attached character sprite.
Attack frame 4 of 5, follow-through. Sword extended fully left/down.
Cape flowing, body momentum carrying through.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 5

```
Based on the attached character sprite.
Attack frame 5 of 5, recovery. Sword lowering, body straightening.
Returning toward idle stance. Cape settling.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## rest 1

```
Based on the attached character sprite.
Rest pose, sitting on the ground cross-legged,
sword resting across lap, eyes closed, breathing deeply.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## walk 1

```
Based on the attached character sprite.
Walk frame 1 of 2, left foot stepping forward.
Sword held at side, slight forward lean, cape trailing lightly.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## walk 2

```
Based on the attached character sprite.
Walk frame 2 of 2, right foot stepping forward.
Sword held at side, body slightly lifted, cape swaying back.
96x96, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## File Mapping

| # | ファイル名 | 状態 |
|---|-----------|------|
| 1 | `sprite_player_idle_1.png` | idle 1 |
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
# 1. Background transparency
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind player

# 2. Sync individual frames to Android/iOS
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py

# 3. Create sprite sheet (6x2 grid, 96px cells, transparent bg)
montage \
  sprite_player_idle_1.png sprite_player_idle_2.png \
  sprite_player_prep_1.png sprite_player_prep_2.png \
  sprite_player_attack_1.png sprite_player_attack_2.png \
  sprite_player_attack_3.png sprite_player_attack_4.png \
  sprite_player_attack_5.png \
  sprite_player_walk_1.png sprite_player_walk_2.png \
  sprite_player_rest_1.png \
  -tile 6x2 -geometry 96x96+0+0 -background none \
  sprite_player_sheet.png

# 4. Copy sprite sheet to platform asset dirs
cp sprite_player_sheet.png ../../composeApp/src/androidMain/res/drawable-nodpi/
mkdir -p ../../iosApp/iosApp/Assets.xcassets/sprite_player_sheet.imageset
cp sprite_player_sheet.png ../../iosApp/iosApp/Assets.xcassets/sprite_player_sheet.imageset/
echo '{"images":[{"filename":"sprite_player_sheet.png","idiom":"universal"}]}' > ../../iosApp/iosApp/Assets.xcassets/sprite_player_sheet.imageset/Contents.json

# 5. Validate
./scripts/assets/run.sh scripts/assets/validate_assets.py
```

## Sprite Sheet Layout (6×2, 96px cells)

```
┌────────┬────────┬────────┬────────┬────────┬────────┐
│ idle_1 │ idle_2 │ prep_1 │ prep_2 │ atk_1  │ atk_2  │   row 0
├────────┼────────┼────────┼────────┼────────┼────────┤
│ atk_3  │ atk_4  │ atk_5  │ walk_1 │ walk_2 │ rest_1 │   row 1
└────────┴────────┴────────┴────────┴────────┴────────┘
   col 0    col 1    col 2    col 3    col 4    col 5
```

| Frame idx | File |
|-----------|------|
| 0 | idle_1 |
| 1 | idle_2 |
| 2 | prep_1 |
| 3 | prep_2 |
| 4 | atk_1 |
| 5 | atk_2 |
| 6 | atk_3 |
| 7 | atk_4 |
| 8 | atk_5 |
| 9 | walk_1 |
| 10 | walk_2 |
| 11 | rest_1 |
