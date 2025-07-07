@echo off
echo Building NBS - Newtown Bill Splitter APK...
echo.

REM Set JAVA_HOME to Java 17 for this build session
set JAVA_HOME=C:\Users\binso\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.15.6-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Java version:
java -version
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please run this script from the project root directory.
    pause
    exit /b 1
)

echo Cleaning previous builds...
call gradlew.bat clean

echo Building debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ APK built successfully!
    echo 📱 APK location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on device:
    echo 1. Enable "Developer options" on your Android device
    echo 2. Enable "USB debugging"
    echo 3. Connect device via USB
    echo 4. Run: adb install app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo.
    echo ❌ Build failed! Please check the error messages above.
    echo.
)

pause 