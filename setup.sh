#!/bin/bash
# ============================================================
# setup.sh
# First-time setup: download Gradle wrapper
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

GRADLE_VERSION="8.6"
GRADLE_ZIP="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ZIP}"
GRADLE_DIST_DIR="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"

echo "=================================================="
echo " Setup: Downloading Gradle $GRADLE_VERSION wrapper"
echo "=================================================="

mkdir -p gradle/wrapper

# Create gradle-wrapper.properties
cat > gradle/wrapper/gradle-wrapper.properties << EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

echo "✅ gradle-wrapper.properties created"

# Download gradlew script
curl -fsSL "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}.0/gradlew" -o gradlew 2>/dev/null || \
cat > gradlew << 'GRADLEW_EOF'
#!/bin/sh
# Gradle start up script for UN*X
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
GRADLEW_EOF

chmod +x gradlew

# Download gradle-wrapper.jar
mkdir -p gradle/wrapper
WRAPPER_JAR_URL="https://github.com/gradle/gradle/raw/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar"
echo "Downloading gradle-wrapper.jar..."
curl -fsSL "$WRAPPER_JAR_URL" -o gradle/wrapper/gradle-wrapper.jar 2>/dev/null || {
    echo "⚠️  Could not download gradle-wrapper.jar automatically."
    echo "   Please run: gradle wrapper --gradle-version $GRADLE_VERSION"
    echo "   (requires Gradle to be installed: brew install gradle)"
}

echo ""
echo "=================================================="
echo " Next steps:"
echo "=================================================="
echo ""
echo "1. Make sure ANDROID_HOME is set in your shell profile:"
echo "   export ANDROID_HOME=\$HOME/Library/Android/sdk"
echo "   export PATH=\$PATH:\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools"
echo ""
echo "2. Install Android SDK (if not already):"
echo "   Download Android Studio from https://developer.android.com/studio"
echo "   OR use: brew install android-commandlinetools"
echo "   Then: sdkmanager 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0'"
echo ""
echo "3. Connect your Android phone via USB and enable USB Debugging"
echo ""
echo "4. Build & install:"
echo "   ./build_and_install.sh"
echo ""
