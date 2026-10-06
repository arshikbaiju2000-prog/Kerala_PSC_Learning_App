# Kerala PSC Exam Preparation Platform

A production-quality, offline-first Android application designed for students preparing for Kerala Public Service Commission (PSC) examinations.

---

## 🎯 Product Philosophy: Ethical Gamification & Deep Mastery

Unlike typical engagement-driven platforms, this application does not employ dark patterns, fake timers, or manipulative loops. The core objective is:
* **Maximize meaningful questions answered and understood**
* **Optimize retention through retrieval practice and spaced repetition**
* **Provide transparent source attribution for every Kerala PSC question and official answer key**

---

## 🛠 Technology Stack

* **Platform:** Android (minSdk 26, targetSdk 35, compileSdk 35)
* **Language:** Kotlin 2.0.21
* **UI Toolkit:** Jetpack Compose (BOM 2024.10.01) with Material 3
* **Architecture:** Clean Layered Architecture (`presentation`, `domain`, `data`, `core`, `di`)
* **Data Flow:** Unidirectional Data Flow (UDF) with Coroutines and StateFlow
* **Navigation:** Type-Safe Navigation Compose with Kotlin Serialization
* **Dependency Injection:** Manual `AppContainer` (lightweight, zero-reflection)
* **Build System:** Gradle 8.11 with Version Catalog (`gradle/libs.versions.toml`)

---

## 📂 Project Architecture

```
com.keralapsc.prep/
├── PSCApplication.kt       # Application entry point & AppContainer initialization
├── MainActivity.kt         # Edge-to-edge Compose root
├── di/                     # Manual AppContainer & ViewModelFactory
├── core/                   # Dispatchers, utilities, and common primitives
├── domain/                 # Pure Kotlin models, repository interfaces, and use cases
├── data/                   # Data sources, local persistence, and repositories (Phase 2+)
├── presentation/           # Compose UI, Themes, Design Tokens, Components, Screens
└── worker/                 # WorkManager background sync workers (Phase 8+)
```

---

## 🚀 Build & Run Instructions

### Prerequisites
* Android Studio (Ladybug / Koala or newer)
* OpenJDK 21
* Android SDK Platform 35

### Command Line Build
```powershell
# Build debug APK and execute unit tests
.\gradlew.bat assembleDebug test
```

### Running on Emulator
```powershell
# List available emulators
emulator -list-avds

# Start the Pixel_8a emulator
emulator -avd Pixel_8a

# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity
adb shell am start -n com.keralapsc.prep/.MainActivity
```

---

## 🗺 Implementation Roadmap

* [x] **Phase 0:** Technical Specification & System Architecture
* [x] **Phase 1:** Project Initialization & Design System Baseline
* [ ] **Phase 2:** Data Layer & Persistence (Room Entities, DAOs, CrossRefs, Seed Pack)
* [ ] **Phase 3:** Domain Layer (Use Cases, Repository Implementations)
* [ ] **Phase 4:** Core Quiz UI & Frictionless Experience
* [ ] **Phase 5:** Main Navigation & Dashboard Integration
* [ ] **Phase 6:** Learning Engine & Spaced Repetition
* [ ] **Phase 7:** Full-Length Timed Mock Examinations
* [ ] **Phase 8:** Incremental Content Sync Engine
* [ ] **Phase 9:** Production Polish & Release Packaging
