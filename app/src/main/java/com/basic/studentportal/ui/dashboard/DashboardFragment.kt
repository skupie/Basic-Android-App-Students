package com.basic.studentportal.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.basic.studentportal.R
import com.basic.studentportal.data.model.DashboardResponse
import com.basic.studentportal.databinding.FragmentDashboardBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toCurrency
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.visible
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

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

        // Student Info
        data.student?.let { student ->
            binding.tvStudentName.text = student.name
            binding.tvClassSection.text = "${student.classLevel.replace("_", " ").uppercase()} • ${student.section.uppercase()}"
            binding.chipStatus.text = student.status.uppercase()
            binding.chipStatus.setChipBackgroundColorResource(
                if (student.status == "active") R.color.status_active else R.color.status_inactive
            )
        }

        // Due Alert
        data.dueSummary?.let { due ->
            if (due.showDueAlert && due.dueAmount > 0) {
                binding.cardDueAlert.visible()
                binding.tvDueMessage.text = due.dueAlertMessage
                    ?: "You have ${due.dueMonthCount} month(s) due: ${due.dueAmount.toCurrency()}"
                binding.btnDismissAlert.setOnClickListener {
                    viewModel.dismissDueAlert()
                    binding.cardDueAlert.gone()
                }
            } else {
                binding.cardDueAlert.gone()
            }
        } ?: binding.cardDueAlert.gone()

        // Routine for today
        binding.tvRoutineDate.text = "Schedule for ${data.routineDate ?: "Today"}"
        if (data.todayRoutines.isEmpty()) {
            binding.tvNoRoutine.visible()
            binding.chipGroupRoutines.gone()
        } else {
            binding.tvNoRoutine.gone()
            binding.chipGroupRoutines.visible()
            binding.chipGroupRoutines.removeAllViews()
            data.todayRoutines.forEach { routine ->
                val chip = Chip(requireContext()).apply {
                    text = "${routine.timeSlot}\n${routine.subject.replace("_", " ").uppercase()}"
                    isCheckable = false
                }
                binding.chipGroupRoutines.addView(chip)
            }
        }

        // Weekly Exam Stats
        data.weeklyExamSummary?.let { exam ->
            binding.tvExamCount.text = exam.examCount.toString()
            binding.tvAvgPercent.text = exam.averagePercent.toPercent()
            binding.tvPerformanceLabel.text = exam.performanceLabel
            exam.trendDelta?.let { delta ->
                val sign = if (delta >= 0) "▲" else "▼"
                binding.tvTrendDelta.text = "$sign ${Math.abs(delta).toPercent()}"
                binding.tvTrendDelta.setTextColor(
                    resources.getColor(if (delta >= 0) R.color.status_active else R.color.status_inactive, null)
                )
                binding.tvTrendDelta.visible()
            } ?: binding.tvTrendDelta.gone()
        }

        // Notes Summary
        data.notesSummary?.let { notes ->
            binding.tvNoteCount.text = "${notes.noteCount} materials"
            binding.tvLatestNote.text = notes.latestNoteTitle ?: "No materials yet"
            binding.tvNoteTeacher.text = notes.latestNoteTeacherName?.let { "by $it" } ?: ""
        }

        // Pending Notice Dialog
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
