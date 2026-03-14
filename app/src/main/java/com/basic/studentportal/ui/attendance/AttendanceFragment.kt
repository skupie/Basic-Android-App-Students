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
import com.basic.studentportal.utils.animateCount
import com.basic.studentportal.utils.animatePercent
import com.basic.studentportal.utils.animateProgress
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceViewModel by viewModels()

    private var displayedMonth: YearMonth = YearMonth.now()
    private val apiFormatter   = DateTimeFormatter.ofPattern("yyyy-MM")
    private val labelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AttendanceAdapter()
        binding.recyclerAttendance.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAttendance.adapter = adapter

        displayedMonth = YearMonth.parse(viewModel.selectedMonth, apiFormatter)
        updateMonthLabel()

        binding.btnPrevMonth.setOnClickListener {
            displayedMonth = displayedMonth.minusMonths(1)
            updateMonthLabel()
            viewModel.filterByMonth(displayedMonth.format(apiFormatter))
        }
        binding.btnNextMonth.setOnClickListener {
            if (displayedMonth < YearMonth.now()) {
                displayedMonth = displayedMonth.plusMonths(1)
                updateMonthLabel()
                viewModel.filterByMonth(displayedMonth.format(apiFormatter))
            }
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        // ── Summary with animated numbers ──────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.cardSummary.visible()

                    is Resource.Success -> {
                        val s = state.data
                        binding.cardSummary.visible()
                        // Animate all counters
                        binding.tvPresent.animateCount(s.presentCount)
                        binding.tvAbsent.animateCount(s.absentCount)
                        binding.tvLate.animateCount(s.lateCount)
                        binding.tvTotalCount.animateCount(s.totalDays)
                        // Animate circular indicator + % label together
                        binding.tvPercent.animatePercent(to = s.attendancePercent)
                        binding.progressAttendance.animateProgress(s.attendancePercent.toInt())
                    }

                    is Resource.Error -> {
                        binding.cardSummary.visible()
                        listOf(binding.tvPresent, binding.tvAbsent,
                               binding.tvLate, binding.tvTotalCount).forEach { it.text = "—" }
                        binding.tvPercent.text = "—"
                        binding.progressAttendance.progress = 0
                    }
                }
            }
        }

        // ── Attendance list ────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendance.collect { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.swipeRefresh.isRefreshing = true
                        binding.tvEmpty.gone()
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        adapter.submitList(state.data.data)
                        if (state.data.data.isEmpty()) {
                            binding.tvEmpty.text = "No records for this month"
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

    private fun updateMonthLabel() {
        binding.tvCurrentMonth.text = displayedMonth.format(labelFormatter)
        val atCurrent = displayedMonth >= YearMonth.now()
        binding.btnNextMonth.alpha     = if (atCurrent) 0.3f else 1f
        binding.btnNextMonth.isEnabled = !atCurrent
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
