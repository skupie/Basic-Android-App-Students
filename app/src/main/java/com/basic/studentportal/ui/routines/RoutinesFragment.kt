package com.basic.studentportal.ui.routines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.data.model.Routine
import com.basic.studentportal.data.model.RoutinesResponse
import com.basic.studentportal.data.repository.RoutineRepository
import com.basic.studentportal.databinding.FragmentRoutinesBinding
import com.basic.studentportal.databinding.ItemRoutineBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toSubjectLabel
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class RoutinesViewModel @Inject constructor(private val repository: RoutineRepository) : ViewModel() {

    private val _routines = MutableStateFlow<Resource<RoutinesResponse>>(Resource.Loading)
    val routines: StateFlow<Resource<RoutinesResponse>> = _routines

    var selectedDate: String? = null

    init { loadRoutines() }

    fun loadRoutines(date: String? = selectedDate) {
        viewModelScope.launch {
            _routines.value = Resource.Loading
            _routines.value = repository.getRoutines(date)
        }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class RoutineAdapter : ListAdapter<Routine, RoutineAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemRoutineBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Routine) {
            b.tvTimeSlot.text = item.timeSlot
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvTeacher.text = item.teacherName?.let { "👨‍🏫 $it" } ?: ""
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    class DiffCb : DiffUtil.ItemCallback<Routine>() {
        override fun areItemsTheSame(a: Routine, b: Routine) = a.id == b.id
        override fun areContentsTheSame(a: Routine, b: Routine) = a == b
    }
}

// ─── Fragment ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class RoutinesFragment : Fragment() {

    private var _binding: FragmentRoutinesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RoutinesViewModel by viewModels()
    private val adapter = RoutineAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerRoutines.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadRoutines() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.routines.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.tvCurrentDate.text = "Schedule: ${state.data.currentDate ?: "Today"}"
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
