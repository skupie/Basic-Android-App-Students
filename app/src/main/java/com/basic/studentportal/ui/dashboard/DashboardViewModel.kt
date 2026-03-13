package com.basic.studentportal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.model.AttendanceSummary
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.repository.AttendanceRepository
import com.basic.studentportal.data.repository.DashboardRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _dashboard = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val dashboard: StateFlow<Resource<DashboardResponse>> = _dashboard

    private val _attendanceSummary = MutableStateFlow<Resource<AttendanceSummary>>(Resource.Loading)
    val attendanceSummary: StateFlow<Resource<AttendanceSummary>> = _attendanceSummary

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            _dashboard.value = repository.getDashboard()
        }
        viewModelScope.launch {
            _attendanceSummary.value = Resource.Loading
            // null month = current month on the server
            _attendanceSummary.value = attendanceRepository.getSummary(month = null)
        }
    }

    fun dismissDueAlert() {
        viewModelScope.launch { repository.dismissDueAlert() }
    }
}
