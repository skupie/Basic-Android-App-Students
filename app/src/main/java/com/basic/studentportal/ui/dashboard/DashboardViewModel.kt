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

    private val _unreadNoticeCount = MutableStateFlow(0)
    val unreadNoticeCount: StateFlow<Int> = _unreadNoticeCount

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            val result = repository.getDashboard()
            _dashboard.value = result
            if (result is Resource.Success) {
                val notice = result.data.pendingNotice
                _unreadNoticeCount.value = if (notice != null && !notice.isAcknowledged) 1 else 0
            }
        }
        viewModelScope.launch {
            _attendanceSummary.value = Resource.Loading
            _attendanceSummary.value = attendanceRepository.getSummary(month = null)
        }
    }

    fun dismissDueAlert() {
        viewModelScope.launch { repository.dismissDueAlert() }
    }
}
