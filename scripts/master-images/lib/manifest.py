"""Supabase master image manifest and env helpers."""

from __future__ import annotations

import functools
import os
import re
from pathlib import Path
from typing import Any

import yaml

REPO_ROOT = Path(__file__).resolve().parents[3]
MASTER_DIR = REPO_ROOT / "backend/assets/master"
MANIFEST_PATH = MASTER_DIR / "manifest.yaml"
SEED_SQL = REPO_ROOT / "backend/db/seed.sql"

UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    re.I,
)

ENTITY_TABLE = {
    "characters": "m_characters",
    "weapons": "m_weapons",
    "dungeons": "m_dungeons",
    "monsters": "m_monsters",
}


@functools.lru_cache(maxsize=1)
def load_manifest() -> dict[str, Any]:
    with MANIFEST_PATH.open(encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}
    if not isinstance(data, dict):
        raise ValueError(f"Invalid manifest: {MANIFEST_PATH}")
    return data


def require_env(name: str) -> str:
    val = os.environ.get(name, "").strip()
    if not val:
        raise RuntimeError(f"環境変数 {name} が未設定です（backend/.env を参照）")
    return val


def supabase_public_url(supabase_url: str, bucket: str, object_key: str) -> str:
    base = supabase_url.rstrip("/")
    return f"{base}/storage/v1/object/public/{bucket}/{object_key}"


def storage_object_key(entity: str, asset_id: str, ext: str) -> str:
    return f"master/{entity}/{asset_id}.{ext.lstrip('.')}"


def file_extension(path: Path) -> str:
    ext = path.suffix.lower().lstrip(".")
    if ext not in {"png", "webp", "jpg", "jpeg"}:
        raise ValueError(f"Unsupported image extension: {path}")
    return "jpg" if ext == "jpeg" else ext


def parse_seed_ids(table: str) -> set[str]:
    ids: set[str] = set()
    in_block = False
    for line in SEED_SQL.read_text(encoding="utf-8").splitlines():
        if f"INSERT INTO {table}" in line:
            in_block = True
            continue
        if in_block:
            if line.strip().startswith("--") or not line.strip():
                continue
            if not line.lstrip().startswith("("):
                break
            parts = line.split("'")
            if len(parts) >= 2:
                ids.add(parts[1])
    return ids
