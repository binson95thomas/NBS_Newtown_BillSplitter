#!/bin/bash

echo "Building NBS - Newtown Bill Splitter APK..."
echo

# Check if gradlew exists
if [ ! -f "gradlew" ]; then
    echo "Error: gradlew not found. Please run this script from the project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

echo "Cleaning previous builds..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo
    echo "✅ APK built successfully!"
    echo "📱 APK location: app/build/outputs/apk/debug/app-debug.apk"
    echo
    echo "To install on device:"
    echo "1. Enable 'Developer options' on your Android device"
    echo "2. Enable 'USB debugging'"
    echo "3. Connect device via USB"
    echo "4. Run: adb install app/build/outputs/apk/debug/app-debug.apk"
    echo
else
    echo
    echo "❌ Build failed! Please check the error messages above."
    echo
fi 