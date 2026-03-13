package com.basic.studentportal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.repository.DashboardRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _dashboard = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val dashboard: StateFlow<Resource<DashboardResponse>> = _dashboard

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            _dashboard.value = repository.getDashboard()
        }
    }

    fun dismissDueAlert() {
        viewModelScope.launch { repository.dismissDueAlert() }
    }
}
