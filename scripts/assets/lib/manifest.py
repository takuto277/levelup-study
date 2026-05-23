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
