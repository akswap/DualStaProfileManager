# Dual STA (DUAL WiFi) Profile Manager & Required Tools

Android application for managing secondary Wi-Fi STA profiles on a rooted POCO F7 (`onyx`) with the Magisk module `onyx_dualsta_overlay` (`1.3-configurable`). The app keeps the primary `wlan0` connection read-only and manages the secondary `wlan1` profile configuration through the installed module.

## Features and upgrades

### 1. Side-by-side live interface cards

- Compact cards show `wlan0` (Primary) and `wlan1` (Secondary) together at the top of the screen.
- Dark Slate (`#0F172A`) card background provides high contrast.
- Live state badges use green for **UP**, red for **DOWN**, and blue for **PRIMARY**.
- Interface data refreshes automatically every three seconds.

### 2. Wi-Fi generation detection

The UI can label detected access points and active links as:

- **Wi-Fi 7+** — MLO evidence was reported in scan information elements, capabilities, or link text.
- **Wi-Fi 7** — 802.11be/EHT, Android Wi-Fi standard 11be, or 320 MHz evidence was found.
- **Wi-Fi 6E** — HE/802.11ax detected on the 6 GHz band.
- **Wi-Fi 6** — HE/802.11ax detected on 2.4 or 5 GHz.
- **Wi-Fi 5** — VHT/802.11ac detected.
- **Wi-Fi 4** — HT/802.11n detected or used as the basic fallback.

The scanner checks Android's `wifiStandard`, channel width, capabilities, and available 802.11 information elements. Active interface cards also inspect the bitrate text returned by `iw dev <interface> link`. The badge describes the evidence Android and the driver expose; it does not change the negotiated Wi-Fi mode.

Badge colors: Wi-Fi 7+ gold (`#D97706`), Wi-Fi 7 orange (`#EA580C`), Wi-Fi 6E indigo (`#4F46E5`), Wi-Fi 6 emerald (`#059669`), Wi-Fi 5 cyan (`#0284C7`), and fallback slate (`#475569`).

### 3. Frequency, band, and channel display

Frequency is converted to the corresponding 2.4, 5, or 6 GHz channel where possible. Examples:

| Frequency | Display |
|---:|---|
| 2412 MHz | 2.4 GHz CH 1 |
| 2437 MHz | 2.4 GHz CH 6 |
| 5180 MHz | 5 GHz CH 36 |
| 5200 MHz | 5 GHz CH 40 |
| 5745 MHz | 5 GHz CH 149 |
| 5955 MHz | 6 GHz CH 1 |
| 6135 MHz | 6 GHz CH 37 |

### 4. Link speed and signal visibility

- RX bitrate uses bright emerald (`#34D399`).
- TX bitrate and SSID use bright sky blue (`#38BDF8`).
- Signal strength uses neon green (`#4ADE80`).
- BSSID uses a high-contrast slate tone.
- Values come from the live `iw` link output when the driver reports them.

### 5. Advanced Wi-Fi scanner

- A tabbed dialog separates **Scanned** networks from **Manual Add**.
- The scanned list is scrollable and shows SSID, BSSID, frequency, band/channel, signal, security, Wi-Fi generation, and current `wlan0`/`wlan1` indicators.
- Security classification includes WPA3, WPA2/WPA3 Transition, WPA2, Open, Enterprise, OWE, and Unknown. Enterprise, OWE, and unknown security profiles are shown but are not offered as supported personal-network configurations.
- Selecting an AP fills its SSID, BSSID, frequency, channel, and detected security automatically. The user supplies the password when required.
- Manual Add supports hidden or manually entered networks.
- The app waits for Android's scan-results broadcast. It identifies fresh results versus cached results returned when Android throttles scanning.
- Wi-Fi, runtime scan permissions, and the device Location service must be enabled for scan results.

### 6. High-contrast controls

- **Refresh** — dark slate (`#1E293B`).
- **Save** — deep teal (`#0F766E`).
- **wlan1: UP / DOWN** — green or red according to the interface state.
- **Scan & Select AP** — deep indigo (`#4338CA`).
- **Connect #1** — ocean blue (`#0284C7`).

### 7. Profile management and root integration

- Reorder profiles with **Move Up** and **Move Down**.
- Enable or disable individual profiles.
- Show or hide passphrases while editing.
- Save a selected AP as the highest-priority secondary profile and restart the secondary helper.
- Poll the resulting `wlan1` link to confirm the selected BSSID/frequency when possible.
- Bring `wlan1` up from the UI when the hotspot interface is not active.
- Read helper status and module logs through root access.

The app uses TopJohnWu `libsu`. Profile changes are Base64-encoded before entering the shell, written to `profiles.conf.tmp`, validated, backed up to `profiles.conf.bak`, assigned `root:root` with mode `0600`, and atomically renamed into place.

## Safety and privacy

- The app does not disconnect, forget, disable, or reconnect primary interface `wlan0`.
- It does not toggle Wi-Fi off and on.
- It requests no Internet permission and operates locally.
- Android backup is disabled with `android:allowBackup="false"`.
- Passphrases are not written to Logcat, crash reporting, or SharedPreferences.
- Screen capture is blocked with `FLAG_SECURE` while the app is open.

## Requirements

- Rooted POCO F7 (`onyx`).
- Magisk root permission granted to the app.
- Installed `onyx_dualsta_overlay` module with `profiles.conf`.
- Wi-Fi and Location enabled for Android Wi-Fi scanning.

## GitHub Actions APK build

Every push to `main` runs unit tests and builds debug and release APKs. Open the repository's **Actions** tab, select the newest **Build Android APK** run, and download the `dual-sta-profile-manager-apks` artifact after the run succeeds.

## Current limitation

The application reports the standard and channel width exposed by Android and the Qualcomm driver. It cannot force the secondary STA to negotiate EHT, 320 MHz, or MLO when the concurrent Wi-Fi firmware/driver path limits `wlan1` to HE/160 MHz.
