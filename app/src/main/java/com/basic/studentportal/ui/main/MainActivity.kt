package com.basic.studentportal.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.basic.studentportal.R
import com.basic.studentportal.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Handle notification deep-link tap
        handleNotificationIntent()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent()
    }

    private fun handleNotificationIntent() {
        val type = intent?.getStringExtra("notification_type") ?: return

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        when (type) {
            "payment", "invoice" -> navController.navigate(R.id.feesFragment)
            "notice"             -> navController.navigate(R.id.noticesFragment)
            "due"                -> navController.navigate(R.id.feesFragment)
            "routine"            -> navController.navigate(R.id.routinesFragment)
            "material"           -> navController.navigate(R.id.studyMaterialsFragment)
        }

        // Clear so repeated onNewIntent doesn't re-navigate
        intent?.removeExtra("notification_type")
    }
}
