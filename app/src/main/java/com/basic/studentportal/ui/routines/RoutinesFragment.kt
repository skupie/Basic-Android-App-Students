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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

private fun getEffectiveRoutineDate(): LocalDate {
    val now = LocalTime.now()
    val today = LocalDate.now()
    return if (!now.isBefore(LocalTime.of(18, 0))) today.plusDays(1) else today
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class RoutinesViewModel @Inject constructor(private val repository: RoutineRepository) : ViewModel() {

    private val _routines = MutableStateFlow<Resource<RoutinesResponse>>(Resource.Loading)
    val routines: StateFlow<Resource<RoutinesResponse>> = _routines

    // The date string last requested — kept so swipe-to-refresh reloads the same date
    var lastRequestedDate: String = getEffectiveRoutineDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        private set

    init { loadRoutines(lastRequestedDate) }

    fun loadRoutines(date: String) {
        lastRequestedDate = date
        viewModelScope.launch {
            _routines.value = Resource.Loading
            _routines.value = repository.getRoutines(date)
        }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

private fun subjectAccentBg(subject: String): Int = when {
    subject.contains("math", true)       -> R.drawable.bg_card_brand
    subject.contains("physics", true)    -> R.drawable.bg_card_brand
    subject.contains("english", true)    -> R.drawable.bg_card_success
    subject.contains("biology", true)    -> R.drawable.bg_card_success
    subject.contains("chemistry", true)  -> R.drawable.bg_card_amber
    subject.contains("history", true)
        || subject.contains("social", true) -> R.drawable.bg_card_warning
    else -> R.drawable.bg_card_dark
}

private fun subjectAccentColor(subject: String): Int = when {
    subject.contains("math", true)       -> R.color.brand_light
    subject.contains("physics", true)    -> R.color.brand_light
    subject.contains("english", true)    -> R.color.success
    subject.contains("biology", true)    -> R.color.success
    subject.contains("chemistry", true)  -> R.color.grade_average
    else                                 -> R.color.brand_light
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

            val accentBg    = subjectAccentBg(item.subject)
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

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Single source of truth for which date is displayed.
     * This is updated IMMEDIATELY when an arrow is pressed — the header changes
     * right away, without waiting for the API response to come back.
     * We never read the date back from the API response (server echoes are unreliable).
     */
    private var displayedDate: LocalDate = getEffectiveRoutineDate()

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

        // Show correct header immediately — don't wait for API
        updateDateHeader()

        // Swipe refresh reloads whatever date is currently displayed
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRoutines(displayedDate.format(dateFmt))
        }

        // ── Prev: always step exactly 1 calendar day backward ────────────────
        binding.btnPrevDay.setOnClickListener {
            displayedDate = displayedDate.minusDays(1)
            updateDateHeader()                              // immediate header update
            viewModel.loadRoutines(displayedDate.format(dateFmt))
        }

        // ── Next: step 1 calendar day forward, but never past effective date ─
        binding.btnNextDay.setOnClickListener {
            val next = displayedDate.plusDays(1)
            if (!next.isAfter(getEffectiveRoutineDate())) {
                displayedDate = next
                updateDateHeader()                          // immediate header update
                viewModel.loadRoutines(displayedDate.format(dateFmt))
            }
        }

        // ── Collect API results ───────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.routines.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true

                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false

                        val count = state.data.data.size
                        binding.tvClassCount.text = when (count) {
                            0    -> "No classes scheduled"
                            1    -> "1 class"
                            else -> "$count classes"
                        }

                        adapter.submitList(state.data.data)

                        if (state.data.data.isEmpty()) binding.tvEmpty.visible()
                        else binding.tvEmpty.gone()
                    }

                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.tvClassCount.text = ""
                        binding.tvEmpty.text = state.message
                        binding.tvEmpty.visible()
                    }
                }
            }
        }
    }

    /**
     * Refreshes the date header label and arrow enabled/alpha state
     * from [displayedDate]. Called immediately on each arrow click.
     */
    private fun updateDateHeader() {
        binding.tvCurrentDate.text = formatDate(displayedDate)

        // Next arrow dimmed/disabled when already at latest available routine day
        val atLatestAvailableDay = !displayedDate.isBefore(getEffectiveRoutineDate())
        binding.btnNextDay.alpha     = if (atLatestAvailableDay) 0.3f else 1f
        binding.btnNextDay.isEnabled = !atLatestAvailableDay

        // Prev arrow always available
        binding.btnPrevDay.alpha     = 1f
        binding.btnPrevDay.isEnabled = true
    }

    /** LocalDate → "Saturday, 14 March 2026" */
    private fun formatDate(date: LocalDate): String {
        val day   = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "$day, ${date.dayOfMonth} $month ${date.year}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
