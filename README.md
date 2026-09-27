# Dual STA Profile Manager

Production-grade Android application for managing secondary Wi-Fi STA profiles on rooted POCO F7 devices (`onyx`) running Magisk module `onyx_dualsta_overlay` (version 1.3-configurable).

## Wi-Fi scan and connect

- Tap the scan icon to request a fresh Android Wi-Fi scan. Android may return cached results when scan throttling is active; the app labels both cases accurately.
- Location permission and the device Location service must be enabled for Android scan results.
- Results show SSID, BSSID, frequency, band, signal level, security, Wi-Fi generation, and current `wlan0`/`wlan1` status.
- Select an access point, enter its password, and save it as the highest-priority secondary STA profile. The Magisk helper is then restarted and the resulting `wlan1` link is verified.
- A manual profile form remains available for hidden networks.

## GitHub Actions APK build

Every push to `main` runs unit tests and builds the APK. Open the repository's **Actions** tab, select the latest **Build Android APK** run, and download the APK artifact after the run succeeds.

## Key Architecture & Safety Guarantees
1. **Zero wlan0 Interference**: The app never executes commands that change, disconnect, forget, disable, or reconnect the primary Wi-Fi interface `wlan0`. `wlan0` is treated strictly as **Read-Only**.
2. **Root & Magisk Integration**: Uses TopJohnWu libsu for secure root command execution without `Runtime.exec("su")`.
3. **Atomic Configuration Writes**: Configuration writes are encoded in Base64 before passing through the shell, written to a temporary file (`profiles.conf.tmp`), verified for correct 7-tab structure, backed up to `profiles.conf.bak`, and atomically renamed with `sync`.
4. **Privacy & Security**:
   - `android:allowBackup="false"`
   - Zero internet permission requested (fully offline).
   - Passphrases are never logged, exposed in crash reports, stored in SharedPreferences, or printed to Logcat.
   - Config file is strictly protected with mode `0600` and owner `root:root`.
