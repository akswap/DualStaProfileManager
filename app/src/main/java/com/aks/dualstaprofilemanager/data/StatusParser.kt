package com.aks.dualstaprofilemanager.data

data class WlanLinkStatus(
    val isConnected: Boolean,
    val ssid: String?,
    val bssid: String?,
    val frequencyMhz: Int?,
    val signalDbm: Int?,
    val rxBitrate: String?,
    val txBitrate: String?
) {
    val bandLabel: String?
        get() = frequencyMhz?.let { freq ->
            when (freq) {
                in 2400..2500 -> "2.4 GHz"
                in 4900..5925 -> "5 GHz"
                in 5925..7125 -> "6 GHz"
                else -> null
            }
        }
}

object StatusParser {
    fun parseIwLink(output: String): WlanLinkStatus {
        if (output.contains("Not connected") || output.isBlank()) {
            return WlanLinkStatus(false, null, null, null, null, null, null)
        }

        var ssid: String? = null
        var bssid: String? = null
        var freq: Int? = null
        var signal: Int? = null
        var rxBitrate: String? = null
        var txBitrate: String? = null

        for (line in output.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("SSID:") -> {
                    ssid = trimmed.substring(5).trim()
                }
                trimmed.startsWith("Connected to") -> {
                    val parts = trimmed.split(" ")
                    if (parts.size >= 3) {
                        bssid = parts[2].lowercase()
                    }
                }
                trimmed.startsWith("freq:") -> {
                    freq = trimmed.substring(5).trim().toIntOrNull()
                }
                trimmed.startsWith("signal:") -> {
                    val sigPart = trimmed.substring(7).trim().replace(" dBm", "").toIntOrNull()
                    signal = sigPart
                }
                trimmed.startsWith("rx bitrate:") -> {
                    rxBitrate = trimmed.substring(11).trim()
                }
                trimmed.startsWith("tx bitrate:") -> {
                    txBitrate = trimmed.substring(11).trim()
                }
            }
        }

        return WlanLinkStatus(
            isConnected = bssid != null,
            ssid = ssid,
            bssid = bssid,
            frequencyMhz = freq,
            signalDbm = signal,
            rxBitrate = rxBitrate,
            txBitrate = txBitrate
        )
    }

    fun sanitizeLogLine(line: String): String {
        return line.replace(Regex("(?i)(pass(word)?[:=]\\s*)(\\S+)"), "$1[REDACTED]")
    }
}
