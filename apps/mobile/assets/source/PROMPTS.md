# Player Sprite Generation Prompts

All prompts use ChatGPT (DALL-E). Attach `sprite_player_idle_1.png` as reference image for consistency.

`[CHARACTER]` の部分を任意のキャラクター説明に置き換える。

**重要**: 全フレームでキャラクターの足元の高さと位置を揃えること。
基準画像（idle_1）の足元ラインに全フレームを合わせる。

## idle 1（基準画像）

```
Pixel art game sprite, 512x512 pixels.
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
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## idle 2（吸う）

```
Based on the attached character sprite.
Gentle inhale. Chest rises 1 pixel, shoulders lift slightly.
Hair tips and cape edge move 1 pixel up.
Feet, waist, sword angle — completely unchanged.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## idle 3（吐く）

```
Based on the attached character sprite.
Gentle exhale. Chest sinks 1 pixel, arms relax slightly.
Hair tips and cape edge move 1 pixel down.
Feet, waist, sword angle — completely unchanged.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## idle 4（戻り）

```
Based on the attached character sprite.
Transition toward neutral. Chest midway, shoulders settling.
Hair and cape between exhale and idle_1 position.
Feet, waist, sword angle — completely unchanged.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## prep 1

```
Based on the attached character sprite.
Prepare frame 1 of 2, gripping sword tighter.
Knees slightly bent, sword rising to shoulder level.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## prep 2

```
Based on the attached character sprite.
Prepare frame 2 of 2, full wind-up.
Sword pulled back behind right shoulder, body leaning forward.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 1

```
Based on the attached character sprite.
Attack frame 1 of 5, forward lunge step.
Right foot steps forward, sword beginning to swing from behind.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 2

```
Based on the attached character sprite.
Attack frame 2 of 5, sword mid-swing at 45° diagonal slash.
Body rotated forward, thin white arc line tracing sword path.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 3

```
Based on the attached character sprite.
Attack frame 3 of 5, peak impact. Sword slashing across center horizontally.
Bright slash effect line, dynamic action pose.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 4

```
Based on the attached character sprite.
Attack frame 4 of 5, follow-through. Sword extended fully left/down.
Cape flowing, body momentum carrying through.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## attack 5

```
Based on the attached character sprite.
Attack frame 5 of 5, recovery. Sword lowering, body straightening.
Returning toward idle stance. Cape settling.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## rest 1

```
Based on the attached character sprite.
Rest pose, sitting on the ground cross-legged,
sword resting across lap, eyes closed, breathing deeply.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## walk 1

```
Based on the attached character sprite.
Walk frame 1 of 2, left foot stepping forward.
Sword held at side, slight forward lean, cape trailing lightly.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## walk 2

```
Based on the attached character sprite.
Walk frame 2 of 2, right foot stepping forward.
Sword held at side, body slightly lifted, cape swaying back.
512x512, same pixel art style and colors, facing right, feet at same ground level as reference. Dark background. No text.
```

## File Mapping

| # | ファイル名 | 状態 |
|---|-----------|------|
| 1 | `sprite_player_idle_1.png` | idle 1 |
| 2 | `sprite_player_idle_2.png` | idle 2 |
| 3 | `sprite_player_idle_3.png` | idle 3 |
| 4 | `sprite_player_idle_4.png` | idle 4 |
| 5 | `sprite_player_prep_1.png` | prep 1 |
| 6 | `sprite_player_prep_2.png` | prep 2 |
| 7 | `sprite_player_attack_1.png` | attack 1 |
| 8 | `sprite_player_attack_2.png` | attack 2 |
| 9 | `sprite_player_attack_3.png` | attack 3 |
| 10 | `sprite_player_attack_4.png` | attack 4 |
| 11 | `sprite_player_attack_5.png` | attack 5 |
| 12 | `sprite_player_walk_1.png` | walk 1 |
| 13 | `sprite_player_walk_2.png` | walk 2 |
| 14 | `sprite_player_rest_1.png` | rest 1 |

## Sprite Sheet Layout (6×3, 512px cells, 3072×1536px)

```
┌────────┬────────┬────────┬────────┬────────┬────────┐
│ idle_1 │ idle_2 │ idle_3 │ idle_4 │ prep_1 │ prep_2 │ row 0
├────────┼────────┼────────┼────────┼────────┼────────┤
│ atk_1  │ atk_2  │ atk_3  │ atk_4  │ atk_5  │ walk_1 │ row 1
├────────┼────────┼────────┼────────┼────────┼────────┤
│ walk_2 │ rest_1 │        │        │        │        │ row 2
└────────┴────────┴────────┴────────┴────────┴────────┘
```

| Frame idx | File |
|-----------|------|
| 0 | idle_1 |
| 1 | idle_2 |
| 2 | idle_3 |
| 3 | idle_4 |
| 4 | prep_1 |
| 5 | prep_2 |
| 6 | atk_1 |
| 7 | atk_2 |
| 8 | atk_3 |
| 9 | atk_4 |
| 10 | atk_5 |
| 11 | walk_1 |
| 12 | walk_2 |
| 13 | rest_1 |
