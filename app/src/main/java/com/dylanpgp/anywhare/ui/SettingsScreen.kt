package com.dylanpgp.anywhare.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.dylanpgp.anywhare.R
import com.dylanpgp.anywhare.data.AppSettings
import com.dylanpgp.anywhare.data.ThemeMode
import kotlinx.coroutines.launch

private fun currentLanguage(): String =
    AppCompatDelegate.getApplicationLocales()[0]?.language?.takeIf { it in setOf("es", "en", "pt") } ?: "es"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(currentLanguage()) }
    val copiedMsg = stringResource(R.string.copied)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Apariencia ---
            Section(stringResource(R.string.section_appearance)) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.AMOLED to R.string.theme_amoled,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.PURPLE to R.string.theme_purple,
                ).forEach { (mode, res) ->
                    OptionRow(stringResource(res), settings.theme == mode) { vm.setTheme(mode) }
                }
            }

            // --- Idioma ---
            Section(stringResource(R.string.section_language)) {
                listOf("es" to R.string.lang_es, "en" to R.string.lang_en, "pt" to R.string.lang_pt).forEach { (tag, res) ->
                    OptionRow(stringResource(res), lang == tag) {
                        lang = tag
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                }
            }

            // --- Ubicación simulada ---
            Section(stringResource(R.string.section_mock)) {
                Text(stringResource(R.string.mock_help), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { ctx.openDeveloperOptions() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_dev_options))
                }
            }

            // --- Permisos ---
            Section(stringResource(R.string.section_permissions)) {
                PermissionRow("ACCESS_FINE_LOCATION", R.string.perm_fine)
                PermissionRow("ACCESS_COARSE_LOCATION", R.string.perm_coarse)
                PermissionRow("ACCESS_MOCK_LOCATION", R.string.perm_mock)
                PermissionRow("FOREGROUND_SERVICE / _LOCATION", R.string.perm_fgs)
                PermissionRow("POST_NOTIFICATIONS", R.string.perm_notif)
                PermissionRow("INTERNET / ACCESS_NETWORK_STATE", R.string.perm_internet)
            }

            // --- Privacidad ---
            Section(stringResource(R.string.section_privacy)) {
                Text(stringResource(R.string.privacy_zero_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.privacy_zero_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.providers_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.providers_body), style = MaterialTheme.typography.bodyMedium)
            }

            // --- Acerca de y apoyo ---
            Section(stringResource(R.string.section_about)) {
                Text(stringResource(R.string.license_text), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { ctx.openUrl(Links.REPO) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.repo_button))
                }
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.donations_title), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.donations_body), style = MaterialTheme.typography.bodyMedium)
                DonationRow("₿", Color(0xFFF7931A), "Bitcoin (BTC)", Links.BTC) {
                    ctx.copyToClipboard("BTC", Links.BTC)
                    scope.launch { snackbar.showSnackbar(copiedMsg) }
                }
                DonationRow("ɱ", Color(0xFFFF6600), "Monero (XMR)", Links.XMR) {
                    ctx.copyToClipboard("XMR", Links.XMR)
                    scope.launch { snackbar.showSnackbar(copiedMsg) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun PermissionRow(name: String, descRes: Int) {
    Column {
        Text(name, style = MaterialTheme.typography.labelLarge)
        Text(stringResource(descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DonationRow(symbol: String, color: Color, name: String, address: String, onCopy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onCopy).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy))
    }
}
