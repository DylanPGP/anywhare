package com.dylanpgp.anywhare

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.osmdroid.config.Configuration
import java.io.File

class AnyWhareApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Español por defecto la primera vez; después AppCompat persiste la elección.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }

        Configuration.getInstance().apply {
            load(this@AnyWhareApp, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = "AnyWhare/${BuildConfig.VERSION_NAME}"
            // Caché dentro del almacenamiento privado: no requiere permisos de almacenamiento.
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
