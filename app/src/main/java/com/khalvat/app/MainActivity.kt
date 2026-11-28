package com.khalvat.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.khalvat.app.data.Prefs
import com.khalvat.app.protect.Notifier
import com.khalvat.app.protect.ProtectionJobService
import com.khalvat.app.protect.Requirements
import com.khalvat.app.ui.AppRoot
import com.khalvat.app.ui.KhalvatTheme
import com.khalvat.app.ui.SetupScreen
import com.khalvat.app.ui.ThemeState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Static launch screen (colour + logo, no animation); the theme switches after it.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = Prefs(this)
        ThemeState.mode = prefs.themeMode
        if (prefs.blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        Notifier.ensureChannels(this)
        ProtectionJobService.schedule(this)

        setContent {
            KhalvatTheme {
                // Re-check on every resume: if a permission or role disappeared, the setup gate returns.
                val ctx = LocalContext.current
                var ready by remember { mutableStateOf(Requirements.status(ctx).ok) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    ready = Requirements.status(ctx).ok
                    Notifier.refreshProtectionAlert(ctx)
                }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    Notifier.refreshProtectionAlert(ctx)
                }

                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    Crossfade(targetState = ready, animationSpec = tween(250), label = "gate") { ok ->
                        if (ok) {
                            AppRoot()
                        } else {
                            SetupScreen(onReady = {
                                ready = true
                                Notifier.refreshProtectionAlert(ctx)
                            })
                        }
                    }
                }
            }
        }
    }
}
