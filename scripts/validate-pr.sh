#!/usr/bin/env bash
# 変更パスに応じて lint / test を実行（PR 前のエージェント向け validate）
# GitHub Actions の代替ではなく、push 前の必須チェック。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

RUN_ALL=false
RUN_IOS=false
BASE="${BASE_BRANCH:-main}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all) RUN_ALL=true; shift ;;
    --ios) RUN_IOS=true; shift ;;
    -h | --help)
      echo "Usage: $0 [--all] [--ios]"
      echo "  --all  変更に関係なく backend + mobile を実行"
      echo "  --ios  macOS 上で xcodebuild も実行（要 Xcode 26.x）"
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

git fetch origin "$BASE" 2>/dev/null || true
CHANGED="$(git diff --name-only "origin/${BASE}...HEAD" 2>/dev/null || git diff --name-only HEAD~1..HEAD 2>/dev/null || echo "")"

need_backend=false
need_mobile=false
need_assets=false
need_master=false
need_script_syntax=false

if $RUN_ALL; then
  need_backend=true
  need_mobile=true
else
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    case "$f" in
      backend/*|render.yaml) need_backend=true ;;
      apps/mobile/*) need_mobile=true ;;
      apps/mobile/assets/*|scripts/assets/*) need_assets=true ;;
      backend/assets/master/*|scripts/master-images/*) need_master=true ;;
      .github/workflows/ci.yml)
        need_backend=true
        need_mobile=true
        need_assets=true
        need_master=true
        RUN_IOS=true
        ;;
      scripts/*.sh) need_script_syntax=true ;;
    esac
  done <<< "$CHANGED"
fi

FAILED=0
STEPS_RAN=0

run_step() {
  local name="$1"
  shift
  STEPS_RAN=$((STEPS_RAN + 1))
  echo ""
  echo "━━━ $name ━━━"
  if "$@"; then
    echo "✅ $name"
  else
    echo "❌ $name" >&2
    FAILED=1
  fi
}

if $need_backend; then
  run_step "Backend test" bash -c 'cd backend && CGO_ENABLED=1 go test ./... -count=1'
  run_step "Backend lint (go vet)" bash -c 'cd backend && go vet ./...'
fi

if $need_mobile; then
  LP="$ROOT/apps/mobile/local.properties"
  if [[ ! -f "$LP" ]]; then
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -n "$SDK" ]]; then
      mkdir -p "$(dirname "$LP")"
      printf 'sdk.dir=%s\napi.key=local-validate-placeholder\n' "$SDK" > "$LP"
    fi
  fi
  run_step "Mobile KMP Android compile" bash -c \
    'cd apps/mobile && ./gradlew :shared:compileDebugKotlinAndroid --no-daemon -q'
  run_step "Mobile KMP unit tests" bash -c \
    'cd apps/mobile && ./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest --no-daemon -q'
  run_step "Mobile KMP iOS framework link" bash -c \
    'cd apps/mobile && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon -q'
fi

if $need_assets; then
  run_step "Assets validate" bash -c \
    './scripts/assets/run.sh scripts/assets/validate_assets.py'
fi

if $need_master; then
  run_step "Master images validate" bash -c 'cd backend && make master-images-validate'
fi

if $need_script_syntax; then
  while IFS= read -r f; do
    [[ -z "$f" ]] && continue
    case "$f" in
      scripts/*.sh)
        run_step "Shell syntax ($f)" bash -n "$f"
        ;;
    esac
  done <<< "$CHANGED"
fi

if $RUN_IOS && $need_mobile; then
  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "⚠️  iOS xcodebuild は macOS のみ。CI（iOS — CI workflow）で確認してください。"
  else
    run_step "iOS xcodebuild" bash -c \
      'cd apps/mobile && xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "generic/platform=iOS Simulator" -configuration Debug build CODE_SIGNING_ALLOWED=NO'
  fi
fi

echo ""
if [[ $FAILED -ne 0 ]]; then
  echo "validate-pr: 1 件以上失敗" >&2
  exit 1
fi
if [[ $STEPS_RAN -eq 0 ]]; then
  if [[ -z "$CHANGED" ]]; then
    echo "ℹ️  変更ファイルが検出できませんでした。--all で全チェックを実行してください。"
  else
    echo "ℹ️  変更パス（docs/skills 等）に対応する自動チェックはありません。"
    echo "    コード変更がある場合は ./scripts/validate-pr.sh --all を実行してください。"
  fi
  exit 0
fi
echo "validate-pr: すべて成功"
