#!/usr/bin/env python3
"""
Supabase Storage にマスタ画像をアップロードし、PostgreSQL の image_url を更新する。

Usage:
  # backend/.env を読み込んだうえで:
  make master-images-upload DRY_RUN=1
  make master-images-upload

環境変数:
  SUPABASE_URL                  https://xxxx.supabase.co
  SUPABASE_SERVICE_ROLE_KEY     Settings > API（サーバー専用・モバイルに埋め込まない）
  SUPABASE_STORAGE_BUCKET       省略時 manifest.bucket（既定 game-assets）
  DATABASE_URL                  postgres://... （Supabase 接続文字列）
  DRY_RUN=1                     アップロード・UPDATE を行わず表示のみ
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

import psycopg2
import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ENTITY_TABLE,
    MASTER_DIR,
    file_extension,
    load_manifest,
    require_env,
    storage_object_key,
    supabase_public_url,
)


def upload_file(
    supabase_url: str,
    service_key: str,
    bucket: str,
    object_key: str,
    file_path: Path,
    content_type: str,
    dry_run: bool,
) -> None:
    url = f"{supabase_url.rstrip('/')}/storage/v1/object/{bucket}/{object_key}"
    if dry_run:
        print(f"  [dry-run] PUT {url}")
        return

    with file_path.open("rb") as f:
        resp = requests.post(
            url,
            data=f.read(),
            headers={
                "Authorization": f"Bearer {service_key}",
                "Content-Type": content_type,
                "x-upsert": "true",
            },
            timeout=120,
        )
    if resp.status_code not in (200, 201):
        raise RuntimeError(f"Upload failed ({resp.status_code}): {resp.text}")


def update_image_url(
    database_url: str,
    table: str,
    asset_id: str,
    public_url: str,
    dry_run: bool,
) -> None:
    sql = f"UPDATE {table} SET image_url = %s WHERE id = %s"
    if dry_run:
        print(f"  [dry-run] {sql}  -- {public_url!r}, {asset_id}")
        return

    conn = psycopg2.connect(database_url)
    try:
        with conn.cursor() as cur:
            cur.execute(sql, (public_url, asset_id))
        conn.commit()
    finally:
        conn.close()


def content_type_for(ext: str) -> str:
    return {
        "png": "image/png",
        "webp": "image/webp",
        "jpg": "image/jpeg",
    }.get(ext, "application/octet-stream")


def main() -> int:
    dry_run = os.environ.get("DRY_RUN", "").lower() in {"1", "true", "yes"}

    manifest = load_manifest()
    assets = manifest.get("assets") or []
    if not assets:
        print("manifest.assets が空です。PNG を source/ に置き manifest を更新してください。")
        return 1

    supabase_url = require_env("SUPABASE_URL")
    service_key = require_env("SUPABASE_SERVICE_ROLE_KEY")
    database_url = require_env("DATABASE_URL")
    bucket = os.environ.get("SUPABASE_STORAGE_BUCKET", manifest.get("bucket", "game-assets"))

    if database_url.startswith("host="):
        print(
            "ERROR: DATABASE_URL は postgres:// 形式（Supabase URI）を指定してください。",
            file=sys.stderr,
        )
        print("  upload スクリプトは psql/GORM DSN 形式には未対応です。", file=sys.stderr)
        return 1

    print(f"Uploading {len(assets)} asset(s) to bucket={bucket}" + (" [DRY RUN]" if dry_run else ""))

    for entry in assets:
        entity = entry["entity"]
        asset_id = str(entry["id"])
        rel_file = entry["file"]
        table = ENTITY_TABLE[entity]
        src = MASTER_DIR / rel_file
        ext = file_extension(src)
        object_key = storage_object_key(entity, asset_id, ext)
        public_url = supabase_public_url(supabase_url, bucket, object_key)
        ctype = content_type_for(ext)

        print(f"\n{entity} {asset_id}")
        print(f"  file: {rel_file}")
        print(f"  url:  {public_url}")

        upload_file(supabase_url, service_key, bucket, object_key, src, ctype, dry_run)
        update_image_url(database_url, table, asset_id, public_url, dry_run)

    print("\nDone.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
