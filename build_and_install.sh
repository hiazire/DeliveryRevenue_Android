#!/bin/bash
# ============================================================
# build_and_install.sh
# Build DeliveryRevenue APK and install via ADB
# Usage: ./build_and_install.sh [debug|release]
# ============================================================

set -e

MODE=${1:-debug}
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=================================================="
echo " 外送營業額 APP - Build & Install"
echo " Mode: $MODE"
echo "=================================================="

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Install with: brew install openjdk@17"
    exit 1
fi

# Check ADB
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Install Android platform-tools:"
    echo "   brew install android-platform-tools"
    exit 1
fi

# Check device connected
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android device connected."
    echo "   Enable USB Debugging on your phone and connect via USB."
    exit 1
fi
echo "✅ Found $DEVICES device(s)"

cd "$PROJECT_DIR"

# Make gradlew executable
chmod +x ./gradlew

echo ""
echo "🔨 Building APK ($MODE)..."

if [ "$MODE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at: $APK_PATH"
    exit 1
fi

echo ""
echo "📦 Installing APK..."
adb install -r "$APK_PATH"

echo ""
echo "🚀 Launching app..."
adb shell am start -n "com.q8js.deliveryrevenue/.MainActivity"

echo ""
echo "✅ Done! App installed and launched."
echo "   APK: $APK_PATH"
