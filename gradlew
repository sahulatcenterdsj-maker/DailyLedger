#!/bin/sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Gradle wrapper jar is missing; downloading official Gradle wrapper..."
  mkdir -p "$APP_HOME/gradle/wrapper"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar" -o "$JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$JAR" "https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar"
  else
    echo "Install curl or wget, then rerun ./gradlew" >&2
    exit 1
  fi
fi
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_CMD" -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
