#!/usr/bin/env bash
# Builds tvtools.dex (SetLocale, ApproveInstalls) (run with app_process on the TV). Git Bash, no Gradle.
set -euo pipefail
cd "$(dirname "$0")"
SDK="$LOCALAPPDATA/Android/Sdk"
JAR="$SDK/platforms/android-34/android.jar"
JAVA_HOME="$(mise where java@temurin-17)"
rm -rf out && mkdir -p out/classes
"$JAVA_HOME/bin/javac" -source 8 -target 8 -bootclasspath "$JAR" -nowarn -d out/classes src/SetLocale.java src/ApproveInstalls.java
"$SDK/build-tools/34.0.0/d8.bat" --min-api 26 --lib "$JAR" --output out out/classes/*.class
mv out/classes.dex tvtools.dex && echo "built tvtools.dex"
