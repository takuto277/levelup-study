#!/usr/bin/env python3
"""
manifest.yaml と実ファイル・seed slug 対応を検証する。

追加検証:
- OS 側に存在するが source/manifest に登録されていない PNG（orphan）の検出
- EnemySpriteAssets.kt bundledKeys と manifest enemy_sprite_keys の整合性

Usage:
  python3 scripts/assets/validate_assets.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ANDROID_DRAWABLE,
    ANDROID_DRAWABLE_NODPI,
    ANDROID_RES,
    ENEMY_SPRITE_KT,
    IOS_ASSETS,
    MANIFEST_PATH,
    SOURCE_DIR,
    collect_managed_filenames,
    collect_os_exceptions,
    enumerate_android_pngs,
    enumerate_ios_imagesets,
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
    managed_names = collect_managed_filenames(manifest)
    os_exceptions = collect_os_exceptions(manifest)

    # ---- source → sync 整合性 ----
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

    # ---- enemy sprite_keys の source → drawable-nodpi 整合性 ----
    for key_ in enemy_keys:
        name = f"sprite_enemy_{key_}_1.png"
        src = SOURCE_DIR / name
        if src.is_file():
            dest_nodpi = ANDROID_DRAWABLE_NODPI / name
            if not dest_nodpi.is_file():
                errors.append(f"Android drawable-nodpi missing (run sync): {name}")
            imageset = IOS_ASSETS / f"sprite_enemy_{key_}_1.imageset" / name
            if not imageset.is_file():
                errors.append(f"iOS imageset missing (run sync): {name}")

    # ---- OS 側 orphan PNG 検出（source/manifest 未登録、例外除く） ----
    # Android
    android_pngs = enumerate_android_pngs(ANDROID_RES)
    for rel_path in android_pngs:
        filename = Path(rel_path).name
        if filename in os_exceptions or rel_path in os_exceptions:
            continue
        parent = Path(rel_path).parent.as_posix()
        if parent in os_exceptions:
            continue
        if filename not in managed_names:
            errors.append(f"Orphan PNG on Android (not in source/manifest): {rel_path}")

    # iOS
    ios_imagesets = enumerate_ios_imagesets(IOS_ASSETS)
    for imageset_name, png_path in ios_imagesets.items():
        if imageset_name in os_exceptions:
            continue
        filename = png_path.name
        if filename not in managed_names:
            rel = png_path.relative_to(IOS_ASSETS).as_posix()
            errors.append(f"Orphan imageset on iOS (not in source/manifest): {rel}")

    # ---- seed slug 整合性 ----
    seed_slugs = parse_monster_slugs_from_seed()
    for slug in seed_slugs:
        if slug not in slug_map:
            errors.append(f"m_monsters.slug not mapped in manifest: {slug}")

    for slug, sprite_key in slug_map.items():
        if sprite_key not in enemy_keys:
            errors.append(
                f"monster_slug_to_sprite_key[{slug}] -> '{sprite_key}' not in enemy_sprite_keys"
            )

    # ---- EnemySpriteAssets.kt 整合性 ----
    if not ENEMY_SPRITE_KT.is_file():
        errors.append(f"Missing Kotlin file: {ENEMY_SPRITE_KT}")
    else:
        kt = ENEMY_SPRITE_KT.read_text(encoding="utf-8")

        # fallbackByLogical マッピング
        for slug, sprite_key in slug_map.items():
            needle = f'"{slug}" to "{sprite_key}"'
            if needle not in kt:
                errors.append(
                    f"EnemySpriteAssets fallback missing mapping (run generate): {slug} -> {sprite_key}"
                )

        # bundledKeys と manifest enemy_sprite_keys の整合性
        kt_bundled = _parse_bundled_keys(kt)
        if kt_bundled is not None:
            kt_set = set(kt_bundled)
            if kt_set != enemy_keys:
                only_kt = kt_set - enemy_keys
                only_manifest = enemy_keys - kt_set
                if only_kt:
                    errors.append(
                        f"EnemySpriteAssets bundledKeys has keys not in manifest: {sorted(only_kt)}"
                    )
                if only_manifest:
                    errors.append(
                        f"manifest enemy_sprite_keys has keys not in bundledKeys: {sorted(only_manifest)}"
                    )

    report(errors)
    return 1 if errors else 0


def _parse_bundled_keys(kt_source: str) -> list[str] | None:
    """EnemySpriteAssets.kt の bundledKeys に含まれるキー一覧をパースする。"""
    m = re.search(r"private val bundledKeys[\s\S]*?listOf\(([\s\S]*?)\)", kt_source)
    if not m:
        return None
    keys = re.findall(r'"([^"]+)"', m.group(1))
    return keys


def report(errors: list[str]) -> None:
    if errors:
        for e in errors:
            fail(e)
        print(f"\n{len(errors)} validation error(s).", file=sys.stderr)
    else:
        print("Asset validation passed.")


if __name__ == "__main__":
    raise SystemExit(main())
