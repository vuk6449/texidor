#!/usr/bin/env bash
# ==============================================================================
# Texidor Build Script for Linux & macOS
# ==============================================================================
# Usage:
#   ./build.sh [OPTIONS]
#
# Architecture Options (Default: --uni):
#   --uni           Build Universal APK (runs on all Android devices)
#   --arm           Build for ARM architecture (arm64-v8a / armeabi-v7a)
#   --x86           Build for x86 / x86-64 architecture (emulators / ChromeOS)
#
# Build Type Options (Default: --release):
#   --release       Build optimized, signed Release APK
#   --debug         Build Debug APK
#
# Other Options:
#   -h, --help      Show this help message
# ==============================================================================

set -euo pipefail

ARCH="uni"
BUILD_TYPE="release"

# Parse CLI arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --uni)
      ARCH="uni"
      shift
      ;;
    --arm)
      ARCH="arm"
      shift
      ;;
    --x86|--x86-64|--x86_64)
      ARCH="x86"
      shift
      ;;
    --release)
      BUILD_TYPE="release"
      shift
      ;;
    --debug)
      BUILD_TYPE="debug"
      shift
      ;;
    -h|--help)
      echo "Texidor Multi-Platform Build Script (macOS / Linux)"
      echo "Usage: ./build.sh [--uni | --arm | --x86] [--release | --debug]"
      echo ""
      echo "Options:"
      echo "  --uni       Build Universal APK (Default)"
      echo "  --arm       Build for ARM (phones, tablets, TV)"
      echo "  --x86       Build for x86-64 (ChromeOS, emulators)"
      echo "  --release   Build Release APK (Default)"
      echo "  --debug     Build Debug APK"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run './build.sh --help' for usage."
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=================================================="
echo " Building Texidor"
echo " Architecture : $ARCH"
echo " Build Type   : $BUILD_TYPE"
echo "=================================================="

# Determine Gradle executable
if [[ -x "./gradlew" ]]; then
  GRADLE_CMD="./gradlew"
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD="gradle"
elif [[ -f "./gradlew" ]]; then
  chmod +x "./gradlew"
  GRADLE_CMD="./gradlew"
else
  echo "Error: Neither ./gradlew nor system gradle was found in PATH."
  exit 1
fi

# Ensure release keystore exists if building release
if [[ "$BUILD_TYPE" == "release" && ! -f "my-upload-key.jks" ]]; then
  echo "Generating local release signing keystore (my-upload-key.jks)..."
  keytool -genkeypair -v -keystore my-upload-key.jks \
    -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass upload -keypass upload \
    -dname "CN=Texidor, OU=Texidor, O=Texidor, L=City, ST=State, C=US"
fi

export STORE_PASSWORD="${STORE_PASSWORD:-upload}"
export KEY_PASSWORD="${KEY_PASSWORD:-upload}"

# Determine Gradle Task
if [[ "$BUILD_TYPE" == "release" ]]; then
  TASK=":app:assembleRelease"
  SRC_APK="app/build/outputs/apk/release/app-release.apk"
else
  TASK=":app:assembleDebug"
  SRC_APK="app/build/outputs/apk/debug/app-debug.apk"
fi

echo "Running: $GRADLE_CMD $TASK"
$GRADLE_CMD "$TASK"

OUT_DIR="build-outputs"
mkdir -p "$OUT_DIR"

FINAL_APK="$OUT_DIR/texidor-${BUILD_TYPE}-${ARCH}.apk"
cp "$SRC_APK" "$FINAL_APK"

echo "=================================================="
echo " BUILD SUCCESSFUL"
echo " Output: $FINAL_APK"
echo " Size  : $(du -h "$FINAL_APK" | cut -f1)"
echo "=================================================="
