package com.basic.studentportal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.model.AttendanceSummary
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.repository.AttendanceRepository
import com.basic.studentportal.data.repository.DashboardRepository
import com.basic.studentportal.data.repository.NoticeRepository
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
    private val noticeRepository: NoticeRepository,
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

    private val _showNoticeAlert = MutableSharedFlow<NoticeAlertData>(extraBufferCapacity = 1)
    val showNoticeAlert: SharedFlow<NoticeAlertData> = _showNoticeAlert.asSharedFlow()

    data class DueAlertData(
        val dueMonthCount: Int,
        val totalDue: Double,
        val message: String?
    )

    data class NoticeAlertData(
        val id: Int,
        val title: String,
        val body: String,
        val noticeDate: String
    )

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            val result = repository.getDashboard()
            _dashboard.value = result

            if (result is Resource.Success) {
                val data = result.data

                // Unread notice badge + notice popup
                val notice = data.pendingNotice
                if (notice != null && !notice.isAcknowledged) {
                    _unreadNoticeCount.value = 1
                    _showNoticeAlert.emit(
                        NoticeAlertData(
                            id         = notice.id,
                            title      = notice.title,
                            body       = notice.body,
                            noticeDate = notice.noticeDate
                        )
                    )
                } else {
                    _unreadNoticeCount.value = 0
                }

                // Due alert — show only when server explicitly sets showDueAlert = true
                val due     = data.dueSummary
                val monthly = data.student?.monthlyFee ?: 0.0
                if (due != null && due.showDueAlert && due.dueMonthCount > 0) {
                    val totalDue = due.dueMonthCount * monthly
                    if (totalDue > 0) {
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

    fun dismissDueAlert(dueMonthCount: Int) {
        viewModelScope.launch {
            repository.dismissDueAlert()
        }
    }

    fun acknowledgeNotice(noticeId: Int) {
        viewModelScope.launch {
            noticeRepository.acknowledgeNotice(noticeId)
            _unreadNoticeCount.value = 0
        }
    }
}
