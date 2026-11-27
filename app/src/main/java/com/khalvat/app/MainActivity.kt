package com.khalvat.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.khalvat.app.data.Prefs
import com.khalvat.app.ui.AppRoot
import com.khalvat.app.ui.KhalvatTheme
import com.khalvat.app.ui.LockScreen
import com.khalvat.app.ui.SplashScreen
import com.khalvat.app.ui.ThemeState

class MainActivity : FragmentActivity() {

    private lateinit var prefs: Prefs

    private var splashDone by mutableStateOf(false)
    private var unlocked by mutableStateOf(false)
    private var lockMessage by mutableStateOf<String?>(null)
    private var authInProgress = false
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        ThemeState.mode = prefs.themeMode

        if (prefs.blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        setContent {
            KhalvatTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val stage = when {
                            !splashDone -> 0
                            !unlocked -> 1
                            else -> 2
                        }
                        Crossfade(targetState = stage, animationSpec = tween(450), label = "stage") { s ->
                            when (s) {
                                0 -> SplashScreen(onFinished = ::onSplashFinished)
                                1 -> LockScreen(message = lockMessage, onUnlock = ::authenticate)
                                else -> AppRoot()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onSplashFinished() {
        splashDone = true
        if (!prefs.appLock) unlocked = true else authenticate()
    }

    override fun onStart() {
        super.onStart()
        if (!splashDone) return

        if (prefs.appLock) {
            val away = SystemClock.elapsedRealtime() - stoppedAt
            if (stoppedAt != 0L && away >= prefs.relockSeconds * 1000L) unlocked = false
            if (!unlocked) authenticate()
        } else {
            unlocked = true
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    private fun authenticate() {
        if (authInProgress || unlocked) return

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (BiometricManager.from(this).canAuthenticate(authenticators)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            lockMessage = "برای استفاده از برنامه ابتدا در تنظیمات گوشی قفل صفحه (پین، الگو یا اثر انگشت) را فعال کنید."
            return
        }

        authInProgress = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    lockMessage = null
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authInProgress = false
                    lockMessage = errString.toString()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود به خلوت")
            .setSubtitle("برای باز کردن برنامه هویت خود را تأیید کنید")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(info)
    }
}
