#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"
if [ ! -f "$JAR" ]; then
  URL="https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
  if command -v curl >/dev/null 2>&1; then curl -fL --retry 2 "$URL" -o "$JAR";
  else echo "Install curl to bootstrap Gradle" >&2; exit 1; fi
fi
if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL=$(sha256sum "$JAR" | cut -d ' ' -f 1)
else
  ACTUAL=$(shasum -a 256 "$JAR" | cut -d ' ' -f 1)
fi
if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "ERROR: gradle-wrapper.jar hash does not match official Gradle checksum" >&2
  exit 1
fi
if [ -n "${JAVA_HOME:-}" ]; then JAVA_CMD="$JAVA_HOME/bin/java";else JAVA_CMD=java;fi
exec "$JAVA_CMD" -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
