package com.basic.studentportal.ui.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.studentportal.data.model.*
import com.basic.studentportal.data.repository.ExamRepository
import com.basic.studentportal.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamsViewModel @Inject constructor(private val repository: ExamRepository) : ViewModel() {

    private val _marks = MutableStateFlow<Resource<WeeklyMarksResponse>>(Resource.Loading)
    val marks: StateFlow<Resource<WeeklyMarksResponse>> = _marks

    private val _assignments = MutableStateFlow<Resource<ListResponse<WeeklyExamAssignment>>>(Resource.Loading)
    val assignments: StateFlow<Resource<ListResponse<WeeklyExamAssignment>>> = _assignments

    private val _syllabi = MutableStateFlow<Resource<ListResponse<WeeklyExamSyllabus>>>(Resource.Loading)
    val syllabi: StateFlow<Resource<ListResponse<WeeklyExamSyllabus>>> = _syllabi

    private val _modelTests = MutableStateFlow<Resource<ListResponse<ModelTest>>>(Resource.Loading)
    val modelTests: StateFlow<Resource<ListResponse<ModelTest>>> = _modelTests

    private val _modelResults = MutableStateFlow<Resource<ListResponse<ModelTestResult>>>(Resource.Loading)
    val modelResults: StateFlow<Resource<ListResponse<ModelTestResult>>> = _modelResults

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _marks.value = Resource.Loading
            _marks.value = repository.getWeeklyMarks()
        }
        viewModelScope.launch {
            _assignments.value = Resource.Loading
            _assignments.value = repository.getAssignments(upcomingOnly = true)
        }
        viewModelScope.launch {
            _syllabi.value = Resource.Loading
            _syllabi.value = repository.getSyllabi()
        }
        viewModelScope.launch {
            _modelTests.value = Resource.Loading
            _modelTests.value = repository.getModelTests()
        }
        viewModelScope.launch {
            _modelResults.value = Resource.Loading
            _modelResults.value = repository.getModelTestResults()
        }
    }

    fun filterByWeek(weekStart: String?) {
        viewModelScope.launch {
            _marks.value = Resource.Loading
            _marks.value = repository.getWeeklyMarks(weekStart)
        }
    }

    fun filterModelResults(modelTestId: Int?) {
        viewModelScope.launch {
            _modelResults.value = Resource.Loading
            _modelResults.value = repository.getModelTestResults(modelTestId)
        }
    }
}
