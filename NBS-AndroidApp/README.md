# 📱 NBS - Newtown Bill Splitter (Android App)

A modern Android application for splitting bills with AI-powered receipt scanning and smart calculations.

## ✨ Features

### 📸 **Smart Bill Scanning**
- **Camera Integration**: Take photos of receipts directly in the app
- **Gallery Selection**: Choose existing photos from your device
- **AI-Powered OCR**: Uses Google ML Kit for text recognition
- **Automatic Item Extraction**: Identifies menu items and prices automatically
- **Multibuy Detection**: Automatically detects and handles discount offers

### 👥 **Member Management**
- **Add/Remove Members**: Easy member management with persistent storage
- **Member Persistence**: Members are saved locally and persist between sessions
- **Equal Split Default**: All members are selected by default for equal splitting

### 🛒 **Item Assignment**
- **Visual Item Cards**: Modern card-based interface for each item
- **Checkbox Assignment**: Easy member assignment with checkboxes
- **Editable Prices**: Tap to edit any item price directly
- **Real-time Calculations**: See cost per person updates instantly
- **Multibuy Styling**: Special styling for discount/multibuy items

### 💰 **Smart Calculations**
- **Global Discount**: Apply percentage discounts to entire bills
- **Proportional Splitting**: Discounts applied proportionally to each member
- **Real-time Totals**: Live updates as you make changes
- **Currency Formatting**: All amounts displayed in GBP (£)

### 📊 **Final Totals**
- **Comprehensive Table**: Shows member, items, subtotal, discount, and final total
- **Items Column**: See exactly what each person ordered
- **Copy Functionality**: One-tap copy of individual totals
- **Professional Layout**: Clean, modern table design

## 🏗️ **Project Structure**

```
app/
├── src/main/
│   ├── java/com/newtown/billsplitter/
│   │   ├── MainActivity.kt                 # Main activity with navigation
│   │   ├── model/                         # Data models
│   │   │   ├── BillItem.kt
│   │   │   ├── Member.kt
│   │   │   └── BillTotals.kt
│   │   ├── ui/
│   │   │   ├── fragments/                 # Main screen fragments
│   │   │   │   ├── MembersFragment.kt
│   │   │   │   ├── BillUploadFragment.kt
│   │   │   │   ├── ItemsFragment.kt
│   │   │   │   └── TotalsFragment.kt
│   │   │   ├── adapter/                   # RecyclerView adapters
│   │   │   │   ├── MainPagerAdapter.kt
│   │   │   │   ├── MembersAdapter.kt
│   │   │   │   ├── ItemsAdapter.kt
│   │   │   │   └── TotalsAdapter.kt
│   │   │   └── camera/                    # Camera functionality
│   │   │       └── CameraActivity.kt
│   │   ├── viewmodel/                     # ViewModels
│   │   │   └── MainViewModel.kt
│   │   ├── data/                          # Database and repositories
│   │   │   ├── database/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── utils/                         # Utility classes
│   │       ├── ImageProcessor.kt
│   │       ├── TextRecognition.kt
│   │       └── Calculator.kt
│   └── res/
│       ├── layout/                        # Layout files
│       ├── values/                        # Resources
│       ├── drawable/                      # Icons and images
│       └── menu/                          # Menu files
```

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Kotlin 1.8.0+
- Gradle 7.4.2+

### **Installation**

1. **Clone the project**
   ```bash
   git clone <repository-url>
   cd NBS-AndroidApp
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `NBS-AndroidApp` folder
   - Click "OK"

3. **Sync Gradle**
   - Wait for Gradle sync to complete
   - Resolve any dependency issues if they arise

4. **Build and Run**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon)
   - Select your device and click "OK"

### **Building APK**

1. **Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

2. **Release APK**
   ```bash
   ./gradlew assembleRelease
   ```
   APK will be in: `app/build/outputs/apk/release/app-release.apk`

## 🎨 **Design System**

### **Colors**
- **Primary**: Purple gradient (#667eea → #764ba2)
- **Surface**: White (#ffffff)
- **Background**: Light gray (#f8fafc)
- **Text**: Dark gray (#1a1a1a)
- **Multibuy**: Orange (#f59e0b)

### **Typography**
- **Headlines**: Material Design 3 Headline Medium
- **Body**: Material Design 3 Body Medium
- **Captions**: Material Design 3 Body Small

### **Components**
- **Cards**: Elevated cards with 16dp corner radius
- **Buttons**: Material Design 3 buttons with 12dp corner radius
- **Input Fields**: Outlined text fields with focus states

## 🔧 **Technical Details**

### **Architecture**
- **MVVM Pattern**: Model-View-ViewModel architecture
- **Repository Pattern**: Data access through repositories
- **Room Database**: Local data persistence
- **LiveData**: Reactive UI updates
- **ViewBinding**: Type-safe view access

### **Dependencies**
- **UI**: Material Design 3, ConstraintLayout, RecyclerView
- **Camera**: CameraX for modern camera functionality
- **OCR**: Google ML Kit for text recognition
- **Database**: Room for local storage
- **Networking**: Retrofit for API calls (future use)
- **Image Loading**: Glide for efficient image loading

### **Permissions**
- **Camera**: For taking photos of receipts
- **Storage**: For accessing gallery images
- **Internet**: For future API integrations

## 📱 **Screenshots**

The app features a modern, intuitive interface with:
- Bottom navigation for easy switching between screens
- Tab layout for additional navigation options
- Card-based design for better visual hierarchy
- Responsive layouts that work on all screen sizes

## 🔮 **Future Enhancements**

- **Cloud Storage**: Backup data to cloud services
- **Multiple Currencies**: Support for different currencies
- **Receipt History**: View and manage past receipts
- **Export Options**: Share results via email, messaging, etc.
- **Dark Mode**: Complete dark theme support
- **Widgets**: Home screen widgets for quick access

## 🤝 **Contributing**

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 **License**

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 **Support**

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the code comments

---

**Built with ❤️ for the Newtown community** 