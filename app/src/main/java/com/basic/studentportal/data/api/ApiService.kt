package com.basic.studentportal.data.api

import com.basic.studentportal.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ────────────────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<AuthUser>

    // ─── Student Profile ─────────────────────────────────────────────────────

    @GET("student/profile")
    suspend fun getStudentProfile(): Response<StudentProfile>

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GET("student/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @POST("student/due-alert/dismiss")
    suspend fun dismissDueAlert(): Response<MessageResponse>

    // ─── Attendance ───────────────────────────────────────────────────────────

    @GET("student/attendance")
    suspend fun getAttendance(
        @Query("month") month: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<ListResponse<AttendanceRecord>>

    @GET("student/attendance/summary")
    suspend fun getAttendanceSummary(
        @Query("month") month: String? = null
    ): Response<AttendanceSummary>

    // ─── Fee & Payments ───────────────────────────────────────────────────────

    @GET("student/invoices")
    suspend fun getInvoices(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): Response<InvoicesResponse>

    @GET("student/payments")
    suspend fun getPayments(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<ListResponse<FeePayment>>

    // ─── Weekly Exams ─────────────────────────────────────────────────────────

    @GET("student/weekly-exams/marks")
    suspend fun getWeeklyMarks(
        @Query("week_start") weekStart: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<WeeklyMarksResponse>

    @GET("student/weekly-exams/assignments")
    suspend fun getWeeklyAssignments(
        @Query("week_start") weekStart: String? = null,
        @Query("upcoming_only") upcomingOnly: Boolean = false,
        @Query("page") page: Int = 1
    ): Response<ListResponse<WeeklyExamAssignment>>

    @GET("student/weekly-exams/syllabi")
    suspend fun getWeeklySyllabi(
        @Query("week_start") weekStart: String? = null,
        @Query("subject") subject: String? = null,
        @Query("page") page: Int = 1
    ): Response<ListResponse<WeeklyExamSyllabus>>

    // ─── Model Tests ──────────────────────────────────────────────────────────

    @GET("student/model-tests")
    suspend fun getModelTests(): Response<ListResponse<ModelTest>>

    @GET("student/model-tests/results")
    suspend fun getModelTestResults(
        @Query("model_test_id") modelTestId: Int? = null,
        @Query("page") page: Int = 1
    ): Response<ListResponse<ModelTestResult>>

    // ─── Routines ─────────────────────────────────────────────────────────────

    @GET("student/routines")
    suspend fun getRoutines(
        @Query("date") date: String? = null,
        @Query("page") page: Int = 1
    ): Response<RoutinesResponse>

    // ─── Notices ──────────────────────────────────────────────────────────────

    @GET("student/notices")
    suspend fun getNotices(
        @Query("page") page: Int = 1
    ): Response<ListResponse<StudentNotice>>

    @GET("student/notices/pending")
    suspend fun getPendingNotice(): Response<PendingNoticeResponse>

    @POST("student/notices/{noticeId}/acknowledge")
    suspend fun acknowledgeNotice(
        @Path("noticeId") noticeId: Int
    ): Response<MessageResponse>

    // ─── Study Materials ──────────────────────────────────────────────────────

    @GET("student/study-materials")
    suspend fun getStudyMaterials(
        @Query("subject") subject: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<StudyMaterialsResponse>
}
