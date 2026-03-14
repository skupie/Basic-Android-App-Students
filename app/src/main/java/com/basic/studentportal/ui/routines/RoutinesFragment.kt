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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class RoutinesViewModel @Inject constructor(private val repository: RoutineRepository) : ViewModel() {

    private val _routines = MutableStateFlow<Resource<RoutinesResponse>>(Resource.Loading)
    val routines: StateFlow<Resource<RoutinesResponse>> = _routines

    var selectedDate: String? = null

    init {
        // Pass today's date explicitly — null lets the server return the whole week
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        loadRoutines(todayStr)
    }

    fun loadRoutines(date: String? = selectedDate) {
        viewModelScope.launch {
            selectedDate = date
            _routines.value = Resource.Loading
            _routines.value = repository.getRoutines(date)
        }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

private fun subjectAccentBg(subject: String): Int = when {
    subject.contains("math", true)      -> R.drawable.bg_card_brand
    subject.contains("physics", true)   -> R.drawable.bg_card_brand
    subject.contains("english", true)   -> R.drawable.bg_card_success
    subject.contains("biology", true)   -> R.drawable.bg_card_success
    subject.contains("chemistry", true) -> R.drawable.bg_card_amber
    subject.contains("history", true)
            || subject.contains("social", true) -> R.drawable.bg_card_warning
    else -> R.drawable.bg_card_dark
}

private fun subjectAccentColor(subject: String): Int = when {
    subject.contains("math", true)      -> R.color.brand_light
    subject.contains("physics", true)   -> R.color.brand_light
    subject.contains("english", true)   -> R.color.success
    subject.contains("biology", true)   -> R.color.success
    subject.contains("chemistry", true) -> R.color.grade_average
    else                                -> R.color.brand_light
}

class RoutineAdapter : ListAdapter<Routine, RoutineAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemRoutineBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Routine) {
            val ctx = b.root.context
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvTeacher.text = item.teacherName?.let { "🏫 $it" } ?: ""

            val parts = item.timeSlot.split("-")
            b.tvTimeSlot.text = parts.firstOrNull()?.trim() ?: item.timeSlot
            b.tvTimeLabel.text = item.timeSlot

            val accentBg = subjectAccentBg(item.subject)
            val accentColor = ContextCompat.getColor(ctx, subjectAccentColor(item.subject))
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

    // Date navigation — populated from API's available_dates list
    private var availableDates: List<String> = emptyList()
    private var currentIndex: Int = -1

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

        // Refresh current date's classes
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRoutines(availableDates.getOrNull(currentIndex))
        }

        // ── Arrow navigation ─────────────────────────────────────────────────
        binding.btnPrevDay.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                viewModel.loadRoutines(availableDates[currentIndex])
            }
        }

        binding.btnNextDay.setOnClickListener {
            if (currentIndex < availableDates.size - 1) {
                currentIndex++
                viewModel.loadRoutines(availableDates[currentIndex])
            }
        }

        // ── Collect ──────────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.routines.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true

                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = state.data

                        // On first successful load, build the dates list and pin to today
                        if (availableDates.isEmpty() && !data.availableDates.isNullOrEmpty()) {
                            availableDates = data.availableDates

                            val todayStr = LocalDate.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            currentIndex = availableDates.indexOf(todayStr)
                            if (currentIndex < 0) currentIndex = availableDates.size - 1
                        }

                        // Header: prefer server echo, fall back to what we requested
                        val displayDate = data.currentDate
                            ?: viewModel.selectedDate
                            ?: availableDates.getOrNull(currentIndex)
                        binding.tvCurrentDate.text = formatDateLabel(displayDate)

                        val count = data.data.size
                        binding.tvClassCount.text = when {
                            count == 0 -> "No classes today"
                            count == 1 -> "1 class today"
                            else -> "$count classes today"
                        }

                        // Dim arrows at boundaries
                        binding.btnPrevDay.alpha = if (currentIndex > 0) 1f else 0.3f
                        binding.btnNextDay.alpha =
                            if (currentIndex < availableDates.size - 1) 1f else 0.3f

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

    /** "2026-03-14" → "Saturday, 14 March 2026" */
    private fun formatDateLabel(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Today"
        return try {
            val d = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val day   = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val month = d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            "$day, ${d.dayOfMonth} $month ${d.year}"
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
