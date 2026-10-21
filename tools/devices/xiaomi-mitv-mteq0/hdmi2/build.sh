#!/usr/bin/env bash
# Builds hdmi2.apk with the Android SDK tools only (no Gradle). Run from this folder in Git Bash.
set -euo pipefail
cd "$(dirname "$0")"
SDK="$LOCALAPPDATA/Android/Sdk"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
JAVA_HOME="$(mise where java@temurin-17)"
rm -rf out && mkdir -p out/classes out/dex
"$BT/aapt2.exe" compile --dir res -o out/res.zip
"$BT/aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 29 \
    -o out/unsigned.apk out/res.zip
"$JAVA_HOME/bin/javac" -source 8 -target 8 -bootclasspath "$JAR" -nowarn -d out/classes src/com/gathin/hdmi2/Open.java
"$BT/d8.bat" --min-api 26 --lib "$JAR" --output out/dex out/classes/com/gathin/hdmi2/*.class
(cd out/dex && "$JAVA_HOME/bin/jar" uf ../unsigned.apk classes.dex)
"$BT/zipalign.exe" -f 4 out/unsigned.apk out/aligned.apk
"$BT/apksigner.bat" sign --ks "$(cygpath -m "$HOME")/.android/debug.keystore" --ks-pass pass:android --key-pass pass:android \
    --out hdmi2.apk out/aligned.apk
"$BT/apksigner.bat" verify hdmi2.apk && echo "built hdmi2.apk"
