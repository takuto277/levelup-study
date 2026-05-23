#!/usr/bin/env python3
"""
manifest.yaml の monster_slug_to_sprite_key から EnemySpriteAssets.kt の fallbackByLogical を生成する。

Usage:
  python3 scripts/assets/generate_enemy_sprite_assets.py
  python3 scripts/assets/generate_enemy_sprite_assets.py --check
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import ENEMY_SPRITE_KT, load_manifest  # noqa: E402

MAP_START = "    private val fallbackByLogical: Map<String, String> = mapOf("
MAP_END = "    )"


def build_map_block(slug_map: dict[str, str]) -> str:
    lines = [MAP_START]
    for slug in sorted(slug_map.keys()):
        sprite = slug_map[slug]
        lines.append(f'        "{slug}" to "{sprite}",')
    lines.append(MAP_END)
    return "\n".join(lines)


def replace_fallback_map(content: str, new_block: str) -> str:
    pattern = re.compile(
        r"    private val fallbackByLogical: Map<String, String> = (?:mapOf\([\s\S]*?\)|emptyMap\(\))",
        re.MULTILINE,
    )
    if not pattern.search(content):
        raise ValueError("Could not find fallbackByLogical in EnemySpriteAssets.kt")
    return pattern.sub(new_block, content, count=1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Exit 1 if Kotlin would change")
    args = parser.parse_args()

    manifest = load_manifest()
    slug_map: dict[str, str] = manifest.get("monster_slug_to_sprite_key") or {}
    new_block = build_map_block(slug_map)

    original = ENEMY_SPRITE_KT.read_text(encoding="utf-8")
    updated = replace_fallback_map(original, new_block)

    if args.check:
        if updated != original:
            print("EnemySpriteAssets.kt is out of date. Run generate_enemy_sprite_assets.py", file=sys.stderr)
            return 1
        print("EnemySpriteAssets.kt is up to date.")
        return 0

    if updated != original:
        ENEMY_SPRITE_KT.write_text(updated, encoding="utf-8")
        print(f"Updated {ENEMY_SPRITE_KT}")
    else:
        print("No changes needed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
