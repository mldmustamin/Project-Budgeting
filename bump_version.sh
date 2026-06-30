#!/bin/bash
# Auto-increment APK version before build
# Run from project root: bash bump_version.sh && ./gradlew assembleDebug

cd "$(dirname "$0")"
BUILD_FILE="app/build.gradle.kts"
CURRENT_VERSION=$(grep "versionCode" "$BUILD_FILE" | grep -o '[0-9]\+' | head -1)
NEW_VERSION=$((CURRENT_VERSION + 1))

sed -i "s/versionCode = $CURRENT_VERSION/versionCode = $NEW_VERSION/" "$BUILD_FILE"
sed -i "s/versionName = \"[^\"]*\"/versionName = \"2.0.0-b$NEW_VERSION\"/" "$BUILD_FILE"

echo "Version bumped: $CURRENT_VERSION → $NEW_VERSION"
