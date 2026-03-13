package com.basic.studentportal.ui.routines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
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
            selectedDate = date
            _routines.value = Resource.Loading
            _routines.value = repository.getRoutines(date)
        }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

// Subject → accent color map (bg drawable + text color)
private fun subjectAccentBg(subject: String): Int = when {
    subject.contains("math", true) -> R.drawable.bg_card_brand
    subject.contains("physics", true) -> R.drawable.bg_card_brand
    subject.contains("english", true) -> R.drawable.bg_card_success
    subject.contains("biology", true) -> R.drawable.bg_card_success
    subject.contains("chemistry", true) -> R.drawable.bg_card_amber
    subject.contains("history", true) || subject.contains("social", true) -> R.drawable.bg_card_warning
    else -> R.drawable.bg_card_dark
}

private fun subjectAccentColor(subject: String): Int = when {
    subject.contains("math", true) -> R.color.brand_light
    subject.contains("physics", true) -> R.color.brand_light
    subject.contains("english", true) -> R.color.success
    subject.contains("biology", true) -> R.color.success
    subject.contains("chemistry", true) -> R.color.grade_average
    else -> R.color.brand_light
}

class RoutineAdapter : ListAdapter<Routine, RoutineAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemRoutineBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Routine) {
            val ctx = b.root.context
            val subject = item.subject

            b.tvSubject.text = subject.toSubjectLabel()
            b.tvTeacher.text = item.teacherName?.let { "🏫 $it" } ?: ""

            // Time slot: may be "08:00 AM - 09:00 AM" or similar
            // Left label shows just the start time
            val parts = item.timeSlot.split("-")
            b.tvTimeSlot.text = parts.firstOrNull()?.trim() ?: item.timeSlot
            b.tvTimeLabel.text = item.timeSlot

            // Accent colors per subject
            val accentBg = subjectAccentBg(subject)
            val accentColor = ContextCompat.getColor(ctx, subjectAccentColor(subject))
            b.cardRoutine.background = ContextCompat.getDrawable(ctx, accentBg)
            b.viewAccentLine.setBackgroundColor(accentColor)
            b.tvTimeLabel.setTextColor(accentColor)
            b.tvTimeSlot.setTextColor(accentColor)
            b.viewTimelineDot.setBackgroundColor(accentColor)
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

    private var availableDates: List<String> = emptyList()
    private var dateIndex: Int = -1   // -1 = "today" (null date)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutinesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerRoutines.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRoutines.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadRoutines(viewModel.selectedDate) }

        binding.btnPrevDay.setOnClickListener {
            if (availableDates.isNotEmpty()) {
                val cur = if (dateIndex < 0) availableDates.size - 1 else dateIndex
                val prev = (cur - 1).coerceAtLeast(0)
                dateIndex = prev
                viewModel.loadRoutines(availableDates[prev])
            }
        }

        binding.btnNextDay.setOnClickListener {
            if (availableDates.isNotEmpty()) {
                val cur = if (dateIndex < 0) availableDates.size - 1 else dateIndex
                val next = (cur + 1).coerceAtMost(availableDates.size - 1)
                dateIndex = next
                viewModel.loadRoutines(availableDates[next])
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.routines.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = state.data

                        availableDates = data.availableDates ?: emptyList()

                        // Date display
                        binding.tvCurrentDate.text = data.currentDate ?: "Today"
                        val count = data.data.size
                        binding.tvClassCount.text = if (count > 0) "$count class${if (count != 1) "es" else ""} today" else ""

                        adapter.submitList(data.data)

                        if (data.data.isEmpty()) binding.tvEmpty.visible()
                        else binding.tvEmpty.gone()
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
