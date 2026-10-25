#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
BUILD_TOOLS="${BUILD_TOOLS:-$SDK/build-tools/37.0.0}"
PLATFORM_JAR="${PLATFORM_JAR:-$SDK/platforms/android-37.0/android.jar}"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$BUILD_TOOLS:$PATH"

PACKAGE="com.example.pathprobe"
ACTIVITY="$PACKAGE/.MainActivity"
OUT="$ROOT/build"
GEN="$OUT/gen"
CLASSES="$OUT/classes"
DEX="$OUT/dex"
KEYSTORE="$ROOT/.keystore/debug.keystore"

require_file() {
	if [ ! -e "$1" ]; then
		echo "error: missing $1" >&2
		echo "       set ANDROID_HOME, BUILD_TOOLS, PLATFORM_JAR or JAVA_HOME as needed" >&2
		exit 1
	fi
}

require_adb() {
	if ! command -v adb >/dev/null 2>&1; then
		echo "error: adb not found on PATH" >&2
		exit 1
	fi
}

ensure_keystore() {
	if [ -f "$KEYSTORE" ]; then
		return
	fi

	require_file "$JAVA_HOME/bin/keytool"
	echo "==> Creating local debug keystore"
	mkdir -p "$(dirname "$KEYSTORE")"
	"$JAVA_HOME/bin/keytool" -genkeypair -noprompt \
		-keystore "$KEYSTORE" \
		-storepass android -keypass android \
		-alias androiddebugkey \
		-keyalg RSA -keysize 2048 -validity 10000 \
		-dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
}

build_app() {
	require_file "$BUILD_TOOLS/aapt2"
	require_file "$BUILD_TOOLS/d8"
	require_file "$BUILD_TOOLS/zipalign"
	require_file "$BUILD_TOOLS/apksigner"
	require_file "$PLATFORM_JAR"
	require_file "$BUILD_TOOLS/core-lambda-stubs.jar"
	require_file "$JAVA_HOME/bin/javac"
	require_file "$JAVA_HOME/bin/keytool"

	rm -rf "$GEN" "$CLASSES" "$DEX"
	rm -f \
		"$OUT/res.zip" \
		"$OUT/base.apk" \
		"$OUT/unaligned.apk" \
		"$OUT/aligned.apk" \
		"$OUT/PathProbe.apk"
	mkdir -p "$GEN" "$CLASSES" "$DEX"

	echo "==> Compiling resources"
	"$BUILD_TOOLS/aapt2" compile \
		--dir app/src/main/res \
		-o "$OUT/res.zip"

	echo "==> Linking resources and manifest"
	"$BUILD_TOOLS/aapt2" link \
		-o "$OUT/base.apk" \
		-I "$PLATFORM_JAR" \
		--manifest app/src/main/AndroidManifest.xml \
		--java "$GEN" \
		--min-sdk-version 26 \
		--target-sdk-version 36 \
		--version-code 1 \
		--version-name 1.0 \
		--auto-add-overlay \
		-R "$OUT/res.zip"

	echo "==> Compiling Java"
	mapfile -t SOURCES < <(find app/src/main/java "$GEN" -name '*.java' -print)
	"$JAVA_HOME/bin/javac" \
		-encoding UTF-8 \
		-source 8 -target 8 \
		-Xlint:-options \
		-bootclasspath "$PLATFORM_JAR:$BUILD_TOOLS/core-lambda-stubs.jar" \
		-d "$CLASSES" \
		"${SOURCES[@]}"

	echo "==> Dexing"
	mapfile -t CLASS_FILES < <(find "$CLASSES" -name '*.class' -print)
	"$BUILD_TOOLS/d8" \
		--min-api 26 \
		--lib "$PLATFORM_JAR" \
		--output "$DEX" \
		"${CLASS_FILES[@]}"

	echo "==> Packaging"
	cp "$OUT/base.apk" "$OUT/unaligned.apk"
	(cd "$DEX" && zip -q -X "$OUT/unaligned.apk" classes.dex)

	echo "==> Aligning"
	"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"

	echo "==> Signing"
	if [ -n "${RELEASE_KEYSTORE_PATH:-}" ] || \
		[ -n "${RELEASE_KEYSTORE_PASSWORD:-}" ] || \
		[ -n "${RELEASE_KEY_ALIAS:-}" ] || \
		[ -n "${RELEASE_KEY_PASSWORD:-}" ]; then
		: "${RELEASE_KEYSTORE_PATH:?RELEASE_KEYSTORE_PATH is required for release signing}"
		: "${RELEASE_KEYSTORE_PASSWORD:?RELEASE_KEYSTORE_PASSWORD is required for release signing}"
		: "${RELEASE_KEY_ALIAS:?RELEASE_KEY_ALIAS is required for release signing}"
		: "${RELEASE_KEY_PASSWORD:?RELEASE_KEY_PASSWORD is required for release signing}"
		KEYSTORE="$RELEASE_KEYSTORE_PATH"
		KEY_ALIAS="$RELEASE_KEY_ALIAS"
		STORE_PASSWORD="$RELEASE_KEYSTORE_PASSWORD"
		KEY_PASSWORD="$RELEASE_KEY_PASSWORD"
	else
		ensure_keystore
		KEY_ALIAS="androiddebugkey"
		STORE_PASSWORD="android"
		KEY_PASSWORD="android"
	fi
	require_file "$KEYSTORE"
	APKSIGNER_KS_PASS="$STORE_PASSWORD" \
	APKSIGNER_KEY_PASS="$KEY_PASSWORD" \
	"$BUILD_TOOLS/apksigner" sign \
		--ks "$KEYSTORE" \
		--ks-key-alias "$KEY_ALIAS" \
		--ks-pass env:APKSIGNER_KS_PASS \
		--key-pass env:APKSIGNER_KEY_PASS \
		--min-sdk-version 26 \
		--out "$OUT/PathProbe.apk" \
		"$OUT/aligned.apk"
	"$BUILD_TOOLS/apksigner" verify "$OUT/PathProbe.apk"

	echo
	echo "APK:  $OUT/PathProbe.apk"
	echo "Size: $(du -h "$OUT/PathProbe.apk" | cut -f1)"
}

install_app() {
	require_adb
	if [ ! -f "$OUT/PathProbe.apk" ]; then
		build_app
	fi
	echo "==> Installing"
	adb install -r "$OUT/PathProbe.apk"
}

run_app() {
	require_adb
	if [ ! -f "$OUT/PathProbe.apk" ]; then
		build_app
	fi
	adb shell am start -W -n "$ACTIVITY"
}

screenshot() {
	require_adb
	mkdir -p "$OUT"
	adb exec-out screencap -p > "$OUT/screenshot.png"
	echo "Screenshot: $OUT/screenshot.png"
}

clean() {
	rm -rf "$OUT"
	echo "Removed $OUT"
}

COMMAND="${1:-build}"
case "$COMMAND" in
	build)      build_app ;;
	install)    install_app ;;
	run)        run_app ;;
	screenshot) screenshot ;;
	clean)      clean ;;
	keystore)   ensure_keystore && echo "Keystore: $KEYSTORE" ;;
	*)
		echo "usage: $0 {build|install|run|screenshot|clean|keystore}" >&2
		exit 2
		;;
esac
