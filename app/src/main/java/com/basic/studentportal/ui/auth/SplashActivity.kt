package com.basic.studentportal.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.basic.studentportal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Must be registered here (not inside onCreate) per Android Activity Result API rules
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whether granted or denied, always proceed to login
        navigateToLogin()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            requestNotificationPermissionThenProceed()
        }
    }

    private fun requestNotificationPermissionThenProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): POST_NOTIFICATIONS must be granted at runtime.
            // Declaring it in AndroidManifest.xml alone is NOT enough on API 33+.
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Android 12 and below: permission is granted automatically by the OS
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        finish()
    }
}
