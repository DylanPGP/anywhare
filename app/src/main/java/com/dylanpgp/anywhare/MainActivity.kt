package com.dylanpgp.anywhare

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dylanpgp.anywhare.ui.AnyWhareRoot
import com.dylanpgp.anywhare.ui.MainViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // La splash se mantiene hasta cargar los ajustes: transición sin parpadeos de tema.
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }
        enableEdgeToEdge()
        setContent { AnyWhareRoot(viewModel) }
    }
}
