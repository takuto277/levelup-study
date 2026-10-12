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
    IMAGE_EXTS,
    IOS_ASSETS,
    IOS_CONTENTS_JSON,
    IOS_SPRITES_DIR,
    SOURCE_DIR,
    SPRITE_NAME_PREFIXES,
    REPO_ROOT,
    collect_managed_stems,
    collect_os_exceptions,
    load_manifest,
    resolve_source_file,
)


def _is_sprite_stem(stem: str) -> bool:
    return stem.startswith(SPRITE_NAME_PREFIXES)


def _remove_stale_siblings(dest_dir: Path, filename: str) -> None:
    """同じ stem で拡張子が違う古い画像（例: .png 残骸）を削除する。"""
    stem = Path(filename).stem
    for ext in IMAGE_EXTS:
        cand = dest_dir / f"{stem}{ext}"
        if cand.name != filename and cand.exists():
            cand.unlink()


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

    stem = Path(filename).stem

    # スプライトは WebP を loose resource として配置（Asset Catalog は WebP 非対応）
    if _is_sprite_stem(stem):
        changed = False
        dest = IOS_SPRITES_DIR / filename
        if not dest.is_file() or not filecmp.cmp(src, dest, shallow=False):
            changed = True
            if not dry_check:
                IOS_SPRITES_DIR.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dest)
                _remove_stale_siblings(IOS_SPRITES_DIR, filename)
        # 旧 Asset Catalog の imageset を掃除
        legacy = ios_imageset_dir(filename)
        if legacy.exists():
            changed = True
            if not dry_check:
                shutil.rmtree(legacy, ignore_errors=True)
        return changed

    # それ以外（bg/prop/banner など）は従来どおり imageset
    imageset = ios_imageset_dir(filename)
    dest = imageset / filename
    contents = imageset / "Contents.json"
    expected_contents = IOS_CONTENTS_JSON % {"filename": filename}

    changed = False
    if not dest.is_file() or not filecmp.cmp(src, dest, shallow=False):
        changed = True
        if not dry_check:
            imageset.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, dest)
            _remove_stale_siblings(imageset, filename)

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
        _remove_stale_siblings(dest.parent, filename)
    return True


def collect_source_files(manifest: dict) -> list[str]:
    files: list[str] = []
    for entry in manifest.get("battle_assets", []):
        name = entry.get("file")
        if not name:
            continue
        resolved = resolve_source_file(name)
        if resolved is not None:
            files.append(resolved.name)

    for key in manifest.get("enemy_sprite_keys", []):
        resolved = resolve_source_file(f"sprite_enemy_{key}_1.png")
        if resolved is not None:
            files.append(resolved.name)

    return sorted(set(files))


def clean_os_orphans(manifest: dict, dry_check: bool) -> list[str]:
    """source/manifest に登録のない OS 側画像を列挙／削除する。例外は除外。"""
    managed_stems = collect_managed_stems(manifest)
    os_exceptions = collect_os_exceptions(manifest)
    removed: list[str] = []

    # Android drawable + drawable-nodpi
    for d in (ANDROID_DRAWABLE, ANDROID_DRAWABLE_NODPI):
        if not d.is_dir():
            continue
        for p in sorted(d.iterdir()):
            if p.suffix.lower() not in IMAGE_EXTS:
                continue
            if p.name in os_exceptions or p.stem in os_exceptions:
                continue
            if p.stem not in managed_stems:
                removed.append(str(p.relative_to(d.parents[2])))  # relative to res/
                if not dry_check:
                    p.unlink()

    # iOS loose sprite resources
    if IOS_SPRITES_DIR.is_dir():
        for p in sorted(IOS_SPRITES_DIR.iterdir()):
            if p.suffix.lower() not in IMAGE_EXTS:
                continue
            if p.name in os_exceptions or p.stem in os_exceptions:
                continue
            if p.stem not in managed_stems:
                removed.append(str(p.relative_to(REPO_ROOT)))
                if not dry_check:
                    p.unlink()

    # iOS imageset（スプライトは loose resource へ移行済み）
    if IOS_ASSETS.is_dir():
        for contents_json in sorted(IOS_ASSETS.rglob("*.imageset/Contents.json")):
            imageset = contents_json.parent
            if imageset.name in os_exceptions:
                continue
            stem = imageset.name[: -len(".imageset")]
            if _is_sprite_stem(stem):
                removed.append(str(imageset.relative_to(IOS_ASSETS)))
                if not dry_check:
                    shutil.rmtree(imageset, ignore_errors=True)
                continue
            for img in sorted(imageset.iterdir()):
                if img.suffix.lower() not in IMAGE_EXTS:
                    continue
                if img.name in os_exceptions or img.stem in os_exceptions:
                    continue
                if img.stem not in managed_stems:
                    removed.append(str(img.relative_to(IOS_ASSETS)))
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
