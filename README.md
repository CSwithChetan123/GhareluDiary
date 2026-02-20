# 📱 GhareluDiary - Household Staff Management App

Android app for tracking household staff attendance and expenses, designed for Indian households.

## 🎯 Live on Google Play
**[Download on Google Play Store](https://play.google.com/store/apps/details?id=com.ghareludiarydevelopment.app&hl=en_IN)**

Recently launched | Actively maintained

---

## 📋 About

GhareluDiary solves the common problem of tracking household staff (maid, cook, driver, gardener) attendance and expenses. Replaces messy paper registers with a simple digital solution.

**Problem Solved:**
- Lost paper registers
- Payment disputes over attendance
- Forgotten attendance records
- Manual monthly salary calculations
- Data loss when phone is lost

**Solution:**
- One-tap YES/NO attendance marking
- Automatic monthly calculations
- PDF report generation
- Secure cloud backup
- Multi-device sync

---

## ✨ Key Features

- ✅ Track multiple categories (Maid, Cook, Driver, Gardener, Milk, Water)
- ✅ Simple YES/NO daily entry system
- ✅ Edit or delete entries anytime
- ✅ Monthly summary with attendance statistics
- ✅ PDF report generation
- ✅ Cloud sync with Firebase (data never lost)
- ✅ Offline support with automatic sync
- ✅ Daily reminders
- ✅ Secure Google Sign-In

---

## 🛠️ Tech Stack

**Languages & Architecture**
- Kotlin
- MVVM (Model-View-ViewModel)
- Repository Pattern

**Android Components**
- Room Database (local storage)
- Kotlin Coroutines + Flow (async operations)
- Material Design 3 (UI)
- View Binding

**Backend & Cloud**
- Firebase Authentication (Google Sign-In)
- Firebase Firestore (cloud database)
- WorkManager (notifications)

**Key Implementations**
- PDF generation using Android PdfDocument API
- Offline-first architecture with cloud sync
- Solved cloud data storage challenges with efficient sync strategy
- Duplicate prevention and conflict resolution

---

## 🏗️ Complete Project Structure
```
GhareluDiary/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ghareludiary/app/
│   │   │   │   │
│   │   │   │   ├── GhareluApplication.kt
│   │   │   │   │
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── EntryDao.kt
│   │   │   │   │   │   └── UserProfileDao.kt
│   │   │   │   │   │
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Entry.kt
│   │   │   │   │   │   ├── UserProfile.kt
│   │   │   │   │   │   └── CategoryType.kt
│   │   │   │   │   │
│   │   │   │   │   ├── remote/
│   │   │   │   │   │   └── FirebaseManager.kt
│   │   │   │   │   │
│   │   │   │   │   └── repository/
│   │   │   │   │       └── GhareluRepository.kt
│   │   │   │   │
│   │   │   │   ├── ui/
│   │   │   │   │   ├── splash/
│   │   │   │   │   │   └── SplashActivity.kt
│   │   │   │   │   │
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   └── GSignIn.kt
│   │   │   │   │   │
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   │   └── MainViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── category/
│   │   │   │   │   │   ├── CategoryActivity.kt
│   │   │   │   │   │   └── CategoryViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── report/
│   │   │   │   │   │   ├── ReportActivity.kt
│   │   │   │   │   │   └── ReportViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── SettingsActivity.kt
│   │   │   │   │   │
│   │   │   │   │   └── adapter/
│   │   │   │   │       ├── EntryAdapter.kt
│   │   │   │   │       └── CategoryAdapter.kt
│   │   │   │   │
│   │   │   │   └── utils/
│   │   │   │       ├── NotificationScheduler.kt
│   │   │   │       ├── NotificationHelper.kt
│   │   │   │       └── PdfGenerator.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_category.xml
│   │   │   │   │   ├── activity_report.xml
│   │   │   │   │   ├── activity_settings.xml
│   │   │   │   │   ├── item_entry.xml
│   │   │   │   │   └── item_category.xml
│   │   │   │   │
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── themes.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   │
│   │   │   │   ├── values-night/
│   │   │   │   │   └── themes.xml
│   │   │   │   │
│   │   │   │   ├── drawable/
│   │   │   │   ├── mipmap/
│   │   │   │   └── xml/
│   │   │   │
│   │   │   ├── AndroidManifest.xml
│   │   │   └── google-services.json
│   │   │
│   │   └── test/
│   │       └── (unit tests)
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## 💡 Key Technical Challenges Solved

### 1. Cloud Data Storage & Sync
**Challenge:** Ensuring data consistency between local Room database and Firebase Firestore while handling offline scenarios.

**Solution:**
- Offline-first architecture with Room as single source of truth
- Background sync to Firestore when network available
- Duplicate prevention using date-based unique constraints
- Last-write-wins conflict resolution
- Automatic retry mechanism for failed sync operations
- Efficient batch operations to minimize network calls

### 2. MVVM Architecture Implementation
**Challenge:** Maintaining clean separation of concerns and ensuring UI responsiveness.

**Solution:**
- Repository pattern as single source of truth
- ViewModel manages UI state with StateFlow
- LiveData for observing database changes
- Coroutines for async operations without blocking UI
- Clear data flow: View → ViewModel → Repository → Data Source

### 3. Date Normalization & Consistency
**Challenge:** Handling timezone differences and ensuring consistent date storage.

**Solution:**
- All dates normalized to midnight (00:00:00) local time
- Calendar-based date manipulation for accuracy
- Consistent date formatting across app (dd MMM yyyy)
- Month-year string format for efficient grouping and queries

### 4. PDF Report Generation
**Challenge:** Creating professional PDF reports without external libraries.

**Solution:**
- Custom PDF generation using Android PdfDocument API
- Dynamic table layouts with proper spacing
- Category-wise filtering and date range selection
- Formatted currency and attendance statistics

---

## 📊 Project Stats

- **Lines of Code:** 8,000+
- **Kotlin Files:** 50+
- **XML Layouts:** 25+
- **Development Time:** 3 months
- **Status:** Live on Play Store

---

## 🎓 Skills Demonstrated

- ✅ MVVM architecture pattern
- ✅ Room database with complex queries
- ✅ Firebase integration (Authentication + Firestore)
- ✅ Kotlin Coroutines and Flow for async operations
- ✅ Material Design 3 implementation
- ✅ Google Play Store deployment
- ✅ Cloud sync strategy and offline-first architecture
- ✅ PDF generation without third-party libraries
- ✅ WorkManager for background tasks
- ✅ Repository pattern for data management

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Minimum SDK 24 (Android 7.0)
- Target SDK 34 (Android 14)
- JDK 17
- Firebase account

### Setup Instructions

1. **Clone the repository**
```bash
git clone https://github.com/CSwithChetan123/GhareluDiary.git
cd GhareluDiary
```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync to complete

3. **Firebase Configuration**
   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Add Android app with package name: `com.ghareludiary.app`
   - Download `google-services.json`
   - Place file in `app/` directory
   - Enable Authentication (Google Sign-In)
   - Enable Firestore Database

4. **Build and Run**
   - Sync Gradle files
   - Build → Make Project
   - Run on emulator or physical device

---

## 📝 Version History

**v1.0.4** (Current - February 2026)
- ✨ Entry deletion feature
- ✨ Account management in Settings
- ✨ Privacy Policy & Terms of Service links
- ✨ Contact/Feedback form integration
- 🐛 Fixed entry display bugs in category screens
- 🐛 Improved Firebase sync reliability
- 🔧 Performance optimizations

**v1.0.3** (January 2026)
- ✨ Settings screen with app information
- ✨ Sign out functionality
- 🔧 UI improvements

**v1.0.1** (January 2026)
- 🎨 App rebranding (name change)
- 🎨 New app icon
- 🐛 Minor bug fixes

**v1.0.0** (January 2026)
- 🎉 Initial public release
- ✅ Core attendance tracking features
- ✅ Firebase cloud sync
- ✅ PDF report generation
- ✅ Daily reminders

---

## 📧 Contact

**Chetan Thapa**
- Email: cswithchetan@gmail.com
- LinkedIn: [linkedin.com/in/chetan-thapa-a145b0184](https://www.linkedin.com/in/chetan-thapa-a145b0184/)
- Play Store: [GhareluDiary](https://play.google.com/store/apps/details?id=com.ghareludiarydevelopment.app&hl=en_IN)

---

## 📄 Copyright & License

**© 2026 Chetan Thapa. All Rights Reserved.**

### Terms of Use

This project is available for viewing and educational purposes.

**✅ Allowed:**
- View source code for learning
- Reference architecture patterns in your own projects
- Use as portfolio/interview reference
- Study implementation techniques

**❌ Not Allowed:**
- Commercial redistribution
- Publishing modified versions on app stores
- Claiming this work as your own
- Removing copyright notices
- Using the code in production applications

### Legal Protection

This application is published on Google Play Store and protected under copyright law. The source code is shared for educational and portfolio demonstration purposes only.

For collaboration or licensing inquiries, please contact via email.

**Official App:** [GhareluDiary on Play Store](https://play.google.com/store/apps/details?id=com.ghareludiarydevelopment.app&hl=en_IN)

---

**Built with ❤️ for Indian households**
