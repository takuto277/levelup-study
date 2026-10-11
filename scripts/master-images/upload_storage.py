#!/usr/bin/env python3
"""Supabase Storage へ画像をアップロードする（DB 更新なし）。

複数フレームのキャラ画像など、m_* テーブルに 1:1 対応しない画像を
Storage に置くためのユーティリティ。image_url の更新は行わない。

Usage:
  # backend/.env を読み込んだうえで（run.sh 経由）:
  make master-images-upload-storage-stg \
      DIR=backend/assets/master/source/characters PREFIX=master/characters
  make master-images-upload-storage-stg DIR=... PREFIX=... DRY_RUN=1

環境変数の解決は upload.py の --env と同じ（dev/stg/prod）。
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from lib.manifest import REPO_ROOT, load_manifest  # noqa: E402
from upload import (  # noqa: E402
    MissingEnvError,
    content_type_for,
    resolve_config,
    upload_file,
)

SUPPORTED_EXTS = {".png", ".webp", ".jpg", ".jpeg"}


def resolve_dir(raw: str) -> Path:
    p = Path(raw)
    return p if p.is_absolute() else REPO_ROOT / p


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Supabase Storage へ画像をアップロード（DB 更新なし）"
    )
    parser.add_argument("--env", choices=["dev", "stg", "prod"], required=True)
    parser.add_argument(
        "--dir",
        required=True,
        help="アップロードするディレクトリ（リポジトリルート相対 or 絶対パス）",
    )
    parser.add_argument(
        "--prefix",
        required=True,
        help="Storage 上のキープレフィックス（例 master/characters）",
    )
    parser.add_argument(
        "--ext",
        action="append",
        default=None,
        help="対象拡張子（例 webp）。未指定は png/webp/jpg すべて",
    )
    args = parser.parse_args(argv)

    dry_run = os.environ.get("DRY_RUN", "").lower() in {"1", "true", "yes"}

    target_dir = resolve_dir(args.dir)
    if not target_dir.is_dir():
        print(f"ERROR: ディレクトリが存在しません: {target_dir}", file=sys.stderr)
        return 1

    exts = (
        {e if e.startswith(".") else f".{e}" for e in args.ext}
        if args.ext
        else SUPPORTED_EXTS
    )
    files = sorted(p for p in target_dir.iterdir() if p.suffix.lower() in exts)
    if not files:
        print(f"ERROR: 対象ファイルがありません: {target_dir} ({sorted(exts)})", file=sys.stderr)
        return 1

    manifest = load_manifest()
    try:
        cfg = resolve_config(args.env, manifest.get("bucket", "game-assets"))
    except MissingEnvError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    if not cfg.storage_enabled:
        print(
            "ERROR: この環境では Storage が無効です "
            f"({cfg.env.upper()}_SUPABASE_URL / {cfg.env.upper()}_SUPABASE_SERVICE_ROLE_KEY)",
            file=sys.stderr,
        )
        return 1

    prefix = args.prefix.strip("/")
    print(
        f"[env={cfg.env}] {len(files)} file(s) → bucket={cfg.bucket}, prefix={prefix}/"
        + (" [DRY RUN]" if dry_run else "")
    )

    for src in files:
        ext = src.suffix.lower().lstrip(".")
        object_key = f"{prefix}/{src.name}"
        print(f"\n{src.name}")
        print(f"  key: {object_key}")
        upload_file(
            cfg.supabase_url,
            cfg.service_key,
            cfg.bucket,
            object_key,
            src,
            content_type_for(ext),
            dry_run,
        )

    print("\nDone.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
