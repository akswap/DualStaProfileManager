package com.aks.dualstaprofilemanager

import com.aks.dualstaprofilemanager.data.*
import org.junit.Assert.*
import org.junit.Test

class DualStaExtendedTest {

    @Test
    fun testScanSecurityClassification() {
        assertEquals(SecurityClassification.WPA3, ScanParser.classifySecurity("[WPA3-SAE-CCMP][ESS]"))
        assertEquals(SecurityClassification.WPA2, ScanParser.classifySecurity("[WPA2-PSK-CCMP][ESS]"))
        assertEquals(SecurityClassification.WPA2_WPA3, ScanParser.classifySecurity("[WPA2-PSK-CCMP][WPA3-SAE-CCMP][ESS]"))
        assertEquals(SecurityClassification.OPEN, ScanParser.classifySecurity("[ESS]"))
        assertEquals(SecurityClassification.OPEN, ScanParser.classifySecurity(""))
        assertEquals(SecurityClassification.ENTERPRISE, ScanParser.classifySecurity("[WPA2-EAP-CCMP][ESS]"))
        assertEquals(SecurityClassification.OWE, ScanParser.classifySecurity("[OWE][ESS]"))
    }

    @Test
    fun testDuplicateSsidsDifferentBssids() {
        val ap1 = DiscoveredAp("HomeWiFi", "11:22:33:44:55:66", 2412, -50, "[WPA2-PSK-CCMP]", SecurityClassification.WPA2)
        val ap2 = DiscoveredAp("HomeWiFi", "AA:BB:CC:DD:EE:FF", 5180, -60, "[WPA3-SAE-CCMP]", SecurityClassification.WPA3)

        assertNotEquals(ap1.bssid, ap2.bssid)
        assertEquals(ap1.ssid, ap2.ssid)
        assertTrue(ap1.bssid != ap2.bssid)
    }

    @Test
    fun testPreservingExistingProfilesAndOrdering() {
        val initialProfiles = listOf(
            WifiProfile(1, true, "ProfileA", "00:11:22:33:44:55", 2412, "WPA2", "password123"),
            WifiProfile(2, true, "ProfileB", "AA:BB:CC:DD:EE:FF", 5180, "WPA3", "password456")
        )

        val newProfile = WifiProfile(1, true, "ProfileNew", "11:22:33:44:55:66", 6215, "WPA3", "newpassword")
        val combined = mutableListOf(newProfile)
        combined.addAll(initialProfiles)

        val normalized = combined.mapIndexed { index, p -> p.copy(priority = index + 1) }

        assertEquals(3, normalized.size)
        assertEquals(1, normalized[0].priority)
        assertEquals("ProfileNew", normalized[0].ssid)
        assertEquals(2, normalized[1].priority)
        assertEquals("ProfileA", normalized[1].ssid)
        assertEquals(3, normalized[2].priority)
        assertEquals("ProfileB", normalized[2].ssid)
    }

    @Test
    fun testOpenProfileEmptyPassword() {
        assertTrue(Validators.isValidSecurityAndPassphrase("OPEN", ""))
        assertFalse(Validators.isValidSecurityAndPassphrase("OPEN", "notempty"))
    }

    @Test
    fun testDecimalIwFrequencyParsing() {
        val iwOutputNormal = "freq: 6215\nsignal: -55 dBm"
        val iwOutputDecimal = "freq: 6215.0\nsignal: -55 dBm"

        val statusNormal = StatusParser.parseIwLink(iwOutputNormal)
        val statusDecimal = StatusParser.parseIwLink(iwOutputDecimal)

        assertEquals(6215, statusNormal.frequencyMhz)
        assertEquals(6215, statusDecimal.frequencyMhz)
    }

    @Test
    fun testSelectedProfileConnectionVerification() {
        val selectedBssid = "02:11:22:33:44:55"
        val selectedFreq = 6215

        val linkOutput = "Connected to 02:11:22:33:44:55 (on wlan1)\nfreq: 6215.0\nSSID: Test6G"
        val parsed = StatusParser.parseIwLink(linkOutput)

        val bssidMatch = parsed.bssid?.equals(selectedBssid, ignoreCase = true) == true
        val freqMatch = parsed.frequencyMhz == selectedFreq

        assertTrue(parsed.isConnected)
        assertTrue(bssidMatch)
        assertTrue(freqMatch)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMalformedConfigurationRejection() {
        val malformedConfig = "# priority\tenabled\tssid\n1\t1\tIncompleteFields"
        ConfigParser.parseConfig(malformedConfig)
    }
}
