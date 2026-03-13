package com.basic.studentportal.ui.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.basic.studentportal.databinding.FragmentAttendanceBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceViewModel by viewModels()

    // Tracks the month currently displayed in the picker
    private var displayedMonth: YearMonth = YearMonth.now()
    private val apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    private val labelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── RecyclerView setup (was missing LayoutManager — caused blank list) ──
        val adapter = AttendanceAdapter()
        binding.recyclerAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAttendance.adapter = adapter

        // ── Initialise month picker to the ViewModel's current selection ──────
        displayedMonth = YearMonth.parse(viewModel.selectedMonth, apiFormatter)
        updateMonthLabel()

        // ── Month navigation ──────────────────────────────────────────────────
        binding.btnPrevMonth.setOnClickListener {
            displayedMonth = displayedMonth.minusMonths(1)
            updateMonthLabel()
            viewModel.filterByMonth(displayedMonth.format(apiFormatter))
        }

        binding.btnNextMonth.setOnClickListener {
            // Don't allow navigating beyond the current month
            if (displayedMonth < YearMonth.now()) {
                displayedMonth = displayedMonth.plusMonths(1)
                updateMonthLabel()
                viewModel.filterByMonth(displayedMonth.format(apiFormatter))
            }
        }

        // ── Swipe refresh ─────────────────────────────────────────────────────
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        // ── Summary (stats card) ──────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { state ->
                when (state) {
                    is Resource.Loading -> {
                        // Keep card visible with dash placeholders while loading
                        binding.cardSummary.visible()
                    }
                    is Resource.Success -> {
                        val s = state.data
                        binding.cardSummary.visible()
                        binding.tvPresent.text  = s.presentCount.toString()
                        binding.tvAbsent.text   = s.absentCount.toString()
                        binding.tvLate.text     = s.lateCount.toString()
                        // FIX: bind total days which was never set before
                        binding.tvTotalCount.text = s.totalDays.toString()
                        binding.tvPercent.text  = s.attendancePercent.toPercent()
                        binding.progressAttendance.progress = s.attendancePercent.toInt()
                    }
                    is Resource.Error -> {
                        // Keep card visible with dashes so the UI doesn't look broken
                        binding.cardSummary.visible()
                        binding.tvPresent.text  = "—"
                        binding.tvAbsent.text   = "—"
                        binding.tvLate.text     = "—"
                        binding.tvTotalCount.text = "—"
                        binding.tvPercent.text  = "—"
                        binding.progressAttendance.progress = 0
                    }
                }
            }
        }

        // ── Attendance list ───────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendance.collect { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.swipeRefresh.isRefreshing = true
                        binding.tvEmpty.gone()
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val records = state.data.data
                        adapter.submitList(records)
                        if (records.isEmpty()) {
                            binding.tvEmpty.text = "No attendance records for this month"
                            binding.tvEmpty.visible()
                        } else {
                            binding.tvEmpty.gone()
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.tvEmpty.text = state.message
                        binding.tvEmpty.visible()
                    }
                }
            }
        }
    }

    /** Updates the month label and enables/disables the next-month arrow */
    private fun updateMonthLabel() {
        binding.tvCurrentMonth.text = displayedMonth.format(labelFormatter)
        // Gray out the "›" arrow when we're already on the current month
        val isCurrentMonth = displayedMonth >= YearMonth.now()
        binding.btnNextMonth.alpha = if (isCurrentMonth) 0.3f else 1.0f
        binding.btnNextMonth.isEnabled = !isCurrentMonth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
