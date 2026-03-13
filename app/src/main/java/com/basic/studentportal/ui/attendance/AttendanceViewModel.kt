package com.basic.studentportal.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.model.AttendanceRecord
import com.basic.studentportal.data.model.AttendanceSummary
import com.basic.studentportal.data.model.ListResponse
import com.basic.studentportal.data.repository.AttendanceRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _attendance = MutableStateFlow<Resource<ListResponse<AttendanceRecord>>>(Resource.Loading)
    val attendance: StateFlow<Resource<ListResponse<AttendanceRecord>>> = _attendance

    private val _summary = MutableStateFlow<Resource<AttendanceSummary>>(Resource.Loading)
    val summary: StateFlow<Resource<AttendanceSummary>> = _summary

    var selectedMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        private set

    init { loadAll() }

    fun loadAll() {
        loadAttendance()
        loadSummary()
    }

    fun filterByMonth(month: String) {
        selectedMonth = month
        loadAll()
    }

    private fun loadAttendance() {
        viewModelScope.launch {
            _attendance.value = Resource.Loading
            _attendance.value = repository.getAttendance(month = selectedMonth)
        }
    }

    private fun loadSummary() {
        viewModelScope.launch {
            _summary.value = Resource.Loading
            _summary.value = repository.getSummary(month = selectedMonth)
        }
    }
}
