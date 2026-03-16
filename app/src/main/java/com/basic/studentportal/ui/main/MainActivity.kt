package com.basic.studentportal.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.basic.studentportal.R
import com.basic.studentportal.data.api.AuthEventBus
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.databinding.ActivityMainBinding
import com.basic.studentportal.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var tokenDataStore: TokenDataStore

    @Inject
    lateinit var authEventBus: AuthEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.dashboardFragment) {
                navController.popBackStack(R.id.dashboardFragment, false)
                true
            } else {
                navController.navigate(item.itemId)
                true
            }
        }

        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.dashboardFragment) {
                navController.popBackStack(R.id.dashboardFragment, false)
            }
        }

        // Listen for 401 Unauthenticated events from AuthInterceptor.
        // When the same account logs in on another device, the server revokes
        // the old token. Any subsequent API call returns 401. We catch it here
        // and redirect to LoginActivity immediately instead of showing an error.
        lifecycleScope.launch {
            authEventBus.unauthorizedEvent.collect {
                forceLogout()
            }
        }

        handleNotificationIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }

    fun performLogout() {
        lifecycleScope.launch {
            tokenDataStore.clearAll()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun forceLogout() {
        // Token already cleared by AuthInterceptor — just navigate to login
        startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun handleNotificationIntent() {
        val type = intent?.getStringExtra("notification_type") ?: return

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        val destinationId: Int? = when (type) {
            "payment", "invoice", "due" -> R.id.feesFragment
            "notice"                   -> R.id.noticesFragment
            "routine"                  -> R.id.routinesFragment
            "material"                 -> R.id.studyMaterialsFragment
            else                       -> null
        }

        destinationId?.let { navController.navigate(it) }
        intent?.removeExtra("notification_type")
    }
}
