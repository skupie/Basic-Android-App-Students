package com.basic.studentportal.ui.dashboard

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.basic.studentportal.R
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.model.Routine
import com.basic.studentportal.data.model.RoutinesResponse
import com.basic.studentportal.data.model.StudentNotice
import com.basic.studentportal.databinding.FragmentDashboardBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.animatePercent
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toCurrency
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
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

        // ── Routine preview ───────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.routinePreview.collect { state ->
                bindRoutinePreview(state, viewModel.routinePreviewDate.value)
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
        // Loaded separately from RoutineRepository so we can show the next day's
        // routine automatically after 6 PM.

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

        // ── Pending Notice — shown as scrollable dialog ───────────────────────
        binding.cardNotice.gone()
        data.pendingNotice?.let { notice ->
            if (!notice.isAcknowledged) showNoticeDialog(notice)
        }
    }

    private fun bindRoutinePreview(
        state: Resource<RoutinesResponse>,
        requestedDate: String
    ) {
        binding.tvRoutineDate.text =
            if (!LocalTime.now().isBefore(LocalTime.of(18, 0))) {
                "TOMORROW'S CLASSES · $requestedDate"
            } else {
                "TODAY'S CLASSES · $requestedDate"
            }

        when (state) {
            is Resource.Loading -> {
                // Keep previous routine preview while reloading
            }

            is Resource.Success -> {
                if (state.data.data.isEmpty()) {
                    binding.tvNoRoutine.text = "No classes scheduled"
                    binding.tvNoRoutine.visible()
                    binding.containerRoutines.gone()
                } else {
                    binding.tvNoRoutine.gone()
                    binding.containerRoutines.visible()
                    buildRoutineRows(state.data.data)
                }
            }

            is Resource.Error -> {
                binding.tvNoRoutine.text = state.message
                binding.tvNoRoutine.visible()
                binding.containerRoutines.gone()
            }
        }
    }

    // ── Bengali digit helper ──────────────────────────────────────────────────

    private fun toBengaliDigits(number: Long): String {
        val d = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch -> if (ch.isDigit()) d[ch.digitToInt()] else ch }.joinToString("")
    }

    private fun fromHtml(html: String): CharSequence =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        else
            @Suppress("DEPRECATION") Html.fromHtml(html)

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
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
        }

        val bodyText = "প্রিয় শিক্ষার্থী, আপনার ${toBengaliDigits(dueMonthCount.toLong())} মাসের বকেয়া জমা হয়েছে " +
                "${toBengaliDigits(Math.round(totalDue))} টাকা। অনুগ্রহ করে বকেয়াটি আগামী ২ কর্ম দিবসের মধ্যে পরিশোধ করুন।"

        dialogView.findViewById<TextView>(R.id.tvDialogBody).text = bodyText
        dialogView.findViewById<TextView>(R.id.tvDialogAmount).text = totalDue.toCurrency()
        dialogView.findViewById<Button>(R.id.btnDialogOk).setOnClickListener {
            viewModel.dismissDueAlert(dueMonthCount)
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── Pending Notice Dialog ─────────────────────────────────────────────────

    private fun showNoticeDialog(notice: StudentNotice) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Outer card container
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialog_due_card)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(20), dpToPx(18), dpToPx(16), dpToPx(18))
            setBackgroundColor(resources.getColor(R.color.brand_primary, null))
        }
        header.addView(TextView(requireContext()).apply {
            text = "📢  নোটিশ"
            textSize = 15f
            setTextColor(resources.getColor(android.R.color.white, null))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })

        // Scrollable body
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            isVerticalScrollBarEnabled = true
        }

        val body = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(12))
        }

        // Title
        body.addView(TextView(requireContext()).apply {
            text = notice.title
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, null))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
        })

        // Body — HTML rendered
        body.addView(TextView(requireContext()).apply {
            text = fromHtml(notice.body)
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
        })

        // Date
        body.addView(TextView(requireContext()).apply {
            text = "📅 ${notice.noticeDate}"
            textSize = 11f
            setTextColor(resources.getColor(R.color.text_hint, null))
        })

        scrollView.addView(body)

        // Footer with button — always visible
        val footer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(20))
        }

        footer.addView(Button(requireContext()).apply {
            text = "✓  বুঝেছি"
            setBackgroundResource(R.drawable.bg_btn_primary)
            setTextColor(resources.getColor(android.R.color.white, null))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(48)
            )
            setOnClickListener { dialog.dismiss() }
        })

        container.addView(header)
        container.addView(scrollView)
        container.addView(footer)

        dialog.setContentView(container)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )
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
