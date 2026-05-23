#!/usr/bin/env bash
# scripts/assets/ 配下 Python ツール用 venv ラッパー
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VENV="$ROOT/scripts/assets/.venv"

if [[ ! -d "$VENV" ]]; then
  python3 -m venv "$VENV"
  "$VENV/bin/pip" install -q -r "$ROOT/scripts/assets/requirements.txt"
fi

exec "$VENV/bin/python" "$@"
