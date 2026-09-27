package com.aks.dualstaprofilemanager

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aks.dualstaprofilemanager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { MaterialTheme { DualStaScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualStaScreen() {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Tap Refresh to grant root and load the module") }
    var profiles by remember { mutableStateOf<List<WifiProfile>>(emptyList()) }
    var wlan0 by remember { mutableStateOf("Not loaded") }
    var wlan1 by remember { mutableStateOf("Not loaded") }

    fun refresh() = scope.launch {
        busy = true
        runCatching {
            withContext(Dispatchers.IO) {
                check(RootShellManager.isRootAvailable()) { "Root access was not granted" }
                check(RootShellManager.checkModuleExists()) { "onyx_dualsta_overlay v1.3 module was not found" }
                val loaded = ConfigParser.parseConfig(RootShellManager.readConfig())
                Triple(loaded, RootShellManager.getWlanLink("wlan0"), RootShellManager.getWlanLink("wlan1"))
            }
        }.onSuccess {
            profiles = it.first; wlan0 = it.second; wlan1 = it.third
            message = "Loaded ${profiles.size} profile(s)"
        }.onFailure { message = it.message ?: "Load failed" }
        busy = false
    }

    fun save(reconnect: Boolean) = scope.launch {
        busy = true
        runCatching {
            val validated = profiles.mapIndexed { i, p ->
                require(Validators.isValidSsid(p.ssid)) { "Profile ${i + 1}: invalid SSID" }
                require(Validators.isValidBssid(p.bssid)) { "Profile ${i + 1}: invalid BSSID" }
                require(Validators.isValidFrequency(p.frequencyMhz)) { "Profile ${i + 1}: invalid frequency" }
                require(Validators.isValidSecurityAndPassphrase(p.security, p.passphrase)) { "Profile ${i + 1}: invalid security/password" }
                p.copy(priority = i + 1, bssid = p.bssid.lowercase())
            }
            withContext(Dispatchers.IO) {
                check(RootShellManager.saveConfigAtomic(ConfigParser.serializeConfig(validated))) { "Atomic save failed" }
                if (reconnect) check(RootShellManager.restartSecondaryHelper()) { "Saved, but helper restart failed" }
            }
            profiles = validated
        }.onSuccess { message = if (reconnect) "Saved. Watchdog will retry wlan1 within 30 seconds." else "Saved safely." }
         .onFailure { message = it.message ?: "Save failed" }
        busy = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Dual STA Profile Manager") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { refresh() }, enabled = !busy) { Text("Refresh") }
                Button(onClick = { save(false) }, enabled = !busy && profiles.isNotEmpty()) { Text("Save") }
                Button(onClick = { save(true) }, enabled = !busy && profiles.isNotEmpty()) { Text("Save + reconnect") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            StatusCard("Primary wlan0 — read only", wlan0)
            StatusCard("Secondary wlan1", wlan1)
            Text("Profiles", style = MaterialTheme.typography.titleLarge)
            profiles.forEachIndexed { index, profile ->
                ProfileEditor(profile, index, profiles.size,
                    onChange = { changed -> profiles = profiles.toMutableList().also { it[index] = changed } },
                    onDelete = { profiles = profiles.toMutableList().also { it.removeAt(index) } },
                    onUp = if (index > 0) {{ profiles = profiles.toMutableList().also { java.util.Collections.swap(it, index, index - 1) } }} else null,
                    onDown = if (index < profiles.lastIndex) {{ profiles = profiles.toMutableList().also { java.util.Collections.swap(it, index, index + 1) } }} else null)
            }
            OutlinedButton(onClick = { profiles = profiles + WifiProfile(profiles.size + 1, true, "", "", 6215, "WPA3", "") }) { Text("Add profile") }
            Text("Passwords stay only in root-owned profiles.conf (mode 0600). This app never changes wlan0.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun StatusCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(text, style = MaterialTheme.typography.bodySmall) } }
}

@Composable private fun ProfileEditor(p: WifiProfile, index: Int, count: Int, onChange: (WifiProfile) -> Unit, onDelete: () -> Unit, onUp: (() -> Unit)?, onDown: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row { Text("#${index + 1}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text("Enabled"); Switch(p.enabled, { onChange(p.copy(enabled = it)) }) }
        OutlinedTextField(p.ssid, { onChange(p.copy(ssid = it)) }, label = { Text("SSID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(p.bssid, { onChange(p.copy(bssid = it)) }, label = { Text("BSSID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(p.frequencyMhz.toString(), { it.toIntOrNull()?.let { n -> onChange(p.copy(frequencyMhz = n)) } }, label = { Text("Frequency MHz (${p.bandLabel})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("WPA3", "WPA2", "OPEN").forEach { sec -> FilterChip(selected = p.security == sec, onClick = { onChange(p.copy(security = sec, passphrase = if (sec == "OPEN") "" else p.passphrase)) }, label = { Text(sec) }) } }
        if (p.security != "OPEN") OutlinedTextField(p.passphrase, { onChange(p.copy(passphrase = it)) }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onUp?.invoke() }, enabled = onUp != null) { Text("Up") }
            TextButton(onClick = { onDown?.invoke() }, enabled = onDown != null) { Text("Down") }
            Spacer(Modifier.weight(1f)); TextButton(onClick = onDelete) { Text("Delete") }
        }
    } }
}
