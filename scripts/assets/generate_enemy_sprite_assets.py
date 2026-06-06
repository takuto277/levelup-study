#!/usr/bin/env python3
"""
manifest.yaml の enemy_sprite_keys と monster_slug_to_sprite_key から
EnemySpriteAssets.kt の bundledKeys と fallbackByLogical を生成する。

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

BUNDLED_START = "    private val bundledKeys: Set<String> = buildSet {"
BUNDLED_END = "    }"
MAP_START = "    private val fallbackByLogical: Map<String, String> = mapOf("
MAP_END = "    )"


def build_bundled_block(keys: list[str]) -> str:
    lines = [BUNDLED_START]
    lines.append("        addAll(")
    lines.append("            listOf(")
    for i, key in enumerate(sorted(keys)):
        comma = "," if i < len(keys) - 1 else ""
        lines.append(f'                "{key}"{comma}')
    lines.append("            )")
    lines.append("        )")
    lines.append(BUNDLED_END)
    return "\n".join(lines)


def build_map_block(slug_map: dict[str, str]) -> str:
    lines = [MAP_START]
    for slug in sorted(slug_map.keys()):
        sprite = slug_map[slug]
        lines.append(f'        "{slug}" to "{sprite}",')
    lines.append(MAP_END)
    return "\n".join(lines)


def replace_block(content: str, pattern: str, new_block: str, label: str) -> str:
    compiled = re.compile(pattern, re.MULTILINE)
    if not compiled.search(content):
        raise ValueError(f"Could not find {label} in EnemySpriteAssets.kt")
    return compiled.sub(new_block, content, count=1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="Exit 1 if Kotlin would change")
    args = parser.parse_args()

    manifest = load_manifest()
    enemy_keys: list[str] = manifest.get("enemy_sprite_keys") or []
    slug_map: dict[str, str] = manifest.get("monster_slug_to_sprite_key") or {}

    if not enemy_keys:
        print("No enemy_sprite_keys in manifest.", file=sys.stderr)
        return 1

    new_bundled = build_bundled_block(enemy_keys)
    new_map = build_map_block(slug_map)

    original = ENEMY_SPRITE_KT.read_text(encoding="utf-8")

    updated = replace_block(
        original,
        r"    private val bundledKeys: Set<String> = buildSet \{[\s\S]*?\n    \}",
        new_bundled,
        "bundledKeys",
    )
    updated = replace_block(
        updated,
        r"    private val fallbackByLogical: Map<String, String> = (?:mapOf\([\s\S]*?\)|emptyMap\(\))",
        new_map,
        "fallbackByLogical",
    )

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
