package com.basic.studentportal.ui.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.basic.studentportal.databinding.FragmentAttendanceBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private var _binding: FragmentAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceViewModel by viewModels()
    private lateinit var adapter: AttendanceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AttendanceAdapter()
        binding.recyclerAttendance.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        val s = state.data
                        binding.tvPresent.text = s.presentCount.toString()
                        binding.tvAbsent.text = s.absentCount.toString()
                        binding.tvLate.text = s.lateCount.toString()
                        binding.tvPercent.text = s.attendancePercent.toPercent()
                        binding.progressAttendance.progress = s.attendancePercent.toInt()
                        binding.cardSummary.visible()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.attendance.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        adapter.submitList(state.data.data)
                        if (state.data.data.isEmpty()) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
