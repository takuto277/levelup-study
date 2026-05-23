#!/usr/bin/env python3
"""Validate backend/assets/master/manifest.yaml and source files."""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ENTITY_TABLE,
    MANIFEST_PATH,
    MASTER_DIR,
    UUID_RE,
    load_manifest,
    parse_seed_ids,
)


def main() -> int:
    errors: list[str] = []

    if not MANIFEST_PATH.is_file():
        errors.append(f"Missing manifest: {MANIFEST_PATH}")
        report(errors)
        return 1

    manifest = load_manifest()
    entities = manifest.get("entities") or {}
    assets = manifest.get("assets") or []

    if not manifest.get("bucket"):
        errors.append("manifest.bucket is required")

    for entry in assets:
        entity = entry.get("entity")
        asset_id = entry.get("id")
        rel_file = entry.get("file")

        if entity not in ENTITY_TABLE:
            errors.append(f"Unknown entity: {entity}")
            continue
        if not asset_id or not UUID_RE.match(str(asset_id)):
            errors.append(f"Invalid UUID for entity={entity}: {asset_id}")
            continue
        if not rel_file:
            errors.append(f"Missing file for {entity}/{asset_id}")
            continue

        src = MASTER_DIR / rel_file
        if not src.is_file():
            errors.append(f"Source file missing: {rel_file}")

        seed_ids = parse_seed_ids(ENTITY_TABLE[entity])
        if str(asset_id) not in seed_ids:
            errors.append(
                f"ID not in seed.sql {ENTITY_TABLE[entity]}: {asset_id} "
                "(seed 追加後に manifest を更新してください)"
            )

    report(errors)
    if errors:
        return 1
    if not assets:
        print("Manifest OK (assets エントリは 0 件 — PNG 追加後に manifest を更新)")
    else:
        print(f"Manifest OK ({len(assets)} asset(s)).")
    return 0


def report(errors: list[str]) -> None:
    for e in errors:
        print(f"ERROR: {e}", file=sys.stderr)
    if errors:
        print(f"\n{len(errors)} validation error(s).", file=sys.stderr)


if __name__ == "__main__":
    raise SystemExit(main())
