package com.basic.studentportal.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.basic.studentportal.R
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.model.Routine
import com.basic.studentportal.databinding.FragmentDashboardBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.animatePercent
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toCurrency
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvGreeting.text = getGreeting()
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

        binding.btnNotification.setOnClickListener {
            findNavController().navigate(R.id.noticesFragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        binding.quickAttendance.setOnClickListener {
            findNavController().navigate(R.id.attendanceFragment)
        }
        binding.quickSchedule.setOnClickListener {
            findNavController().navigate(R.id.routinesFragment)
        }
        binding.quickResults.setOnClickListener {
            findNavController().navigate(R.id.examsFragment)
        }
        binding.quickMaterials.setOnClickListener {
            findNavController().navigate(R.id.studyMaterialsFragment)
        }
        binding.tvViewAllRoutines.setOnClickListener {
            findNavController().navigate(R.id.routinesFragment)
        }
        binding.tvViewAllMaterials.setOnClickListener {
            findNavController().navigate(R.id.studyMaterialsFragment)
        }

        // ── Dashboard data ────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboard.collect { state ->
                when (state) {
                    is Resource.Loading -> showLoading(true)
                    is Resource.Success -> {
                        showLoading(false)
                        bindDashboard(state.data)
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        binding.tvError.text = state.message
                        binding.tvError.visible()
                        binding.scrollContent.gone()
                    }
                }
            }
        }

        // ── Attendance percentage ─────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendanceSummary.collect { state ->
                when (state) {
                    is Resource.Success ->
                        binding.tvAttendancePct.animatePercent(to = state.data.attendancePercent)
                    is Resource.Loading -> binding.tvAttendancePct.text = "—"
                    is Resource.Error   -> binding.tvAttendancePct.text = "—"
                }
            }
        }

        // ── Unread notice badge ───────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.unreadNoticeCount.collect { count ->
                if (count > 0) binding.dotNotification.visible()
                else binding.dotNotification.gone()
            }
        }

        // ── Due alert dialog ──────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showDueAlert.collect { alertData ->
                showDueAlertDialog(alertData.dueMonthCount, alertData.totalDue)
            }
        }

        // ── Notice alert dialog ───────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showNoticeAlert.collect { noticeData ->
                showNoticeAlertDialog(noticeData)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.swipeRefresh.isRefreshing = loading
        if (loading) binding.tvError.gone()
    }

    private fun bindDashboard(data: DashboardResponse) {
        binding.scrollContent.visible()
        binding.tvError.gone()

        // ── Student Info ──────────────────────────────────────────────────────
        data.student?.let { student ->
            binding.tvHeaderName.text = student.name
            binding.tvStudentName.text = buildString {
                val classLabel = student.classLevel
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
                append(classLabel)
                append(" — ")
                append(student.section.replaceFirstChar { it.uppercaseChar() })
            }
            binding.tvAcademicYear.text = student.academicYear
                ?.let { "Academic Year $it" } ?: ""

            if (!student.enrollmentDate.isNullOrBlank()) {
                binding.tvEnrollmentDate.text = "📅 Admitted: ${student.enrollmentDate}"
                binding.tvEnrollmentDate.visibility = View.VISIBLE
            } else {
                binding.tvEnrollmentDate.visibility = View.GONE
            }

            binding.chipStatus.text = "● ${student.status.uppercase()}"
            binding.chipStatus.setChipBackgroundColorResource(
                if (student.status == "active") R.color.status_active_bg
                else R.color.status_inactive_bg
            )
        }

        // ── Hero card stats ───────────────────────────────────────────────────
        data.weeklyExamSummary?.let { exam ->
            binding.tvAvgScore.animatePercent(to = exam.averagePercent)
        }

        data.dueSummary?.let { due ->
            val monthlyFee = data.student?.monthlyFee ?: 0.0
            val calculatedDue = due.dueMonthCount * monthlyFee
            binding.tvDueFees.text = if (calculatedDue > 0) calculatedDue.toCurrency() else "0 ৳"
        }
        binding.cardDueAlert.gone()

        // ── Today's Classes ───────────────────────────────────────────────────
        binding.tvRoutineDate.text = if (!data.routineDate.isNullOrBlank())
            "TODAY'S CLASSES · ${data.routineDate}"
        else "TODAY'S CLASSES"

        if (data.todayRoutines.isEmpty()) {
            binding.tvNoRoutine.visible()
            binding.containerRoutines.gone()
        } else {
            binding.tvNoRoutine.gone()
            binding.containerRoutines.visible()
            buildRoutineRows(data.todayRoutines)
        }

        // ── Exam Performance ──────────────────────────────────────────────────
        data.weeklyExamSummary?.let { exam ->
            binding.tvExamCount.text = exam.examCount.toString()
            binding.tvAvgPercent.text = exam.averagePercent.toPercent()
            binding.tvPerformanceLabel.text = exam.performanceLabel
            exam.trendDelta?.let { delta ->
                val sign = if (delta >= 0) "▲" else "▼"
                binding.tvTrendDelta.text = "$sign ${Math.abs(delta).toPercent()}"
                binding.tvTrendDelta.setTextColor(
                    resources.getColor(
                        if (delta >= 0) R.color.status_active else R.color.status_inactive,
                        null
                    )
                )
                binding.tvTrendDelta.visible()
            } ?: binding.tvTrendDelta.gone()
        }

        // ── Study Materials ───────────────────────────────────────────────────
        data.notesSummary?.let { notes ->
            binding.tvNoteCount.text = "${notes.noteCount} materials"
            binding.tvLatestNote.text = notes.latestNoteTitle ?: "No materials yet"
            binding.tvNoteTeacher.text = notes.latestNoteTeacherName?.let { "by $it" } ?: ""
        }

        // Inline notice card is no longer used — notices are shown as a popup dialog
        binding.cardNotice.gone()
    }

    // ── Bengali digit helper ──────────────────────────────────────────────────

    private fun toBengaliDigits(number: Long): String {
        val d = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch -> if (ch.isDigit()) d[ch.digitToInt()] else ch }.joinToString("")
    }

    // ── Due Alert Dialog ──────────────────────────────────────────────────────

    private fun showDueAlertDialog(dueMonthCount: Int, totalDue: Double) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_due_alert, null)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val bodyText = "প্রিয় শিক্ষার্থী, আপনার ${toBengaliDigits(dueMonthCount.toLong())} মাসের বকেয়া জমা হয়েছে " +
                "${toBengaliDigits(Math.round(totalDue))} টাকা। অনুগ্রহ করে বকেয়াটি আগামী ২ কর্ম দিবসের মধ্যে পরিশোধ করুন।"

        dialogView.findViewById<TextView>(R.id.tvDialogBody).text = bodyText
        dialogView.findViewById<TextView>(R.id.tvDialogAmount).text = totalDue.toCurrency()
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogOk).setOnClickListener {
            viewModel.dismissDueAlert(dueMonthCount)
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── Notice Alert Dialog ───────────────────────────────────────────────────

    private fun showNoticeAlertDialog(noticeData: DashboardViewModel.NoticeAlertData) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val dialogView = layoutInflater.inflate(R.layout.dialog_due_alert, null)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialogView.findViewById<TextView>(R.id.tvDialogBody).text = noticeData.body
        dialogView.findViewById<TextView>(R.id.tvDialogAmount).text = noticeData.title
        dialogView.findViewById<android.widget.Button>(R.id.btnDialogOk).setOnClickListener {
            viewModel.acknowledgeNotice(noticeData.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun buildRoutineRows(routines: List<Routine>) {
        binding.containerRoutines.removeAllViews()
        routines.forEach { routine ->
            val rowCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = requireContext().getDrawable(R.drawable.bg_card_dark)
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = dpToPx(8) }
            }

            val timeBox = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                background = requireContext().getDrawable(R.drawable.bg_card_brand)
                setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = dpToPx(12) }
            }

            val parts = routine.timeSlot.split("-")
            timeBox.addView(TextView(requireContext()).apply {
                text = parts.getOrNull(0)?.trim() ?: routine.timeSlot
                textSize = 11f
                setTextColor(resources.getColor(R.color.brand_light, null))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            val endText = parts.getOrNull(1)?.trim() ?: ""
            if (endText.isNotEmpty()) {
                timeBox.addView(TextView(requireContext()).apply {
                    text = endText
                    textSize = 11f
                    setTextColor(resources.getColor(R.color.brand_light, null))
                })
            }

            val textCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(requireContext()).apply {
                text = routine.subject.replace("_", " ")
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                textSize = 15f
                setTextColor(resources.getColor(R.color.text_primary, null))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            textCol.addView(TextView(requireContext()).apply {
                text = routine.teacherName ?: ""
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_secondary, null))
            })

            val dot = View(requireContext()).apply {
                background = requireContext().getDrawable(R.drawable.ic_circle)
                backgroundTintList = resources.getColorStateList(R.color.brand_primary, null)
                val size = dpToPx(8)
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginStart = dpToPx(8) }
            }

            rowCard.addView(timeBox)
            rowCard.addView(textCol)
            rowCard.addView(dot)
            binding.containerRoutines.addView(rowCard)
        }
    }

    private fun getGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11  -> "Good Morning 🌅"
        in 12..16 -> "Good Afternoon ☀️"
        in 17..20 -> "Good Evening 🌆"
        else      -> "Good Night 🌙"
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
