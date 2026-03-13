package com.basic.studentportal.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.basic.studentportal.R
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.data.model.Routine
import com.basic.studentportal.databinding.FragmentDashboardBinding
import com.basic.studentportal.utils.Resource
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

        // Time-based greeting
        binding.tvGreeting.text = getGreeting()

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

        // Quick access navigation
        binding.quickAttendance.setOnClickListener {
            findNavController().navigate(R.id.attendanceFragment)
        }
        binding.quickFees.setOnClickListener {
            findNavController().navigate(R.id.feesFragment)
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

        // Observe dashboard state
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
    }

    private fun showLoading(loading: Boolean) {
        binding.swipeRefresh.isRefreshing = loading
        if (loading) {
            binding.tvError.gone()
        }
    }

    private fun bindDashboard(data: DashboardResponse) {
        binding.scrollContent.visible()
        binding.tvError.gone()

        // ── Student Info ──────────────────────────────────────────────────────
        data.student?.let { student ->
            // Header greeting uses stored name; hero card shows class info
            binding.tvHeaderName.text = student.name
            binding.tvStudentName.text = buildString {
                val classLabel = student.classLevel
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercaseChar() }
                    }
                append(classLabel)
                append(" — ")
                append(student.section.replaceFirstChar { it.uppercaseChar() })
            }
            binding.tvAcademicYear.text = student.academicYear
                ?.let { "Academic Year $it" } ?: ""

            binding.chipStatus.text = "● ${student.status.uppercase()}"
            binding.chipStatus.setChipBackgroundColorResource(
                if (student.status == "active") R.color.status_active_bg
                else R.color.status_inactive_bg
            )
        }

        // ── Stats in hero card ─────────────────────────────────────────────
        // Attendance — from dashboard summary if provided; else show placeholder
        // Avg score — from weekly exam summary
        data.weeklyExamSummary?.let { exam ->
            binding.tvAvgScore.text = exam.averagePercent.toPercent()
        }

        // Due fees — from due summary
        data.dueSummary?.let { due ->
            binding.tvDueFees.text = if (due.dueAmount > 0)
                due.dueAmount.toCurrency()
            else "0 ৳"
        }

        // Attendance percent placeholder (not always in dashboard; show dash if missing)
        binding.tvAttendancePct.text = "—"

        // ── Due Alert ─────────────────────────────────────────────────────
        data.dueSummary?.let { due ->
            if (due.showDueAlert && due.dueAmount > 0) {
                binding.cardDueAlert.visible()
                binding.tvDueMessage.text =
                    if (due.dueMonthCount > 0) "${due.dueMonthCount} months pending • Tap to pay"
                    else due.dueAlertMessage ?: "Fee payment pending"
                binding.tvDueAmount.text = due.dueAmount.toCurrency()
                binding.btnDismissAlert.setOnClickListener {
                    viewModel.dismissDueAlert()
                    binding.cardDueAlert.gone()
                }
            } else {
                binding.cardDueAlert.gone()
            }
        } ?: binding.cardDueAlert.gone()

        // ── Today's Routine ───────────────────────────────────────────────
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

        // ── Exam Stats ────────────────────────────────────────────────────
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

        // ── Notes Summary ─────────────────────────────────────────────────
        data.notesSummary?.let { notes ->
            binding.tvNoteCount.text = "${notes.noteCount} materials"
            binding.tvLatestNote.text = notes.latestNoteTitle ?: "No materials yet"
            binding.tvNoteTeacher.text = notes.latestNoteTeacherName?.let { "by $it" } ?: ""
        }

        // ── Pending Notice ────────────────────────────────────────────────
        data.pendingNotice?.let { notice ->
            binding.cardNotice.visible()
            binding.tvNoticeTitle.text = notice.title
            binding.tvNoticeBody.text = notice.body
            binding.tvNoticeDate.text = notice.noticeDate
            binding.btnAcknowledge.setOnClickListener {
                binding.cardNotice.gone()
            }
            binding.btnCloseNotice.setOnClickListener {
                binding.cardNotice.gone()
            }
        } ?: binding.cardNotice.gone()
    }

    /** Dynamically builds a row per routine inside containerRoutines */
    private fun buildRoutineRows(routines: List<Routine>) {
        binding.containerRoutines.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        routines.forEach { routine ->
            // Outer card row
            val rowCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                background = requireContext().getDrawable(R.drawable.bg_card_dark)
                setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dpToPx(8)
                layoutParams = lp
            }

            // Time box (left pill)
            val timeBox = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                background = requireContext().getDrawable(R.drawable.bg_card_brand)
                setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dpToPx(12)
                layoutParams = lp
            }

            // Parse time slot "HH:MM-HH:MM" or "HH:MM" and show start/end stacked
            val parts = routine.timeSlot.split("-")
            val startTime = TextView(requireContext()).apply {
                text = parts.getOrNull(0)?.trim() ?: routine.timeSlot
                textSize = 11f
                setTextColor(resources.getColor(R.color.brand_light, null))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val endTime = TextView(requireContext()).apply {
                text = parts.getOrNull(1)?.trim() ?: ""
                textSize = 11f
                setTextColor(resources.getColor(R.color.brand_light, null))
            }
            timeBox.addView(startTime)
            if (endTime.text.isNotEmpty()) timeBox.addView(endTime)

            // Subject + teacher column
            val textCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = lp
            }
            val tvSubject = TextView(requireContext()).apply {
                text = routine.subject.replace("_", " ")
                    .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                textSize = 15f
                setTextColor(resources.getColor(R.color.text_primary, null))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val tvTeacher = TextView(requireContext()).apply {
                text = routine.teacherName ?: ""
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_secondary, null))
            }
            textCol.addView(tvSubject)
            textCol.addView(tvTeacher)

            // Status dot (right)
            val dot = View(requireContext()).apply {
                background = requireContext().getDrawable(R.drawable.ic_circle)
                backgroundTintList = resources.getColorStateList(R.color.brand_primary, null)
                val size = dpToPx(8)
                val lp = LinearLayout.LayoutParams(size, size)
                lp.marginStart = dpToPx(8)
                layoutParams = lp
            }

            rowCard.addView(timeBox)
            rowCard.addView(textCol)
            rowCard.addView(dot)

            binding.containerRoutines.addView(rowCard)
        }
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning 🌅"
            in 12..16 -> "Good Afternoon ☀️"
            in 17..20 -> "Good Evening 🌆"
            else -> "Good Night 🌙"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
