#!/usr/bin/env bash
# scripts/assets/ 配下 Python ツール用 venv ラッパー
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VENV="$ROOT/scripts/assets/.venv"
REQ="$ROOT/scripts/assets/requirements.txt"
STAMP="$VENV/.requirements_installed"

_venv_install() {
  rm -rf "$VENV"
  python3 -m venv "$VENV"
  "$VENV/bin/pip" install -q -r "$REQ"
  shasum -a 256 "$REQ" > "$STAMP"
}

_venv_ok() {
  [[ -f "$STAMP" ]] \
    && [[ "$(cat "$STAMP")" == "$(shasum -a 256 "$REQ")" ]] \
    && "$VENV/bin/python" -c "import yaml, PIL" 2>/dev/null
}

if ! _venv_ok; then
  echo "[run.sh] .venv が不完全なため再作成します ..." >&2
  _venv_install
fi

exec "$VENV/bin/python" "$@"
