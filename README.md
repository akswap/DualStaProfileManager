# Dual STA Profile Manager

Production-grade Android application for managing secondary Wi-Fi STA profiles on rooted POCO F7 devices (`onyx`) running Magisk module `onyx_dualsta_overlay` (version 1.3-configurable).

## Getting an APK without Android Studio (Free GitHub Actions)
If you don't have Android Studio installed on your PC:
1. Download the project ZIP from this app.
2. Create a new repository on GitHub (public or private) and upload all the extracted project files.
3. Go to the **Actions** tab on GitHub — you will see the **Build Android APK** workflow running automatically.
4. Once completed (in ~2 minutes), click on the workflow run and download the compiled APK artifact directly from GitHub!

## Key Architecture & Safety Guarantees
1. **Zero wlan0 Interference**: The app never executes commands that change, disconnect, forget, disable, or reconnect the primary Wi-Fi interface `wlan0`. `wlan0` is treated strictly as **Read-Only**.
2. **Root & Magisk Integration**: Uses TopJohnWu libsu for secure root command execution without `Runtime.exec("su")`.
3. **Atomic Configuration Writes**: Configuration writes are encoded in Base64 before passing through the shell, written to a temporary file (`profiles.conf.tmp`), verified for correct 7-tab structure, backed up to `profiles.conf.bak`, and atomically renamed with `sync`.
4. **Privacy & Security**:
   - `android:allowBackup="false"`
   - Zero internet permission requested (fully offline).
   - Passphrases are never logged, exposed in crash reports, stored in SharedPreferences, or printed to Logcat.
   - Config file is strictly protected with mode `0600` and owner `root:root`.
