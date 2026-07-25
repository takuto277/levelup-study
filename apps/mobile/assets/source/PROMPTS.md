# Player Sprite Generation Prompts

All prompts use ChatGPT (DALL-E). Attach `sprite_player_idle_1.png` as reference image for consistency.

## idle 1（基準画像）

```
Pixel art game sprite, 96x96 pixels.
Character description: female holy knight, long flowing red hair,
wearing elegant silver-white plate armor with gold trim and a crimson cape.
Holding a longsword in right hand, a small shield on left arm.
Expression calm and noble, gentle light glowing from armor.
Pose: standing, idle battle-ready, facing right (looking rightward),
sword held at side.
Style: 16-bit retro JRPG, crisp pixel edges, no anti-aliasing, 32 colors max.
Solid dark background (will be removed later).
No text, no watermark, no UI elements.
```

## idle 2

```
Based on the attached character sprite.
Idle animation frame 2 of 2, gentle exhale.
Body sinks 1 pixel, chest slightly down. Arms relaxed, sword unchanged.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## prep 1

```
Based on the attached character sprite.
Prepare frame 1 of 2, gripping sword tighter.
Knees slightly bent, sword rising to shoulder level.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## prep 2

```
Based on the attached character sprite.
Prepare frame 2 of 2, full wind-up.
Sword pulled back behind right shoulder, body leaning forward.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## attack 1

```
Based on the attached character sprite.
Attack frame 1 of 5, forward lunge step.
Right foot steps forward, sword beginning to swing from behind.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## attack 2

```
Based on the attached character sprite.
Attack frame 2 of 5, sword mid-swing at 45° diagonal slash.
Body rotated forward, thin white arc line tracing sword path.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## attack 3

```
Based on the attached character sprite.
Attack frame 3 of 5, peak impact. Sword slashing across center horizontally.
Bright slash effect line, dynamic action pose.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## attack 4

```
Based on the attached character sprite.
Attack frame 4 of 5, follow-through. Sword extended fully left/down.
Cape flowing, body momentum carrying through.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## attack 5

```
Based on the attached character sprite.
Attack frame 5 of 5, recovery. Sword lowering, body straightening.
Returning toward idle stance. Cape settling.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## rest 1

```
Based on the attached character sprite.
Rest pose, sword tip resting on ground. Both hands on sword pommel.
Slightly hunched, catching breath after battle.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## walk 1

```
Based on the attached character sprite.
Walk frame 1 of 2, left foot stepping forward.
Sword held at side, slight forward lean, cape trailing lightly.
96x96, same pixel art style and colors, facing right. Dark background. No text.
```

## walk 2

```
Based on the attached character sprite.
Walk frame 2 of 2, right foot stepping forward.
Sword held at side, body slightly lifted, cape swaying back.
96x96, same pixel art style and colors, facing right. Dark background. No text.
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
# Background transparency
./scripts/assets/run.sh scripts/assets/make_player_sprites_transparent.py --kind player

# Sync to Android/iOS
./scripts/assets/run.sh scripts/assets/sync_battle_assets.py

# Validate
./scripts/assets/run.sh scripts/assets/validate_assets.py
```
