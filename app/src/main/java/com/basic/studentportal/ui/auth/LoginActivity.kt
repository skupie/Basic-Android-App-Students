package com.basic.studentportal.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.basic.studentportal.R
import com.basic.studentportal.databinding.ActivityLoginBinding
import com.basic.studentportal.ui.main.MainActivity
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    private var isEmailMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            viewModel.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) navigateToMain()
            }
        }

        selectLoginTab(email = true)

        binding.tabEmail.setOnClickListener {
            if (!isEmailMode) selectLoginTab(email = true)
        }
        binding.tabMobile.setOnClickListener {
            if (isEmailMode) selectLoginTab(email = false)
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun selectLoginTab(email: Boolean) {
        isEmailMode = email
        val activeDrawable = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
        val activeTxt   = ContextCompat.getColor(this, R.color.text_primary)
        val inactiveTxt = ContextCompat.getColor(this, R.color.text_hint)

        if (email) {
            binding.tabEmail.background = activeDrawable
            binding.tabEmail.setTextColor(activeTxt)
            binding.tabMobile.background = null
            binding.tabMobile.setTextColor(inactiveTxt)
            binding.tilEmail.isVisible = true
            binding.tilMobile.isVisible = false
            binding.labelIdentifier.text = "EMAIL ADDRESS"
            binding.etEmail.hint = "student@school.com"
            binding.etEmail.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        } else {
            binding.tabMobile.background = activeDrawable
            binding.tabMobile.setTextColor(activeTxt)
            binding.tabEmail.background = null
            binding.tabEmail.setTextColor(inactiveTxt)
            binding.tilEmail.isVisible = false
            binding.tilMobile.isVisible = true
            binding.labelIdentifier.text = "MOBILE NUMBER"
        }
    }

    private fun attemptLogin() {
        val identifier = if (isEmailMode) {
            binding.etEmail.text.toString().trim()
        } else {
            binding.etMobile.text.toString().trim()
        }
        val password = binding.etPassword.text.toString().trim()

        if (identifier.isEmpty()) {
            if (isEmailMode) binding.tilEmail.error = "Email is required"
            else binding.tilMobile.error = "Mobile number is required"
            return
        }
        if (isEmailMode && !android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            return
        }
        if (!isEmailMode && (identifier.length < 10 || !identifier.all { it.isDigit() })) {
            binding.tilMobile.error = "Enter a valid mobile number"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }

        binding.tilEmail.error = null
        binding.tilMobile.error = null
        binding.tilPassword.error = null

        // Pass isMobile flag so the correct API field is used
        viewModel.login(identifier, password, isMobile = !isEmailMode)
    }

    private fun observeLogin() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnLogin.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnLogin.isEnabled = true
                        navigateToMain()
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnLogin.isEnabled = true
                        if (state.message.isNotBlank()) showToast(state.message)
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onStart() {
        super.onStart()
        observeLogin()
    }
}
