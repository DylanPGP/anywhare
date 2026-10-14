package com.dylanpgp.anywhare.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_MAP = "map"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AnyWhareRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val current = settings ?: return

    AnyWhareTheme(current.theme) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = ROUTE_MAP) {
            composable(ROUTE_MAP) {
                MapScreen(vm = vm, settings = current, onOpenSettings = { nav.navigate(ROUTE_SETTINGS) })
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(vm = vm, settings = current, onBack = { nav.popBackStack() })
            }
        }
    }
}
