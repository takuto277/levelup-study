#!/bin/sh
# Xcode「Compile Kotlin Framework」フェーズ用。
# Gradle (KMP) には Java が必要。GUI 起動の Xcode には JAVA_HOME が無いことが多い。
set -eu

if [ "YES" = "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-}" ]; then
  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES"
  exit 0
fi

if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  elif [ -d "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [ -n "$JAVA_HOME" ]; then
      export JAVA_HOME
    fi
  fi
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
  echo "error: Java が見つかりません。Android Studio をインストールするか、JAVA_HOME を設定してください。" >&2
  echo "  例: export JAVA_HOME=\"/Applications/Android Studio.app/Contents/jbr/Contents/Home\"" >&2
  exit 1
fi

cd "${SRCROOT}/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
