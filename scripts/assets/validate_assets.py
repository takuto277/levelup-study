#!/usr/bin/env python3
"""
manifest.yaml と実ファイル・seed slug 対応を検証する。

Usage:
  python3 scripts/assets/validate_assets.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ANDROID_DRAWABLE,
    ENEMY_SPRITE_KT,
    IOS_ASSETS,
    MANIFEST_PATH,
    SOURCE_DIR,
    load_manifest,
    parse_monster_slugs_from_seed,
)


def fail(msg: str) -> None:
    print(f"ERROR: {msg}", file=sys.stderr)


def main() -> int:
    errors: list[str] = []

    if not MANIFEST_PATH.is_file():
        errors.append(f"Missing manifest: {MANIFEST_PATH}")
        report(errors)
        return 1

    manifest = load_manifest()
    slug_map: dict[str, str] = manifest.get("monster_slug_to_sprite_key") or {}
    enemy_keys: set[str] = set(manifest.get("enemy_sprite_keys") or [])

    for entry in manifest.get("battle_assets", []):
        filename = entry.get("file")
        required = entry.get("required", False)
        if not filename:
            errors.append("battle_assets entry missing 'file'")
            continue
        src = SOURCE_DIR / filename
        if required and not src.is_file():
            errors.append(f"Required source missing: {filename}")
        if src.is_file():
            if not (ANDROID_DRAWABLE / filename).is_file():
                errors.append(f"Android drawable missing (run sync): {filename}")
            imageset = IOS_ASSETS / f"{Path(filename).stem}.imageset" / filename
            if not imageset.is_file():
                errors.append(f"iOS imageset missing (run sync): {filename}")

    seed_slugs = parse_monster_slugs_from_seed()
    for slug in seed_slugs:
        if slug not in slug_map:
            errors.append(f"m_monsters.slug not mapped in manifest: {slug}")

    for slug, sprite_key in slug_map.items():
        if sprite_key not in enemy_keys:
            errors.append(
                f"monster_slug_to_sprite_key[{slug}] -> '{sprite_key}' not in enemy_sprite_keys"
            )

    if not ENEMY_SPRITE_KT.is_file():
        errors.append(f"Missing Kotlin file: {ENEMY_SPRITE_KT}")
    else:
        kt = ENEMY_SPRITE_KT.read_text(encoding="utf-8")
        for slug, sprite_key in slug_map.items():
            needle = f'"{slug}" to "{sprite_key}"'
            if needle not in kt:
                errors.append(
                    f"EnemySpriteAssets fallback missing mapping (run generate): {slug} -> {sprite_key}"
                )

    report(errors)
    return 1 if errors else 0


def report(errors: list[str]) -> None:
    if errors:
        for e in errors:
            fail(e)
        print(f"\n{len(errors)} validation error(s).", file=sys.stderr)
    else:
        print("Asset validation passed.")


if __name__ == "__main__":
    raise SystemExit(main())
