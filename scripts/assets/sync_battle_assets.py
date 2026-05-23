#!/usr/bin/env python3
"""
apps/mobile/assets/source/ の PNG を Android drawable と iOS Assets.xcassets に同期する。

Usage:
  python3 scripts/assets/sync_battle_assets.py
  python3 scripts/assets/sync_battle_assets.py --check  # 差分があると exit 1（CI 用）
"""

from __future__ import annotations

import argparse
import filecmp
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ANDROID_DRAWABLE,
    IOS_ASSETS,
    IOS_CONTENTS_JSON,
    SOURCE_DIR,
    load_manifest,
)


def ios_imageset_dir(filename: str) -> Path:
    stem = Path(filename).stem
    return IOS_ASSETS / f"{stem}.imageset"


def sync_ios(filename: str, dry_check: bool) -> bool:
    src = SOURCE_DIR / filename
    if not src.is_file():
        return False

    imageset = ios_imageset_dir(filename)
    dest_png = imageset / filename
    contents = imageset / "Contents.json"
    expected_contents = IOS_CONTENTS_JSON % {"filename": filename}

    changed = False
    if not dest_png.is_file() or not filecmp.cmp(src, dest_png, shallow=False):
        changed = True
        if not dry_check:
            imageset.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest_png)

    if not contents.is_file() or contents.read_text(encoding="utf-8") != expected_contents:
        changed = True
        if not dry_check:
            imageset.mkdir(parents=True, exist_ok=True)
            contents.write_text(expected_contents, encoding="utf-8")

    return changed


def sync_android(filename: str, dry_check: bool) -> bool:
    src = SOURCE_DIR / filename
    if not src.is_file():
        return False

    dest = ANDROID_DRAWABLE / filename
    if dest.is_file() and filecmp.cmp(src, dest, shallow=False):
        return False

    if not dry_check:
        ANDROID_DRAWABLE.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
    return True


def collect_source_files(manifest: dict) -> list[str]:
    files: list[str] = []
    for entry in manifest.get("battle_assets", []):
        name = entry.get("file")
        if name and (SOURCE_DIR / name).is_file():
            files.append(name)

    for key in manifest.get("enemy_sprite_keys", []):
        name = f"sprite_enemy_{key}_1.png"
        if (SOURCE_DIR / name).is_file():
            files.append(name)

    return sorted(set(files))


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync battle assets to Android/iOS")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Do not write; exit 1 if platforms are out of sync",
    )
    args = parser.parse_args()
    dry_check = args.check

    manifest = load_manifest()
    files = collect_source_files(manifest)
    if not files:
        print("No PNG files in source/ to sync.", file=sys.stderr)
        return 1

    any_changed = False
    for filename in files:
        if sync_android(filename, dry_check):
            any_changed = True
            action = "would sync" if dry_check else "synced"
            print(f"Android: {action} {filename}")
        if sync_ios(filename, dry_check):
            any_changed = True
            action = "would sync" if dry_check else "synced"
            print(f"iOS: {action} {filename}")

    if dry_check and any_changed:
        print("Platforms are out of sync with source/. Run sync_battle_assets.py without --check.", file=sys.stderr)
        return 1

    print(f"Done. {len(files)} file(s) processed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
