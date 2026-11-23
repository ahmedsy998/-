# Masroofy (مصروفي)

**Masroofy** is a 100% offline, native Android personal expense and income tracker built for **Syrian Pounds (SYP / ل.س)** with a full **Arabic RTL** Material Design 3 interface.

## What the App Does

- **100% Offline & Private:** All data is stored locally on the device using Room (SQLite) and DataStore. No internet permission, no accounts, no cloud sync, no ads, and no analytics.
- **Exact Whole-Number SYP Accounting:** Amounts are stored and calculated in exact integer arithmetic (`Long`) and formatted with thousand separators (`125,000 ل.س`).
- **Fast Transaction Entry:** Add expenses or income with a large numeric keypad, built-in and custom categories, date & time pickers, payment method (`نقداً`, `تحويل بنكي`, `بطاقة`), optional notes, and weekly/monthly recurring rules.
- **Interactive Dashboard:** View your current balance, monthly income & expenses, today's expenses, an interactive category donut chart, and a 7-day bar chart (tap any slice or bar to jump straight to filtered transactions).
- **Search & Multi-Filter Ledger:** Daily grouped transaction list with daily totals, full-text search by note or category, and filters for type, category, payment method, and date range (Today, This Week, This Month, or Custom Range).
- **Monthly Budgets & Local Alerts:** Set monthly limits per expense category and receive offline local notifications at **80%** and **100%** usage.
- **Weekly & Monthly Reports:** Category breakdowns, biggest single expense, and percentage comparison against the previous period.
- **Export & Backup:** Export transactions to UTF-8 CSV (with BOM for Excel) via the Android Share Sheet, and create/restore full JSON backups with confirmation before replacing data.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (Light, Dark, and System themes; full Arabic RTL)
- **Architecture:** Single-activity MVVM with `ViewModel` and `StateFlow`
- **Persistence:** Room Database + Jetpack DataStore Preferences
- **SDK:** `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`

## Opening in Android Studio

1. Clone or download this repository.
2. Open **Android Studio** (Ladybug or newer recommended) and select **File → Open...**, then choose the project root directory.
3. Let Gradle sync using the included Gradle Wrapper (`gradle/wrapper/gradle-wrapper.properties`).
4. Run the `app` configuration on an emulator or physical Android device (Android 8.0 / API 26+).

## Building a Debug APK from the Command Line

Ensure you have **JDK 17** and the **Android SDK** configured (`ANDROID_HOME` or `ANDROID_SDK_ROOT`).

If `debug.keystore` does not already exist in the project root, generate the standard Android debug keystore first (this file is gitignored and never committed):

```bash
keytool -genkeypair -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

Then build the debug APK with the Gradle wrapper:

```bash
./gradlew assembleDebug
```

The installable APK will be generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions CI

Every push triggers `.github/workflows/build-apk.yml`, which builds `./gradlew assembleDebug` and uploads `app-debug.apk` as a workflow artifact named **`Masroofy-APK`**.
