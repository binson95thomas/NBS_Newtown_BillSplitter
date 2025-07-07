# 🚀 NBS Android App Setup Guide

## 📋 Prerequisites

Before you begin, make sure you have the following installed:

### **Required Software**
- **Android Studio** (Arctic Fox or later)
  - Download from: https://developer.android.com/studio
  - Minimum version: 2020.3.1 or later

- **Java Development Kit (JDK)**
  - Version 8 or 11 (recommended)
  - Android Studio usually includes this

- **Android SDK**
  - API Level 24+ (Android 7.0)
  - Target API Level 34 (Android 14)
  - Build Tools 34.0.0

### **System Requirements**
- **Windows**: Windows 10 or later
- **macOS**: macOS 10.14 or later
- **Linux**: Ubuntu 18.04 or later
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space

## 🔧 Installation Steps

### **Step 1: Download Android Studio**
1. Go to https://developer.android.com/studio
2. Download the latest version for your OS
3. Install Android Studio following the wizard

### **Step 2: Setup Android Studio**
1. **First Launch Setup**:
   - Choose "Standard" installation
   - Let it download SDK components
   - Wait for initial setup to complete

2. **Install Required SDK Components**:
   - Open Android Studio
   - Go to Tools → SDK Manager
   - Install these components:
     - Android SDK Platform 34
     - Android SDK Build-Tools 34.0.0
     - Android SDK Platform-Tools
     - Android Emulator
     - Intel x86 Emulator Accelerator (HAXM installer)

### **Step 3: Open the Project**
1. **Clone or Download**:
   ```bash
   git clone <repository-url>
   cd NBS-AndroidApp
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Click "Open an existing project"
   - Navigate to the `NBS-AndroidApp` folder
   - Click "OK"

3. **Wait for Gradle Sync**:
   - Android Studio will automatically sync the project
   - This may take 5-10 minutes on first run
   - Watch the progress bar at the bottom

### **Step 4: Configure Emulator (Optional)**
1. **Create Virtual Device**:
   - Go to Tools → AVD Manager
   - Click "Create Virtual Device"
   - Choose a phone (e.g., Pixel 6)
   - Download and select a system image (API 34 recommended)
   - Click "Finish"

## 🏗️ Building the App

### **Method 1: Using Android Studio**
1. **Connect Device or Start Emulator**:
   - Connect Android device via USB (enable USB debugging)
   - OR start the emulator from AVD Manager

2. **Build and Run**:
   - Click the green "Run" button (▶️)
   - Select your device/emulator
   - Click "OK"
   - Wait for build and installation

### **Method 2: Using Command Line**
1. **Windows**:
   ```cmd
   cd NBS-AndroidApp
   build_apk.bat
   ```

2. **macOS/Linux**:
   ```bash
   cd NBS-AndroidApp
   chmod +x build_apk.sh
   ./build_apk.sh
   ```

3. **Manual Gradle**:
   ```bash
   cd NBS-AndroidApp
   ./gradlew assembleDebug
   ```

## 📱 Installing on Device

### **Enable Developer Options**
1. Go to Settings → About Phone
2. Tap "Build Number" 7 times
3. Go back to Settings → Developer Options
4. Enable "USB Debugging"

### **Install APK**
1. **Via ADB**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Via File Manager**:
   - Copy APK to device
   - Open file manager
   - Tap APK file
   - Allow installation from unknown sources

## 🔍 Troubleshooting

### **Common Issues**

#### **Gradle Sync Failed**
- **Solution**: 
  - File → Invalidate Caches / Restart
  - Check internet connection
  - Update Android Studio

#### **Build Errors**
- **Solution**:
  - Clean project: Build → Clean Project
  - Rebuild: Build → Rebuild Project
  - Check error messages in Build tab

#### **Missing SDK Components**
- **Solution**:
  - Tools → SDK Manager
  - Install missing components
  - Sync project again

#### **Permission Denied (Linux/macOS)**
- **Solution**:
  ```bash
  chmod +x gradlew
  chmod +x build_apk.sh
  ```

### **Performance Issues**
- **Increase Memory**:
  - File → Settings → Appearance & Behavior → System Settings → Memory Settings
  - Increase "IDE max heap size" to 2048 MB

- **Enable Gradle Offline Mode**:
  - File → Settings → Build, Execution, Deployment → Gradle
  - Check "Offline work"

## 📊 Project Structure

```
NBS-AndroidApp/
├── app/                          # Main app module
│   ├── src/main/
│   │   ├── java/                 # Kotlin source code
│   │   ├── res/                  # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml   # App manifest
│   ├── build.gradle              # App-level build config
│   └── proguard-rules.pro        # Code obfuscation rules
├── build.gradle                  # Project-level build config
├── settings.gradle               # Project settings
├── gradle.properties             # Gradle properties
├── build_apk.bat                 # Windows build script
├── build_apk.sh                  # Unix build script
└── README.md                     # Project documentation
```

## 🎯 Next Steps

After successful setup:

1. **Explore the Code**:
   - Start with `MainActivity.kt`
   - Review the fragment structure
   - Check the data models

2. **Test Features**:
   - Add members
   - Take photos of receipts
   - Assign items to members
   - View totals

3. **Customize**:
   - Modify colors in `colors.xml`
   - Update strings in `strings.xml`
   - Add new features

## 📞 Support

If you encounter issues:

1. **Check the error logs** in Android Studio
2. **Search existing issues** in the repository
3. **Create a new issue** with:
   - Error message
   - Steps to reproduce
   - Device/OS information
   - Screenshots if applicable

---

**Happy coding! 🎉** 