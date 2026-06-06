#!/usr/bin/env python3
"""
apps/mobile/assets/source/ の PNG を Android drawable/drawable-nodpi と
iOS Assets.xcassets に同期する。

Usage:
  python3 scripts/assets/sync_battle_assets.py
  python3 scripts/assets/sync_battle_assets.py --check   # 差分があると exit 1（CI 用）
  python3 scripts/assets/sync_battle_assets.py --clean   # 管理対象外の OS 側 PNG を削除
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
    ANDROID_DRAWABLE_NODPI,
    IOS_ASSETS,
    IOS_CONTENTS_JSON,
    SOURCE_DIR,
    collect_managed_filenames,
    collect_os_exceptions,
    load_manifest,
)


def ios_imageset_dir(filename: str) -> Path:
    stem = Path(filename).stem
    return IOS_ASSETS / f"{stem}.imageset"


def _android_dest(filename: str) -> Path:
    """敵スプライトは drawable-nodpi、それ以外は drawable に配置。"""
    if filename.startswith("sprite_enemy_"):
        return ANDROID_DRAWABLE_NODPI / filename
    return ANDROID_DRAWABLE / filename


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

    dest = _android_dest(filename)
    if dest.is_file() and filecmp.cmp(src, dest, shallow=False):
        return False

    if not dry_check:
        dest.parent.mkdir(parents=True, exist_ok=True)
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


def clean_os_orphans(manifest: dict, dry_check: bool) -> list[str]:
    """source/manifest に登録のない OS 側 PNG を列挙／削除する。例外は除外。"""
    managed_names = collect_managed_filenames(manifest)
    os_exceptions = collect_os_exceptions(manifest)
    removed: list[str] = []

    # Android drawable + drawable-nodpi
    for d in (ANDROID_DRAWABLE, ANDROID_DRAWABLE_NODPI):
        if not d.is_dir():
            continue
        for p in sorted(d.glob("*.png")):
            if p.name in os_exceptions:
                continue
            if p.name not in managed_names:
                removed.append(str(p.relative_to(d.parents[2])))  # relative to res/
                if not dry_check:
                    p.unlink()

    # iOS imageset
    if IOS_ASSETS.is_dir():
        for contents_json in sorted(IOS_ASSETS.rglob("*.imageset/Contents.json")):
            imageset = contents_json.parent
            if imageset.name in os_exceptions:
                continue
            for png in imageset.glob("*.png"):
                if png.name in os_exceptions:
                    continue
                if png.name not in managed_names:
                    removed.append(str(png.relative_to(IOS_ASSETS)))
                    if not dry_check:
                        shutil.rmtree(imageset, ignore_errors=True)
                    break

    return removed


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync battle assets to Android/iOS")
    parser.add_argument(
        "--check",
        action="store_true",
        help="Do not write; exit 1 if platforms are out of sync",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Remove OS-side PNGs that are not in source/manifest (excludes os_specific_assets)",
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
            dest = _android_dest(filename)
            print(f"Android: {action} {dest.relative_to(dest.parents[2])}")
        if sync_ios(filename, dry_check):
            any_changed = True
            action = "would sync" if dry_check else "synced"
            print(f"iOS: {action} {filename}")

    if args.clean:
        removed = clean_os_orphans(manifest, dry_check)
        if removed:
            action = "would remove" if dry_check else "removed"
            for r in removed:
                print(f"Clean: {action} {r}")
            any_changed = True

    if dry_check and any_changed:
        print(
            "Platforms are out of sync with source/. Run sync_battle_assets.py without --check.",
            file=sys.stderr,
        )
        return 1

    print(f"Done. {len(files)} file(s) processed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
