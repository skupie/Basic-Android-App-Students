package com.basic.studentportal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.model.AttendanceSummary
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.model.RoutinesResponse
import com.basic.studentportal.data.repository.AttendanceRepository
import com.basic.studentportal.data.repository.DashboardRepository
import com.basic.studentportal.data.repository.RoutineRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val attendanceRepository: AttendanceRepository,
    private val tokenDataStore: TokenDataStore,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val _dashboard = MutableStateFlow<Resource<DashboardResponse>>(Resource.Loading)
    val dashboard: StateFlow<Resource<DashboardResponse>> = _dashboard

    private val _attendanceSummary = MutableStateFlow<Resource<AttendanceSummary>>(Resource.Loading)
    val attendanceSummary: StateFlow<Resource<AttendanceSummary>> = _attendanceSummary

    private val _routinePreview = MutableStateFlow<Resource<RoutinesResponse>>(Resource.Loading)
    val routinePreview: StateFlow<Resource<RoutinesResponse>> = _routinePreview

    private val _routinePreviewDate = MutableStateFlow(getEffectiveRoutineDateString())
    val routinePreviewDate: StateFlow<String> = _routinePreviewDate

    private val _unreadNoticeCount = MutableStateFlow(0)
    val unreadNoticeCount: StateFlow<Int> = _unreadNoticeCount

    private val _showDueAlert = MutableSharedFlow<DueAlertData>(extraBufferCapacity = 1)
    val showDueAlert: SharedFlow<DueAlertData> = _showDueAlert.asSharedFlow()

    data class DueAlertData(
        val dueMonthCount: Int,
        val totalDue: Double,
        val message: String?
    )

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val effectiveRoutineDate = getEffectiveRoutineDateString()
        _routinePreviewDate.value = effectiveRoutineDate

        viewModelScope.launch {
            _dashboard.value = Resource.Loading
            val result = repository.getDashboard()
            _dashboard.value = result

            if (result is Resource.Success) {
                val data = result.data

                // Unread notice badge
                val notice = data.pendingNotice
                _unreadNoticeCount.value = if (notice != null && !notice.isAcknowledged) 1 else 0

                // Due alert
                // Always calculate total due as: monthlyFee * dueMonthCount
                val due = data.dueSummary
                val monthlyFee = data.student?.monthlyFee ?: 0.0
                val calculatedTotalDue =
                    if (due != null) due.dueMonthCount * monthlyFee else 0.0

                if (due != null && due.showDueAlert && due.dueMonthCount > 0 && calculatedTotalDue > 0) {
                    _showDueAlert.emit(
                        DueAlertData(
                            dueMonthCount = due.dueMonthCount,
                            totalDue = calculatedTotalDue,
                            message = due.dueAlertMessage
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            _attendanceSummary.value = Resource.Loading
            _attendanceSummary.value = attendanceRepository.getSummary(month = null)
        }

        viewModelScope.launch {
            _routinePreview.value = Resource.Loading
            _routinePreview.value = routineRepository.getRoutines(effectiveRoutineDate)
        }
    }

    fun dismissDueAlert(dueMonthCount: Int) {
        viewModelScope.launch {
            repository.dismissDueAlert()
        }
    }

    private fun getEffectiveRoutineDate(): LocalDate {
        val now = LocalTime.now()
        val today = LocalDate.now()
        return if (!now.isBefore(LocalTime.of(18, 0))) today.plusDays(1) else today
    }

    private fun getEffectiveRoutineDateString(): String {
        return getEffectiveRoutineDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}
