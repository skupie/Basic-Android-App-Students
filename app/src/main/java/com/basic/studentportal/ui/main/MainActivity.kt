package com.basic.studentportal.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.basic.studentportal.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar is hidden via layout — fragments have their own headers

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Setup bottom nav with nav controller (standard tab behaviour)
        binding.bottomNavigation.setupWithNavController(navController)

        // Fix #2: Home tab always returns to dashboard root — never remembers a sub-page
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.dashboardFragment) {
                // Pop everything off the back stack back to dashboard
                navController.popBackStack(R.id.dashboardFragment, false)
                true
            } else {
                // For all other tabs, navigate normally (replace current top-level)
                navController.navigate(item.itemId)
                true
            }
        }

        // Re-selecting a tab navigates to it even if already selected
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.dashboardFragment) {
                navController.popBackStack(R.id.dashboardFragment, false)
            }
        }
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
}
