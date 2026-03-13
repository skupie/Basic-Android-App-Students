package com.basic.studentportal.data.model

import com.google.gson.annotations.SerializedName

// ─── Auth ────────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_name") val deviceName: String = "Android App"
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: AuthUser
)

data class AuthUser(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("contact_number") val contactNumber: String?,
    @SerializedName("profile_photo_url") val profilePhotoUrl: String?
)

// ─── Student Profile ─────────────────────────────────────────────────────────

data class StudentProfile(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("class_level") val classLevel: String,
    @SerializedName("academic_year") val academicYear: String?,
    @SerializedName("section") val section: String,
    @SerializedName("monthly_fee") val monthlyFee: Double,
    @SerializedName("admission_fee") val admissionFee: Double?,
    @SerializedName("enrollment_date") val enrollmentDate: String?,
    @SerializedName("status") val status: String,
    @SerializedName("is_passed") val isPassed: Boolean
)

// ─── Dashboard ───────────────────────────────────────────────────────────────

data class DashboardResponse(
    @SerializedName("student") val student: StudentProfile?,
    @SerializedName("due_summary") val dueSummary: DueSummary?,
    @SerializedName("today_routines") val todayRoutines: List<Routine>,
    @SerializedName("routine_date") val routineDate: String?,
    @SerializedName("notes_summary") val notesSummary: NotesSummary?,
    @SerializedName("weekly_exam_summary") val weeklyExamSummary: WeeklyExamSummary?,
    @SerializedName("pending_notice") val pendingNotice: StudentNotice?
)

data class DueSummary(
    @SerializedName("due_month_count") val dueMonthCount: Int,
    @SerializedName("due_amount") val dueAmount: Double,
    @SerializedName("due_months") val dueMonths: List<String>,
    @SerializedName("show_due_alert") val showDueAlert: Boolean,
    @SerializedName("due_alert_message") val dueAlertMessage: String?
)

data class NotesSummary(
    @SerializedName("note_count") val noteCount: Int,
    @SerializedName("latest_note_title") val latestNoteTitle: String?,
    @SerializedName("latest_note_teacher_name") val latestNoteTeacherName: String?
)

data class WeeklyExamSummary(
    @SerializedName("exam_count") val examCount: Int,
    @SerializedName("average_percent") val averagePercent: Double,
    @SerializedName("performance_label") val performanceLabel: String,
    @SerializedName("trend_delta") val trendDelta: Double?,
    @SerializedName("recent_marks") val recentMarks: List<WeeklyExamMark>
)

// ─── Attendance ───────────────────────────────────────────────────────────────

data class AttendanceRecord(
    @SerializedName("id") val id: Int,
    @SerializedName("attendance_date") val attendanceDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("category") val category: String?,
    @SerializedName("note") val note: String?
)

data class AttendanceSummary(
    @SerializedName("month") val month: String,
    @SerializedName("present_count") val presentCount: Int,
    @SerializedName("absent_count") val absentCount: Int,
    @SerializedName("late_count") val lateCount: Int,
    @SerializedName("total_days") val totalDays: Int,
    @SerializedName("attendance_percent") val attendancePercent: Double
)

// ─── Fee & Payments ───────────────────────────────────────────────────────────

data class FeeInvoice(
    @SerializedName("id") val id: Int,
    @SerializedName("billing_month") val billingMonth: String,
    @SerializedName("billing_month_label") val billingMonthLabel: String?,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("amount_due") val amountDue: Double,
    @SerializedName("scholarship_amount") val scholarshipAmount: Double,
    @SerializedName("amount_paid") val amountPaid: Double,
    @SerializedName("outstanding_amount") val outstandingAmount: Double,
    @SerializedName("status") val status: String,
    @SerializedName("payment_mode_last") val paymentModeLast: String?
)

data class FeePayment(
    @SerializedName("id") val id: Int,
    @SerializedName("amount") val amount: Double,
    @SerializedName("payment_date") val paymentDate: String,
    @SerializedName("payment_mode") val paymentMode: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("receipt_number") val receiptNumber: String?,
    @SerializedName("invoice") val invoice: FeeInvoice?
)

data class InvoicesResponse(
    @SerializedName("data") val data: List<FeeInvoice>,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("due_summary") val dueSummary: InvoiceDueSummary?
)

data class InvoiceDueSummary(
    @SerializedName("total_due") val totalDue: Double,
    @SerializedName("due_months_count") val dueMonthsCount: Int
)

// ─── Weekly Exams ─────────────────────────────────────────────────────────────

data class WeeklyExamMark(
    @SerializedName("id") val id: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("exam_date") val examDate: String,
    @SerializedName("marks_obtained") val marksObtained: Double,
    @SerializedName("max_marks") val maxMarks: Double,
    @SerializedName("percentage") val percentage: Double?,
    @SerializedName("remarks") val remarks: String?
)

data class WeeklyMarksResponse(
    @SerializedName("data") val data: List<WeeklyExamMark>,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("week_options") val weekOptions: List<WeekOption>?
)

data class WeekOption(
    @SerializedName("key") val key: String,
    @SerializedName("label") val label: String
)

data class WeeklyExamAssignment(
    @SerializedName("id") val id: Int,
    @SerializedName("exam_date") val examDate: String,
    @SerializedName("exam_name") val examName: String?,
    @SerializedName("subject") val subject: String,
    @SerializedName("teacher_name") val teacherName: String?
)

data class WeeklyExamSyllabus(
    @SerializedName("id") val id: Int,
    @SerializedName("week_start_date") val weekStartDate: String,
    @SerializedName("week_end_date") val weekEndDate: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("subject") val subject: String,
    @SerializedName("syllabus_details") val syllabusDetails: String?,
    @SerializedName("created_by_name") val createdByName: String?
)

// ─── Model Tests ──────────────────────────────────────────────────────────────

data class ModelTest(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("year") val year: String?
)

data class ModelTestResult(
    @SerializedName("id") val id: Int,
    @SerializedName("model_test_id") val modelTestId: Int,
    @SerializedName("test_name") val testName: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("subject") val subject: String,
    @SerializedName("test_set") val testSet: Int?,
    @SerializedName("optional_subject") val optionalSubject: Boolean,
    @SerializedName("mcq_mark") val mcqMark: Double?,
    @SerializedName("mcq_max") val mcqMax: Double?,
    @SerializedName("cq_mark") val cqMark: Double?,
    @SerializedName("cq_max") val cqMax: Double?,
    @SerializedName("practical_mark") val practicalMark: Double?,
    @SerializedName("practical_max") val practicalMax: Double?,
    @SerializedName("total_mark") val totalMark: Double,
    @SerializedName("grade") val grade: String,
    @SerializedName("grade_point") val gradePoint: Double
)

// ─── Routines ─────────────────────────────────────────────────────────────────

data class Routine(
    @SerializedName("id") val id: Int,
    @SerializedName("routine_date") val routineDate: String?,
    @SerializedName("time_slot") val timeSlot: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("teacher_name") val teacherName: String?
)

data class RoutinesResponse(
    @SerializedName("data") val data: List<Routine>,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("available_dates") val availableDates: List<String>?,
    @SerializedName("current_date") val currentDate: String?
)

// ─── Notices ──────────────────────────────────────────────────────────────────

data class StudentNotice(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("notice_date") val noticeDate: String,
    @SerializedName("is_acknowledged") val isAcknowledged: Boolean
)

data class PendingNoticeResponse(
    @SerializedName("notice") val notice: StudentNotice?
)

// ─── Study Materials ──────────────────────────────────────────────────────────

data class StudyMaterial(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("subject_label") val subjectLabel: String?,
    @SerializedName("class_level") val classLevel: String,
    @SerializedName("section") val section: String,
    @SerializedName("uploaded_by_name") val uploadedByName: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("mime_type") val mimeType: String?,
    @SerializedName("file_size_kb") val fileSizeKb: Int?,
    @SerializedName("download_url") val downloadUrl: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class StudyMaterialsResponse(
    @SerializedName("data") val data: List<StudyMaterial>,
    @SerializedName("meta") val meta: PaginationMeta?,
    @SerializedName("subject_options") val subjectOptions: List<SubjectOption>?
)

data class SubjectOption(
    @SerializedName("key") val key: String,
    @SerializedName("label") val label: String
)

// ─── Shared / Pagination ──────────────────────────────────────────────────────

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("from") val from: Int?,
    @SerializedName("to") val to: Int?
)

data class MessageResponse(
    @SerializedName("message") val message: String
)

data class ApiError(
    @SerializedName("message") val message: String,
    @SerializedName("errors") val errors: Map<String, List<String>>?
)

// Generic list wrappers
data class ListResponse<T>(
    @SerializedName("data") val data: List<T>,
    @SerializedName("meta") val meta: PaginationMeta?
)
