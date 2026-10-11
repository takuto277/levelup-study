#!/usr/bin/env python3
"""
Supabase Storage にマスタ画像をアップロードし、PostgreSQL の image_url を更新する。

Usage:
  # backend/.env を読み込んだうえで:
  make master-images-upload-dev     # dev: ローカル DB のみ（Storage は DEV_SUPABASE_* があれば使用）
  make master-images-upload-stg     # stg: STG_SUPABASE_* + STG_DATABASE_URL
  make master-images-upload-prod    # prod: PROD_SUPABASE_* + PROD_DATABASE_URL（要 CONFIRM_PROD_UPLOAD=yes）
  make master-images-upload-stg DRY_RUN=1

環境変数（--env ごとに解決）:
  dev  : DEV_DATABASE_URL → DATABASE_URL / DEV_SUPABASE_URL / DEV_SUPABASE_SERVICE_ROLE_KEY
         / DEV_SUPABASE_STORAGE_BUCKET / DEV_ASSET_BASE_URL
  stg  : STG_DATABASE_URL / STG_SUPABASE_URL / STG_SUPABASE_SERVICE_ROLE_KEY
         / STG_SUPABASE_STORAGE_BUCKET
  prod : PROD_DATABASE_URL / PROD_SUPABASE_URL / PROD_SUPABASE_SERVICE_ROLE_KEY
         / PROD_SUPABASE_STORAGE_BUCKET
  --env 未指定: DATABASE_URL / SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY
               / SUPABASE_STORAGE_BUCKET（従来動作）

  DRY_RUN=1                     アップロード・UPDATE を行わず表示のみ
"""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from urllib.parse import urlparse

import psycopg2
import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import (  # noqa: E402
    ENTITY_TABLE,
    MASTER_DIR,
    file_extension,
    load_manifest,
    storage_object_key,
    supabase_public_url,
)


class MissingEnvError(RuntimeError):
    """必須の環境変数が未設定。"""


@dataclass(frozen=True)
class TargetConfig:
    env: str
    database_url: str
    supabase_url: str
    service_key: str
    bucket: str
    storage_enabled: bool
    asset_base_url: str


def _env(*names: str) -> str:
    for name in names:
        val = os.environ.get(name, "").strip()
        if val:
            return val
    return ""


def _require(value: str, *names: str) -> str:
    if not value:
        raise MissingEnvError(
            f"環境変数 {' / '.join(names)} が未設定です（backend/.env を参照）"
        )
    return value


def resolve_config(env: str | None, default_bucket: str) -> TargetConfig:
    if env == "dev":
        database_url = _require(
            _env("DEV_DATABASE_URL", "DATABASE_URL"), "DEV_DATABASE_URL", "DATABASE_URL"
        )
        supabase_url = _env("DEV_SUPABASE_URL")
        service_key = _env("DEV_SUPABASE_SERVICE_ROLE_KEY")
        bucket = _env("DEV_SUPABASE_STORAGE_BUCKET") or default_bucket
        return TargetConfig(
            env="dev",
            database_url=database_url,
            supabase_url=supabase_url,
            service_key=service_key,
            bucket=bucket,
            storage_enabled=bool(supabase_url and service_key),
            asset_base_url=_env("DEV_ASSET_BASE_URL"),
        )

    if env == "stg":
        return _remote_config(
            env="stg",
            prefix="STG",
            default_bucket=default_bucket,
        )

    if env == "prod":
        return _remote_config(
            env="prod",
            prefix="PROD",
            default_bucket=default_bucket,
        )

    # --env 未指定: 従来のフラット変数
    return TargetConfig(
        env="default",
        database_url=_require(_env("DATABASE_URL"), "DATABASE_URL"),
        supabase_url=_require(_env("SUPABASE_URL"), "SUPABASE_URL"),
        service_key=_require(
            _env("SUPABASE_SERVICE_ROLE_KEY"), "SUPABASE_SERVICE_ROLE_KEY"
        ),
        bucket=_env("SUPABASE_STORAGE_BUCKET") or default_bucket,
        storage_enabled=True,
        asset_base_url="",
    )


def _remote_config(env: str, prefix: str, default_bucket: str) -> TargetConfig:
    return TargetConfig(
        env=env,
        database_url=_require(_env(f"{prefix}_DATABASE_URL"), f"{prefix}_DATABASE_URL"),
        supabase_url=_require(
            _env(f"{prefix}_SUPABASE_URL"), f"{prefix}_SUPABASE_URL"
        ),
        service_key=_require(
            _env(f"{prefix}_SUPABASE_SERVICE_ROLE_KEY"),
            f"{prefix}_SUPABASE_SERVICE_ROLE_KEY",
        ),
        bucket=_env(f"{prefix}_SUPABASE_STORAGE_BUCKET") or default_bucket,
        storage_enabled=True,
        asset_base_url="",
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


def asset_base_url_path(base: str, entity: str, asset_id: str, ext: str) -> str:
    return f"{base.rstrip('/')}/master/{entity}/{asset_id}.{ext}"


def describe_db(database_url: str) -> str:
    try:
        parsed = urlparse(database_url)
    except ValueError:
        return "(unparsable)"
    if parsed.hostname:
        return parsed.hostname
    return "(local/other)"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="マスタ画像を Storage + image_url へ入稿")
    parser.add_argument(
        "--env",
        choices=["dev", "stg", "prod"],
        default=None,
        help="対象環境。未指定は従来のフラット変数（DATABASE_URL/SUPABASE_*）を使用",
    )
    args = parser.parse_args(argv)

    dry_run = os.environ.get("DRY_RUN", "").lower() in {"1", "true", "yes"}

    manifest = load_manifest()
    assets = manifest.get("assets") or []
    if not assets:
        print("manifest.assets が空です。PNG を source/ に置き manifest を更新してください。")
        return 1

    try:
        cfg = resolve_config(args.env, manifest.get("bucket", "game-assets"))
    except MissingEnvError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    if cfg.env != "dev" and cfg.database_url.startswith("host="):
        print(
            "ERROR: DATABASE_URL は postgres:// 形式（Supabase URI）を指定してください。",
            file=sys.stderr,
        )
        print("  upload スクリプトは psql/GORM DSN 形式には未対応です。", file=sys.stderr)
        return 1

    header = f"[env={cfg.env}] {len(assets)} asset(s) → db={describe_db(cfg.database_url)}"
    if cfg.storage_enabled:
        header += f", bucket={cfg.bucket}"
    else:
        header += ", storage=off (dev ローカル DB のみ)"
    print(header + (" [DRY RUN]" if dry_run else ""))

    for entry in assets:
        entity = entry["entity"]
        asset_id = str(entry["id"])
        rel_file = entry["file"]
        table = ENTITY_TABLE[entity]
        src = MASTER_DIR / rel_file
        ext = file_extension(src)
        object_key = storage_object_key(entity, asset_id, ext)
        ctype = content_type_for(ext)

        print(f"\n{entity} {asset_id}")
        print(f"  file: {rel_file}")

        if cfg.storage_enabled:
            public_url = supabase_public_url(cfg.supabase_url, cfg.bucket, object_key)
            print(f"  url:  {public_url}")
            upload_file(
                cfg.supabase_url, cfg.service_key, cfg.bucket, object_key, src, ctype, dry_run
            )
            update_image_url(cfg.database_url, table, asset_id, public_url, dry_run)
        elif cfg.asset_base_url:
            public_url = asset_base_url_path(cfg.asset_base_url, entity, asset_id, ext)
            print(f"  url:  {public_url} (DEV_ASSET_BASE_URL)")
            update_image_url(cfg.database_url, table, asset_id, public_url, dry_run)
        else:
            print(
                "  [skip] Storage 無効 & DEV_ASSET_BASE_URL 未設定のため image_url は更新しません"
            )

    print("\nDone.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
