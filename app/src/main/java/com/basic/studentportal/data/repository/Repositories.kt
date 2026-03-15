package com.basic.studentportal.data.repository

import com.basic.studentportal.data.api.ApiService
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.data.model.*
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.parseError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.net.ConnectException
import javax.inject.Inject
import javax.inject.Singleton

// ─── Base helper ─────────────────────────────────────────────────────────────

private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Resource<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Resource.Success(body)
                else Resource.Error("Empty response body", response.code())
            } else {
                Resource.Error(response.parseError(), response.code())
            }
        } catch (e: UnknownHostException) {
            Resource.Error("ইন্টারনেট সংযোগ নেই। অনুগ্রহ করে নেটওয়ার্ক চেক করুন।")
        } catch (e: SocketTimeoutException) {
            Resource.Error("সার্ভার সাড়া দিচ্ছে না। পরে আবার চেষ্টা করুন।")
        } catch (e: ConnectException) {
            Resource.Error("সার্ভারের সাথে সংযোগ করা যাচ্ছে না। পরে আবার চেষ্টা করুন।")
        } catch (e: Exception) {
            Resource.Error("নেটওয়ার্ক সমস্যা। অনুগ্রহ করে আবার চেষ্টা করুন।")
        }
    }
}

// ─── Auth Repository ─────────────────────────────────────────────────────────

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        if (result is Resource.Success) {
            tokenDataStore.saveAuthData(
                token    = result.data.token,
                name     = result.data.user.name,
                email    = result.data.user.email,
                role     = result.data.user.role,
                photoUrl = result.data.user.profilePhotoUrl
            )
        }
        return result
    }

    suspend fun logout(): Resource<MessageResponse> {
        val result = safeApiCall { api.logout() }
        tokenDataStore.clearAll()
        return result
    }

    suspend fun getMe(): Resource<AuthUser> = safeApiCall { api.getMe() }

    suspend fun updateEmail(newEmail: String, currentPassword: String): Resource<String> {
        val result = safeApiCall {
            api.updateEmail(UpdateEmailRequest(email = newEmail, password = currentPassword))
        }
        return when (result) {
            is Resource.Success -> Resource.Success("Email updated successfully")
            is Resource.Error   -> Resource.Error(result.message, result.code)
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Resource<String> {
        val result = safeApiCall {
            api.updatePassword(
                UpdatePasswordRequest(
                    currentPassword      = currentPassword,
                    password             = newPassword,
                    passwordConfirmation = newPassword
                )
            )
        }
        return when (result) {
            is Resource.Success -> Resource.Success("Password updated successfully")
            is Resource.Error   -> Resource.Error(result.message, result.code)
            is Resource.Loading -> Resource.Loading
        }
    }
}

// ─── Student Repository ───────────────────────────────────────────────────────

@Singleton
class StudentRepository @Inject constructor(private val api: ApiService) {
    suspend fun getProfile(): Resource<StudentProfile> = safeApiCall { api.getStudentProfile() }
}

// ─── Dashboard Repository ─────────────────────────────────────────────────────

@Singleton
class DashboardRepository @Inject constructor(private val api: ApiService) {
    suspend fun getDashboard(): Resource<DashboardResponse> = safeApiCall { api.getDashboard() }
    suspend fun dismissDueAlert(): Resource<MessageResponse> = safeApiCall { api.dismissDueAlert() }
}

// ─── Attendance Repository ────────────────────────────────────────────────────

@Singleton
class AttendanceRepository @Inject constructor(private val api: ApiService) {
    suspend fun getAttendance(month: String? = null, status: String? = null, page: Int = 1): Resource<ListResponse<AttendanceRecord>> =
        safeApiCall { api.getAttendance(month, status, page) }

    suspend fun getSummary(month: String? = null): Resource<AttendanceSummary> =
        safeApiCall { api.getAttendanceSummary(month) }
}

// ─── Fee Repository ───────────────────────────────────────────────────────────

@Singleton
class FeeRepository @Inject constructor(private val api: ApiService) {
    suspend fun getInvoices(status: String? = null, page: Int = 1): Resource<InvoicesResponse> =
        safeApiCall { api.getInvoices(status, page) }

    suspend fun getPayments(page: Int = 1): Resource<ListResponse<FeePayment>> =
        safeApiCall { api.getPayments(page) }
}

// ─── Exam Repository ──────────────────────────────────────────────────────────

@Singleton
class ExamRepository @Inject constructor(private val api: ApiService) {
    suspend fun getWeeklyMarks(weekStart: String? = null, page: Int = 1): Resource<WeeklyMarksResponse> =
        safeApiCall { api.getWeeklyMarks(weekStart, page = page) }

    suspend fun getAssignments(weekStart: String? = null, upcomingOnly: Boolean = false): Resource<ListResponse<WeeklyExamAssignment>> =
        safeApiCall { api.getWeeklyAssignments(weekStart, upcomingOnly) }

    suspend fun getSyllabi(weekStart: String? = null, subject: String? = null): Resource<ListResponse<WeeklyExamSyllabus>> =
        safeApiCall { api.getWeeklySyllabi(weekStart, subject) }

    suspend fun getModelTests(): Resource<ListResponse<ModelTest>> =
        safeApiCall { api.getModelTests() }

    suspend fun getModelTestResults(modelTestId: Int? = null, page: Int = 1): Resource<ListResponse<ModelTestResult>> =
        safeApiCall { api.getModelTestResults(modelTestId, page) }
}

// ─── Routine Repository ───────────────────────────────────────────────────────

@Singleton
class RoutineRepository @Inject constructor(private val api: ApiService) {
    suspend fun getRoutines(date: String? = null): Resource<RoutinesResponse> =
        safeApiCall { api.getRoutines(date) }
}

// ─── Notice Repository ────────────────────────────────────────────────────────

@Singleton
class NoticeRepository @Inject constructor(private val api: ApiService) {
    suspend fun getNotices(page: Int = 1): Resource<ListResponse<StudentNotice>> =
        safeApiCall { api.getNotices(page) }

    suspend fun getPendingNotice(): Resource<PendingNoticeResponse> =
        safeApiCall { api.getPendingNotice() }

    suspend fun acknowledgeNotice(noticeId: Int): Resource<MessageResponse> =
        safeApiCall { api.acknowledgeNotice(noticeId) }
}

// ─── Study Material Repository ────────────────────────────────────────────────

@Singleton
class StudyMaterialRepository @Inject constructor(private val api: ApiService) {
    suspend fun getMaterials(subject: String? = null, page: Int = 1): Resource<StudyMaterialsResponse> =
        safeApiCall { api.getStudyMaterials(subject, page) }
}
