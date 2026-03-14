package com.basic.studentportal.ui.dashboard

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

        // Quick access navigation
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

        // ── Attendance percentage — fetched separately, shown in hero card ────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendanceSummary.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        binding.tvAttendancePct.text = state.data.attendancePercent.toPercent()
                    }
                    is Resource.Loading -> binding.tvAttendancePct.text = "—"
                    is Resource.Error   -> binding.tvAttendancePct.text = "—"
                }
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
                binding.tvEnrollmentDate.visibility = android.view.View.VISIBLE
            } else {
                binding.tvEnrollmentDate.visibility = android.view.View.GONE
            }

            binding.chipStatus.text = "● ${student.status.uppercase()}"
            binding.chipStatus.setChipBackgroundColorResource(
                if (student.status == "active") R.color.status_active_bg
                else R.color.status_inactive_bg
            )
        }

        // ── Hero card stats ───────────────────────────────────────────────────
        // tvAttendancePct is populated by the attendanceSummary collector above

        data.weeklyExamSummary?.let { exam ->
            binding.tvAvgScore.text = exam.averagePercent.toPercent()
        }

        data.dueSummary?.let { due ->
            binding.tvDueFees.text = if (due.dueAmount > 0) due.dueAmount.toCurrency() else "0 ৳"
        }

        // ── Due Alert ─────────────────────────────────────────────────────────
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

        // ── Pending Notice ────────────────────────────────────────────────────
        data.pendingNotice?.let { notice ->
            binding.cardNotice.visible()
            binding.tvNoticeTitle.text = notice.title
            binding.tvNoticeBody.text = notice.body
            binding.tvNoticeDate.text = notice.noticeDate
            binding.btnAcknowledge.setOnClickListener { binding.cardNotice.gone() }
            binding.btnCloseNotice.setOnClickListener { binding.cardNotice.gone() }
        } ?: binding.cardNotice.gone()
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
