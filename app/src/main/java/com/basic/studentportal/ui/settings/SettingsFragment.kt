package com.basic.studentportal.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.repository.AuthRepository
import com.basic.studentportal.databinding.FragmentSettingsBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    data class Profile(val name: String, val email: String)

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    private val _updateResult = MutableStateFlow<Resource<String>?>(null)
    val updateResult: StateFlow<Resource<String>?> = _updateResult

    init {
        viewModelScope.launch {
            val name  = tokenDataStore.getUserName().first() ?: "—"
            val email = tokenDataStore.getUserEmail().first() ?: "—"
            _profile.value = Profile(name, email)
        }
    }

    fun updateEmail(newEmail: String, currentPassword: String) {
        viewModelScope.launch {
            _updateResult.value = Resource.Loading
            val result = authRepository.updateEmail(newEmail, currentPassword)
            if (result is Resource.Success) {
                // Persist updated email locally
                tokenDataStore.saveAuthData(
                    token = tokenDataStore.getToken().first() ?: "",
                    name  = _profile.value?.name ?: "",
                    email = newEmail,
                    role  = "student",
                    photoUrl = null
                )
                _profile.value = _profile.value?.copy(email = newEmail)
            }
            _updateResult.value = result
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _updateResult.value = Resource.Loading
            _updateResult.value = authRepository.updatePassword(currentPassword, newPassword)
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun resetUpdateResult() { _updateResult.value = null }
}

// ─── Fragment ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Profile info ─────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profile.collect { profile ->
                profile ?: return@collect
                binding.tvProfileName.text  = profile.name
                binding.tvProfileEmail.text = profile.email
                binding.tvAvatarInitial.text = profile.name.firstOrNull()?.uppercase() ?: "?"
            }
        }

        // ── Update results ────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateResult.collect { result ->
                result ?: return@collect
                when (result) {
                    is Resource.Loading -> {
                        binding.btnUpdateEmail.isEnabled    = false
                        binding.btnUpdatePassword.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.btnUpdateEmail.isEnabled    = true
                        binding.btnUpdatePassword.isEnabled = true
                        requireContext().showToast(result.data)
                        // Clear fields
                        binding.etNewEmail.text?.clear()
                        binding.etEmailPassword.text?.clear()
                        binding.etCurrentPassword.text?.clear()
                        binding.etNewPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()
                        viewModel.resetUpdateResult()
                    }
                    is Resource.Error -> {
                        binding.btnUpdateEmail.isEnabled    = true
                        binding.btnUpdatePassword.isEnabled = true
                        requireContext().showToast(result.message)
                        viewModel.resetUpdateResult()
                    }
                }
            }
        }

        // ── Update Email ──────────────────────────────────────────────────────
        binding.btnUpdateEmail.setOnClickListener {
            val newEmail  = binding.etNewEmail.text.toString().trim()
            val password  = binding.etEmailPassword.text.toString().trim()
            if (newEmail.isEmpty()) { binding.tilNewEmail.error = "Enter new email"; return@setOnClickListener }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                binding.tilNewEmail.error = "Enter a valid email"; return@setOnClickListener
            }
            if (password.isEmpty()) { binding.tilEmailPassword.error = "Enter your current password"; return@setOnClickListener }
            binding.tilNewEmail.error = null
            binding.tilEmailPassword.error = null
            viewModel.updateEmail(newEmail, password)
        }

        // ── Update Password ───────────────────────────────────────────────────
        binding.btnUpdatePassword.setOnClickListener {
            val current = binding.etCurrentPassword.text.toString().trim()
            val newPwd  = binding.etNewPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()
            if (current.isEmpty()) { binding.tilCurrentPassword.error = "Enter current password"; return@setOnClickListener }
            if (newPwd.length < 8) { binding.tilNewPassword.error = "Minimum 8 characters"; return@setOnClickListener }
            if (newPwd != confirm) { binding.tilConfirmPassword.error = "Passwords do not match"; return@setOnClickListener }
            binding.tilCurrentPassword.error = null
            binding.tilNewPassword.error = null
            binding.tilConfirmPassword.error = null
            viewModel.updatePassword(current, newPwd)
        }

        // ── Logout ────────────────────────────────────────────────────────────
        binding.btnLogout.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Logout") { _, _ ->
                    viewModel.logout()
                    startActivity(
                        Intent(requireContext(),
                            com.basic.studentportal.ui.auth.LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
