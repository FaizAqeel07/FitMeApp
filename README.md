# FitMe - Smart Fitness & Running Tracker 💪🏃‍♂️

<div align="center">
  <img src="https://github.com/FaizAqeel07/FitMeApp/blob/3289320c3aaeb7fdc337d4d3871e00d83f05a5eb/app/src/main/res/drawable/ic_splash_logo.png" alt="FitMe Logo" width="150"/>
  <br/>
  <h3>Centralized, Cloud-Synced, All-in-One Fitness Tracker</h3>
</div>

<br/>


## 📖 About The Project

Tracking weightlifting progress manually is often unstructured and carries a high risk of data loss. Furthermore, users usually have to juggle multiple separate apps to track their gym sessions and running activities. 

**FitMe** provides a centralized, cloud-synced logging solution that combines both weightlifting and cardio tracking into one secure, seamless, and convenient platform. Whether you are logging your Push/Pull/Legs routine or tracking your morning run, FitMe keeps your data safe and accessible across devices.

## ✨ Key Features

* ☁️ **Real-Time Cloud Sync:** Powered by Firebase, all your training history and fitness data are automatically backed up to the cloud, ensuring your data remains secure even if you switch devices.
* ♾️ **All-in-One Tracker:** Say goodbye to fragmented apps. Log your weightlifting routines (sets, reps, weights) and track your cardio runs in one centralized platform.
* 📊 **Interactive Dashboard & Stats:** Visualize your weekly progress and evaluate your training consistency through an intuitive, smart statistics dashboard.
* 🌓 **Modern & Minimalist Interface:** A clean, 100% declarative UI built with Jetpack Compose featuring a dark mode focus for a distraction-free workout experience.

## 🛠️ Under The Hood: Technology & Architecture

This application is built with Clean Architecture and Modern Android Development standards to ensure fast performance and long-term maintainability.

* **Main Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (100% Native Declarative UI)
* **Cloud Backend:** Firebase Auth & Realtime Database for secure user authentication and real-time cloud data syncing.
* **Local Storage:** Room Database (SQLite) acting as a reliable cache for ultra-fast data access.
* **Architecture:** MVVM (Model-View-ViewModel) separating UI from data logic for a clean, testable, and scalable codebase.
* **Asynchronous Operations:** Kotlin Coroutines & Flow for handling seamless background tasks without blocking the UI.

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest stable version recommended)
* Minimum SDK: 24

### Installation
1. Clone this repository:
   ```bash
   git clone [https://github.com/faizaqeel07/fitmeapp.git](https://github.com/faizaqeel07/fitmeapp.git)
2. Open the project in Android Studio.
3. Connect the project to your Firebase Console (Add google-services.json to the app/ directory if you are setting up your own Firebase environment).
4. Sync the project with Gradle files.
5. Build and run the app on an emulator or a physical Android device
