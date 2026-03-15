package com.basic.studentportal.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.model.LoginResponse
import com.basic.studentportal.data.repository.AuthRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    tokenDataStore: TokenDataStore
) : ViewModel() {

    val isLoggedIn = tokenDataStore.isLoggedIn()

    private val _loginState = MutableStateFlow<Resource<LoginResponse>>(Resource.Loading)
    val loginState: StateFlow<Resource<LoginResponse>> = _loginState.asStateFlow()

    init {
        _loginState.value = Resource.Error("", null)
    }

    fun login(identifier: String, password: String, isMobile: Boolean) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading
            _loginState.value = authRepository.login(identifier, password, isMobile)
        }
    }
}
