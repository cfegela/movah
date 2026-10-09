# <img src="assets/icon.png" width="40" height="40" alt="Movah Icon" valign="middle"> Movah

A lightweight, reliable Android utility app that moves files from one directory to another on a set schedule. Built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database**, **AlarmManager**, and **Foreground Services**.

---

## Features

* **Scheduled Daily Moves**: Configure exact execution times (e.g., `03:00 AM`) that run 7 days a week.
* **Full Task Management (CRUD)**: Add, edit, delete, and toggle scheduled moves on/off with an active switch.
* **Overwriting & Atomic File Moves**: Moves all items located in the root of the source directory, automatically overwriting files of the same name in the destination. Utilizes zero-copy atomic renames (`File.renameTo`) with resilient buffered byte stream fallbacks and error cleanup.
* **Instant "Run Now" Testing**: Test any scheduled move immediately with the tap of a button without waiting for the scheduled alarm time.
* **Execution & Activity Logs**: Comprehensive persistent history tracking every move execution with status badges (`SUCCESS`, `PARTIAL`, `FAILED`), total items moved, timestamp, and detailed error reports. Includes a quick "Clear Logs" action.
* **Folder Picker**: Select directories using the Android Storage Access Framework folder picker with the folder icon button.
* **Deep Doze Reliability**: Employs `AlarmManager.setExactAndAllowWhileIdle()` to guarantee execution even when the device enters deep Doze mode.
* **Reboot Rescheduling**: Re-arms all enabled alarms automatically on system startup (`BOOT_COMPLETED`) or system time changes.
* **Data-Sync Foreground Service & WakeLock**: Executes transfers within an Android 14+ compliant `dataSync` Foreground Service holding a CPU `WakeLock` to prevent OEM process killing during bulk transfers.
* **Unified Visual Style**: Matches sibling apps **Snappah** and **Saturatah** with signature Google/Material Blue (`#1A73E8`), clean dark/light themes, and an adaptive launcher icon featuring a blue truck.

---

## Component Architecture

```
com.cfeg.movah
├── data/
│   ├── MoveTask.kt           # Room entity for scheduled move configurations
│   ├── MoveLog.kt            # Room entity for execution history records
│   ├── MoveDao.kt            # Reactive queries & mutations via Kotlin Flow
│   └── MovahDatabase.kt      # Room database singleton
├── engine/
│   ├── FileMoverEngine.kt    # Atomic file rename, overwrite, stream fallback & error handling
│   └── StorageHelper.kt      # Permissions and path resolution
├── scheduler/
│   ├── AlarmScheduler.kt     # Exact AlarmManager registration, cancellation & reboot re-arm
│   ├── FileMoverReceiver.kt  # BroadcastReceiver for alarm triggers, boot, and time shifts
│   └── FileMoverService.kt   # dataSync Foreground Service executing transfers with WakeLock
└── ui/
    ├── MainActivity.kt       # Navigation host with bottom tabs (Schedules & Logs)
    ├── viewmodel/
    │   └── MainViewModel.kt  # StateFlow management for tasks, logs, and triggers
    ├── screens/
    │   ├── TasksScreen.kt    # List of scheduled moves, permission banners, Run Now & controls
    │   ├── TaskEditDialog.kt # Folder pickers and time picker modal
    │   └── LogsScreen.kt     # Activity history list with status badges and clear action
    └── theme/
        ├── Color.kt
        └── Theme.kt          # Material 3 dynamic color scheme matching Snappah/Saturatah
```

---

## Required Permissions

| Permission | Purpose |
| :--- | :--- |
| `android.permission.MANAGE_EXTERNAL_STORAGE` | Grants broad filesystem read/write access for moving files across public folders without SAF performance bottlenecks. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Ensures alarms trigger at exact scheduled timestamps (Android 12+). |
| `android.permission.USE_EXACT_ALARM` | Compatibility for precise scheduling across newer Android targets. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Automatically re-registers scheduled alarms after device restarts. |
| `android.permission.WAKE_LOCK` | Keeps the CPU awake during active file transfer operations. |
| `android.permission.FOREGROUND_SERVICE` | Allows executing background file transfers without OS timeouts. |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Satisfies Android 14+ (API 34+) requirements for data-sync foreground services. |
| `android.permission.POST_NOTIFICATIONS` | Required on Android 13+ (API 33+) to display foreground service progress notifications. |

---

## Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0 (JVM 17) |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Local Database** | AndroidX Room `2.6.1` with KSP |
| **Concurrency** | Kotlin Coroutines & StateFlow |
| **Scheduling** | `AlarmManager` (`RTC_WAKEUP`) + `BroadcastReceiver` |
| **Execution** | `ForegroundService` (type `dataSync`) + `PowerManager.WakeLock` |
| **Target SDKs** | `minSdk 26` (Android 8.0) · `targetSdk 35` (Android 15) |
| **Build System** | Gradle `8.11.1` (Kotlin DSL) · Android Gradle Plugin `8.7.2` |

---

## Building and Installation

### 1. Build the APK

Ensure Java 17 and Android SDK are available in your environment, then run:

```bash
./gradlew assembleDebug
```

The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install via ADB

Connect your Android device with USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Storage Access

Because Movah performs direct file moves across storage directories:
1. Open **Movah** on your device.
2. Tap the **Grant Permission** prompt on the home screen.
3. Toggle **Allow access to manage all files** in Android System Settings.
