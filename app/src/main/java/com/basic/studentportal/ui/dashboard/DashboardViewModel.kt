package com.basic.studentportal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.model.AttendanceSummary
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.repository.AttendanceRepository
import com.basic.studentportal.data.repository.DashboardRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val attendanceRepository: AttendanceRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _dashboard = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val dashboard: StateFlow<Resource<DashboardResponse>> = _dashboard

    private val _attendanceSummary = MutableStateFlow<Resource<AttendanceSummary>>(Resource.Loading)
    val attendanceSummary: StateFlow<Resource<AttendanceSummary>> = _attendanceSummary

    private val _unreadNoticeCount = MutableStateFlow(0)
    val unreadNoticeCount: StateFlow<Int> = _unreadNoticeCount

    private val _showDueAlert = MutableSharedFlow<DueAlertData>(extraBufferCapacity = 1)
    val showDueAlert: SharedFlow<DueAlertData> = _showDueAlert.asSharedFlow()

    data class DueAlertData(
        val dueMonthCount: Int,
        val totalDue: Double,
        val message: String?
    )

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            val result = repository.getDashboard()
            _dashboard.value = result

            if (result is Resource.Success) {
                val data = result.data

                // Unread notice badge
                val notice = data.pendingNotice
                _unreadNoticeCount.value = if (notice != null && !notice.isAcknowledged) 1 else 0

                // Due alert — server's showDueAlert flag is the single source of truth.
                // Use dueAmount directly from server (not calculated) for accuracy.
                val due = data.dueSummary
                if (due != null && due.showDueAlert && due.dueMonthCount > 0 && due.dueAmount > 0) {
                    _showDueAlert.emit(
                        DueAlertData(due.dueMonthCount, due.dueAmount, due.dueAlertMessage)
                    )
                }
            }
        }
        viewModelScope.launch {
            _attendanceSummary.value = Resource.Loading
            _attendanceSummary.value = attendanceRepository.getSummary(month = null)
        }
    }

    fun dismissDueAlert(dueMonthCount: Int) {
        viewModelScope.launch {
            repository.dismissDueAlert()
        }
    }
}
