package com.dylanpgp.anywhare.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dylanpgp.anywhare.R
import com.dylanpgp.anywhare.data.AppSettings
import com.dylanpgp.anywhare.service.SimMode
import com.dylanpgp.anywhare.service.SimulationController
import java.util.Locale

@Composable
fun MapScreen(vm: MainViewModel, settings: AppSettings, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val sim by vm.sim.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val label by vm.label.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val speedText by vm.speedText.collectAsStateWithLifecycle()
    val recenter by vm.recenter.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Diálogo de primer inicio (texto exacto, sin traducir).
    if (!settings.welcomeSeen) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.welcome_title)) },
            text = { Text(stringResource(R.string.welcome_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.markWelcomeSeen()
                    ctx.openUrl(Links.REPO)
                }) { Text(stringResource(R.string.welcome_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { vm.markWelcomeSeen() }) { Text(stringResource(R.string.welcome_nop)) }
            },
        )
    }

    LaunchedEffect(message) {
        val res = message ?: return@LaunchedEffect
        val action = if (res == R.string.error_mock) ctx.getString(R.string.open_dev_options) else null
        val result = snackbar.showSnackbar(
            message = ctx.getString(res),
            actionLabel = action,
            duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        vm.consumeMessage()
        if (result == SnackbarResult.ActionPerformed) ctx.openDeveloperOptions()
    }

    fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (granted(Manifest.permission.ACCESS_FINE_LOCATION)) vm.startSimulation()
        else vm.postMessage(R.string.error_permission)
    }

    fun requestStart() {
        val needed = buildList {
            if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= 33 && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (needed.isEmpty()) vm.startSimulation() else permLauncher.launch(needed.toTypedArray())
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(
            point = sim.point,
            recenterTick = recenter,
            follow = sim.running && sim.mode == SimMode.JOYSTICK,
            dark = isDark,
            onTap = { lat, lon ->
                focus.clearFocus()
                vm.selectPoint(lat, lon)
            },
            modifier = Modifier.fillMaxSize(),
        )

        // --- Buscador superior ---
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 6.dp,
            ) {
                TextField(
                    value = search.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        when {
                            search.loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            search.query.isNotEmpty() -> IconButton(onClick = { vm.clearSearch() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        vm.submitSearch()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }

            if (search.results.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                ) {
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(search.results, key = { it.id }) { place ->
                            ListItem(
                                headlineContent = { Text(place.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    focus.clearFocus()
                                    vm.pickPlace(place)
                                },
                            )
                        }
                    }
                }
            } else {
                sim.point?.let { p ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.place_label), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                            val showLabel = label != null && !(sim.running && sim.mode == SimMode.JOYSTICK)
                            if (showLabel) {
                                Text(label.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", p.lat, p.lon),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp, start = 16.dp, end = 16.dp),
        )

        // --- Botones laterales ---
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalIconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
            }
            FilledTonalIconButton(onClick = { vm.requestRecenter() }) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.center_map))
            }
        }

        // --- Overlay flotante: joystick + panel de velocidad ---
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sim.mode == SimMode.JOYSTICK) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Joystick(onMove = vm::onJoystick)
                }
            }

            val speedValue = speedText.replace(',', '.').toFloatOrNull()
            val speedValid = speedValue != null &&
                speedValue in SimulationController.MIN_SPEED_KMH..SimulationController.MAX_SPEED_KMH

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sim.mode == SimMode.POINT,
                            onClick = { vm.setMode(SimMode.POINT) },
                            label = { Text(stringResource(R.string.mode_point)) },
                        )
                        FilterChip(
                            selected = sim.mode == SimMode.JOYSTICK,
                            onClick = { vm.setMode(SimMode.JOYSTICK) },
                            label = { Text(stringResource(R.string.mode_joystick)) },
                        )
                    }

                    if (sim.mode == SimMode.JOYSTICK) {
                        OutlinedTextField(
                            value = speedText,
                            onValueChange = vm::onSpeedChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.speed_label)) },
                            suffix = { Text(stringResource(R.string.speed_unit)) },
                            isError = !speedValid,
                            supportingText = if (!speedValid) {
                                { Text(stringResource(R.string.speed_error)) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 25, 60).forEach { v ->
                                FilterChip(
                                    selected = speedValue == v.toFloat(),
                                    onClick = { vm.setPresetSpeed(v) },
                                    label = { Text("$v") },
                                )
                            }
                        }
                    }

                    if (sim.running) {
                        Text(
                            stringResource(R.string.status_running, String.format(Locale.US, "%.1f", sim.currentSpeedMs * 3.6f)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Button(
                        onClick = { if (sim.running) vm.stopSimulation() else requestStart() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sim.running || (sim.point != null && (sim.mode == SimMode.POINT || speedValid)),
                    ) {
                        Icon(if (sim.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (sim.running) R.string.action_pause else R.string.action_play))
                    }
                }
            }
        }
    }
}
