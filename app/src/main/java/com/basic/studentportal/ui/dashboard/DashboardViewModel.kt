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

    /**
     * Emits once whenever a due alert should be shown.
     * Using SharedFlow (not StateFlow) so it fires exactly once per trigger,
     * not on every collector re-subscription.
     */
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

                // Due alert — only emit if user hasn't already dismissed this exact count
                val due       = data.dueSummary
                val monthly   = data.student?.monthlyFee ?: 0.0
                if (due != null && due.dueMonthCount > 0) {
                    val totalDue = due.dueMonthCount * monthly
                    if (totalDue > 0 && !tokenDataStore.isDueAlertDismissed(due.dueMonthCount)) {
                        _showDueAlert.emit(
                            DueAlertData(due.dueMonthCount, totalDue, due.dueAlertMessage)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            _attendanceSummary.value = Resource.Loading
            _attendanceSummary.value = attendanceRepository.getSummary(month = null)
        }
    }

    /** Called when user taps "ঠিক আছে" — persists dismissal and notifies server. */
    fun dismissDueAlert(dueMonthCount: Int) {
        viewModelScope.launch {
            tokenDataStore.saveDueAlertDismissed(dueMonthCount)
            repository.dismissDueAlert()
        }
    }
}
