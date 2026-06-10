#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
VENV="$ROOT/scripts/master-images/.venv"
REQ="$ROOT/scripts/master-images/requirements.txt"
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
    && "$VENV/bin/python" -c "import yaml, requests, psycopg2" 2>/dev/null
}

if ! _venv_ok; then
  echo "[run.sh] .venv が不完全なため再作成します ..." >&2
  _venv_install
fi

# backend/.env があれば export（upload 用）
if [[ -f "$ROOT/backend/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT/backend/.env"
  set +a
fi

exec "$VENV/bin/python" "$@"
