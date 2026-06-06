#!/usr/bin/env python3
"""Load apps/mobile/assets/manifest.yaml."""

from __future__ import annotations

import functools
from pathlib import Path
from typing import Any

import yaml

REPO_ROOT = Path(__file__).resolve().parents[3]
MANIFEST_PATH = REPO_ROOT / "apps/mobile/assets/manifest.yaml"
SOURCE_DIR = REPO_ROOT / "apps/mobile/assets/source"
ANDROID_DRAWABLE = REPO_ROOT / "apps/mobile/composeApp/src/androidMain/res/drawable"
ANDROID_DRAWABLE_NODPI = REPO_ROOT / "apps/mobile/composeApp/src/androidMain/res/drawable-nodpi"
ANDROID_RES = REPO_ROOT / "apps/mobile/composeApp/src/androidMain/res"
IOS_ASSETS = REPO_ROOT / "apps/mobile/iosApp/iosApp/Assets.xcassets"
SEED_SQL = REPO_ROOT / "backend/db/seed.sql"
ENEMY_SPRITE_KT = (
    REPO_ROOT
    / "apps/mobile/shared/src/commonMain/kotlin/org/example/project/features/study/EnemySpriteAssets.kt"
)

IOS_CONTENTS_JSON = """{
  "images": [
    { "filename": "%(filename)s", "idiom": "universal", "scale": "1x" },
    { "idiom": "universal", "scale": "2x" },
    { "idiom": "universal", "scale": "3x" }
  ],
  "info": { "author": "xcode", "version": 1 }
}
"""


@functools.lru_cache(maxsize=1)
def load_manifest() -> dict[str, Any]:
    with MANIFEST_PATH.open(encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        raise ValueError(f"Invalid manifest: {MANIFEST_PATH}")
    return data


def parse_monster_slugs_from_seed() -> list[str]:
    slugs: list[str] = []
    in_monsters = False
    for line in SEED_SQL.read_text(encoding="utf-8").splitlines():
        if "INSERT INTO m_monsters" in line:
            in_monsters = True
            continue
        if in_monsters:
            if line.strip().startswith("--") or not line.strip():
                continue
            if not line.lstrip().startswith("("):
                break
            # ('uuid', 'slug', ...
            parts = line.split("'")
            if len(parts) >= 4:
                slugs.append(parts[3])
    return slugs


def collect_managed_filenames(manifest: dict) -> set[str]:
    """manifest から管理対象のファイル名一覧を収集する。"""
    names: set[str] = set()
    for entry in manifest.get("battle_assets", []):
        name = entry.get("file")
        if name:
            names.add(name)
    for key_ in manifest.get("enemy_sprite_keys", []):
        names.add(f"sprite_enemy_{key_}_1.png")
    return names


def collect_os_exceptions(manifest: dict) -> set[str]:
    """os_specific_assets から例外ファイル名／ディレクトリ名を収集する。"""
    exceptions: set[str] = set()
    os_specific = manifest.get("os_specific_assets") or {}
    for platform in ("android", "ios"):
        for name in os_specific.get(platform, []):
            exceptions.add(name)
    return exceptions


def enumerate_android_pngs(base_dir: Path) -> dict[str, Path]:
    """Android res/ 以下の全 PNG ファイルを {相対パス: 絶対パス} で返す。"""
    result: dict[str, Path] = {}
    for p in base_dir.rglob("*.png"):
        result[p.relative_to(base_dir).as_posix()] = p
    return result


def enumerate_ios_imagesets(assets_dir: Path) -> dict[str, Path]:
    """iOS Assets.xcassets 以下の全 imageset を {imageset名: PNGパス} で返す。"""
    result: dict[str, Path] = {}
    for contents_json in assets_dir.rglob("*.imageset/Contents.json"):
        imageset = contents_json.parent
        for png in imageset.glob("*.png"):
            result[imageset.name] = png
            break  # 最初の PNG のみ
    return result
