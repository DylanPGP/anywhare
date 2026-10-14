package com.dylanpgp.anywhare.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Links {
    const val REPO = "https://github.com/DylanPGP/anywhare"
    const val BTC = "bc1qfzrhw9j43plpwjlj7ltmawxgw7ucqx7sxah09x"
    const val XMR = "887fQH1zjjMEoaBUbXaKSp2E5k3VD2cR2bbCCyUqaytY1fgJbJ9QNj1MZQemHet8HrKVXeQ2rhW2ud9sqnqgmTEjTRpkjQ3"
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        // Sin navegador instalado: no hay nada más que hacer.
    }
}

fun Context.openDeveloperOptions() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) {
            // ignorar
        }
    }
}

fun Context.copyToClipboard(label: String, text: String) {
    val cm = getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
