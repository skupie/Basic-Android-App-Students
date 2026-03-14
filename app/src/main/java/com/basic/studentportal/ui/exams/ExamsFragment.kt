package com.basic.studentportal.ui.exams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
import com.basic.studentportal.data.model.ModelTest
import com.basic.studentportal.data.model.ModelTestResult
import com.basic.studentportal.data.model.WeeklyExamAssignment
import com.basic.studentportal.data.model.WeeklyExamMark
import com.basic.studentportal.data.model.WeeklyExamSyllabus
import com.basic.studentportal.data.model.WeekOption
import com.basic.studentportal.databinding.FragmentExamsBinding
import com.basic.studentportal.databinding.ItemExamRoutineBinding
import com.basic.studentportal.databinding.ItemExamSyllabusBinding
import com.basic.studentportal.databinding.ItemModelResultBinding
import com.basic.studentportal.databinding.ItemWeeklyMarkBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.animateCount
import com.basic.studentportal.utils.animateCountFloat
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toSubjectLabel
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ─── Color helpers ────────────────────────────────────────────────────────────

private fun gradeColor(pct: Double): Int = when {
    pct >= 80 -> R.color.grade_excellent
    pct >= 65 -> R.color.grade_good
    pct >= 50 -> R.color.grade_average
    else      -> R.color.danger
}

private fun gradeCardBg(pct: Double): Int = when {
    pct >= 80 -> R.drawable.bg_card_success
    pct >= 65 -> R.drawable.bg_card_brand
    pct >= 50 -> R.drawable.bg_card_amber
    else      -> R.drawable.bg_card_danger
}

private fun gradeLetterCardBg(grade: String): Int = when (grade) {
    "A+"           -> R.drawable.bg_card_success
    "A", "A-"      -> R.drawable.bg_card_brand
    "B", "C"       -> R.drawable.bg_card_amber
    else           -> R.drawable.bg_card_danger
}

private fun gradeLetterColor(grade: String): Int = when (grade) {
    "A+"           -> R.color.grade_excellent
    "A", "A-"      -> R.color.grade_good
    "B", "C"       -> R.color.grade_average
    else           -> R.color.danger
}

// ─── Weekly Marks Adapter ─────────────────────────────────────────────────────

class WeeklyMarkAdapter : ListAdapter<WeeklyExamMark, WeeklyMarkAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemWeeklyMarkBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamMark) {
            val pct = item.percentage
                ?: if (item.maxMarks > 0) (item.marksObtained / item.maxMarks * 100) else 0.0
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvDate.text    = item.examDate
            b.tvScore.text   = "${item.marksObtained.toInt()}/${item.maxMarks.toInt()}"
            b.tvPercent.text = "${pct.toInt()}%"
            val ctx = b.root.context
            b.cardScore.background = ContextCompat.getDrawable(ctx, gradeCardBg(pct))
            b.tvPercent.setTextColor(ContextCompat.getColor(ctx, gradeColor(pct)))
            b.progressScore.max      = 100
            b.progressScore.progress = pct.toInt()
            b.progressScore.progressTintList =
                ContextCompat.getColorStateList(ctx, gradeColor(pct))
            b.tvRemarks.text       = item.remarks ?: ""
            b.tvRemarks.visibility = if (!item.remarks.isNullOrBlank()) View.VISIBLE else View.GONE
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemWeeklyMarkBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCb : DiffUtil.ItemCallback<WeeklyExamMark>() {
        override fun areItemsTheSame(a: WeeklyExamMark, b: WeeklyExamMark) = a.id == b.id
        override fun areContentsTheSame(a: WeeklyExamMark, b: WeeklyExamMark) = a == b
    }
}

// ─── Exam Routine Adapter ─────────────────────────────────────────────────────

class ExamRoutineAdapter : ListAdapter<WeeklyExamAssignment, ExamRoutineAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemExamRoutineBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamAssignment) {
            val parts = item.examDate.split("-")
            b.tvDay.text      = parts.getOrNull(2) ?: "—"
            b.tvMonth.text    = monthAbbr(parts.getOrNull(1)?.toIntOrNull())
            b.tvDate.text     = item.examDate
            b.tvExamName.text = item.examName ?: "Weekly Test"
            b.tvSubject.text  = item.subject.toSubjectLabel()
            b.tvTeacher.text  = item.teacherName ?: ""
            b.tvTeacher.visibility =
                if (item.teacherName.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        private fun monthAbbr(m: Int?): String = when (m) {
            1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"; 5 -> "MAY"; 6 -> "JUN"
            7 -> "JUL"; 8 -> "AUG"; 9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; 12 -> "DEC"
            else -> "—"
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemExamRoutineBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCb : DiffUtil.ItemCallback<WeeklyExamAssignment>() {
        override fun areItemsTheSame(a: WeeklyExamAssignment, b: WeeklyExamAssignment) = a.id == b.id
        override fun areContentsTheSame(a: WeeklyExamAssignment, b: WeeklyExamAssignment) = a == b
    }
}

// ─── Syllabus Adapter ─────────────────────────────────────────────────────────

class SyllabusAdapter : ListAdapter<WeeklyExamSyllabus, SyllabusAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemExamSyllabusBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamSyllabus) {
            b.tvSubject.text   = item.subject.toSubjectLabel()
            b.tvWeekRange.text = buildString {
                append(item.weekStartDate)
                if (!item.weekEndDate.isNullOrBlank()) append(" – ${item.weekEndDate}")
            }
            b.tvTitle.text       = item.title ?: ""
            b.tvTitle.visibility = if (item.title.isNullOrBlank()) View.GONE else View.VISIBLE
            b.tvDetails.text     = item.syllabusDetails ?: "—"
            b.tvSharedBy.text    = item.createdByName?.let { "by $it" } ?: ""
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemExamSyllabusBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCb : DiffUtil.ItemCallback<WeeklyExamSyllabus>() {
        override fun areItemsTheSame(a: WeeklyExamSyllabus, b: WeeklyExamSyllabus) = a.id == b.id
        override fun areContentsTheSame(a: WeeklyExamSyllabus, b: WeeklyExamSyllabus) = a == b
    }
}

// ─── Model Result Adapter ─────────────────────────────────────────────────────

class ModelResultAdapter : ListAdapter<ModelTestResult, ModelResultAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemModelResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ModelTestResult) {
            val ctx = b.root.context
            b.tvTestName.text = item.testName ?: "Model Test"
            b.tvSubject.text  = item.subject.toSubjectLabel()
            b.cardGrade.background =
                ContextCompat.getDrawable(ctx, gradeLetterCardBg(item.grade))
            b.tvGrade.text = item.grade
            b.tvGrade.setTextColor(ContextCompat.getColor(ctx, gradeLetterColor(item.grade)))
            b.tvGradePoint.text = "${item.gradePoint} GPA"
            b.tvMcq.text = item.mcqMark
                ?.let { "${it.toInt()}/${item.mcqMax?.toInt() ?: "?"}" } ?: "—"
            b.tvCq.text  = item.cqMark
                ?.let { "${it.toInt()}/${item.cqMax?.toInt() ?: "?"}" } ?: "—"
            val totalMax = (item.mcqMax ?: 0.0) + (item.cqMax ?: 0.0) + (item.practicalMax ?: 0.0)
            b.tvTotal.text = "${item.totalMark.toInt()}/${totalMax.toInt()}"
            b.tvTotal.setTextColor(ContextCompat.getColor(ctx, gradeLetterColor(item.grade)))
            b.tvYear.visibility = if (!item.year.isNullOrBlank()) {
                b.tvYear.text = item.year; View.VISIBLE
            } else View.GONE
        }
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        ItemModelResultBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))
    class DiffCb : DiffUtil.ItemCallback<ModelTestResult>() {
        override fun areItemsTheSame(a: ModelTestResult, b: ModelTestResult) = a.id == b.id
        override fun areContentsTheSame(a: ModelTestResult, b: ModelTestResult) = a == b
    }
}

// ─── ExamsFragment ────────────────────────────────────────────────────────────

@AndroidEntryPoint
class ExamsFragment : Fragment() {

    private var _binding: FragmentExamsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExamsViewModel by viewModels()

    private val marksAdapter        = WeeklyMarkAdapter()
    private val routineAdapter      = ExamRoutineAdapter()
    private val syllabusAdapter     = SyllabusAdapter()
    private val modelResultAdapter  = ModelResultAdapter()

    // Week dropdown
    private var weekOptions: List<WeekOption> = emptyList()
    private var weekSpinnerReady = false

    // Model test dropdown
    private var modelTests: List<ModelTest> = emptyList()
    private var modelSpinnerReady = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Recyclers ─────────────────────────────────────────────────────────
        binding.recyclerMarks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = marksAdapter
        }
        binding.recyclerRoutine.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routineAdapter
        }
        binding.recyclerSyllabus.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = syllabusAdapter
        }
        binding.recyclerModelTests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = modelResultAdapter
        }

        // ── Tabs ──────────────────────────────────────────────────────────────
        selectTab(weeklyTest = true)
        binding.tabWeeklyTest.setOnClickListener { selectTab(weeklyTest = true) }
        binding.tabModelTests.setOnClickListener { selectTab(weeklyTest = false) }

        // ── Refresh ───────────────────────────────────────────────────────────
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }
        binding.swipeRefreshModel.setOnRefreshListener { viewModel.loadAll() }

        // ── Collect: marks + week options ─────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.marks.collect { state ->
                binding.swipeRefresh.isRefreshing = state is Resource.Loading
                if (state is Resource.Success) {
                    val list = state.data.data
                    marksAdapter.submitList(list)
                    binding.tvEmptyMarks.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                    bindHeroCard(list)

                    // Populate week spinner once
                    val opts = state.data.weekOptions
                    if (!opts.isNullOrEmpty() && weekOptions.isEmpty()) {
                        weekOptions = opts
                        setupWeekSpinner(opts)
                    }
                }
            }
        }

        // ── Collect: assignments ──────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assignments.collect { state ->
                if (state is Resource.Success) {
                    val list = state.data.data
                    routineAdapter.submitList(list)
                    binding.tvEmptyRoutine.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // ── Collect: syllabi ──────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.syllabi.collect { state ->
                if (state is Resource.Success) {
                    val list = state.data.data
                    syllabusAdapter.submitList(list)
                    binding.tvEmptySyllabus.visibility =
                        if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        // ── Collect: model tests (for dropdown) ───────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modelTests.collect { state ->
                if (state is Resource.Success) {
                    val list = state.data.data
                    if (list.isNotEmpty() && modelTests.isEmpty()) {
                        modelTests = list
                        setupModelTestSpinner(list)
                    }
                }
            }
        }

        // ── Collect: model results ────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modelResults.collect { state ->
                binding.swipeRefreshModel.isRefreshing = state is Resource.Loading
                if (state is Resource.Success) {
                    modelResultAdapter.submitList(state.data.data)
                }
            }
        }
    }

    // ── Week spinner setup ────────────────────────────────────────────────────

    private fun setupWeekSpinner(opts: List<WeekOption>) {
        val labels = mutableListOf("All Weeks") + opts.map { it.label }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerWeek.adapter = spinnerAdapter

        weekSpinnerReady = false   // prevent initial selection from firing
        binding.spinnerWeek.post { weekSpinnerReady = true }

        binding.spinnerWeek.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (!weekSpinnerReady) return
                val weekStart = if (pos == 0) null else opts[pos - 1].key
                viewModel.filterByWeek(weekStart)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ── Model test spinner setup ──────────────────────────────────────────────

    private fun setupModelTestSpinner(tests: List<ModelTest>) {
        val labels = mutableListOf("All Model Tests") +
            tests.map { "${it.name}${if (!it.year.isNullOrBlank()) " (${it.year})" else ""}" }

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerModelTest.adapter = spinnerAdapter

        modelSpinnerReady = false
        binding.spinnerModelTest.post { modelSpinnerReady = true }

        binding.spinnerModelTest.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, v: View?, pos: Int, id: Long
                ) {
                    if (!modelSpinnerReady) return
                    val testId = if (pos == 0) null else tests[pos - 1].id
                    viewModel.filterModelResults(testId)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    // ── Hero card ─────────────────────────────────────────────────────────────

    private fun bindHeroCard(marks: List<WeeklyExamMark>) {
        if (marks.isEmpty()) {
            binding.tvAvgPercent.text       = "—"
            binding.tvPerformanceLabel.text = "No data"
            binding.tvTotalExams.text       = "0"
            binding.tvBestScore.text        = "—"
            binding.tvLowestScore.text      = "—"
            return
        }
        val percents = marks.map { m ->
            m.percentage ?: if (m.maxMarks > 0) m.marksObtained / m.maxMarks * 100 else 0.0
        }
        val avg    = percents.average()
        val best   = percents.max()
        val lowest = percents.min()

        animateCountFloat(binding.tvAvgPercent,   avg,    suffix = "")
        binding.tvTotalExams.animateCount(marks.size)
        animateCountFloat(binding.tvBestScore,   best,   suffix = "%")
        animateCountFloat(binding.tvLowestScore, lowest, suffix = "%")

        binding.tvPerformanceLabel.text = when {
            avg >= 85 -> "Excellent 🏆"
            avg >= 70 -> "Good 📈"
            avg >= 55 -> "Average 📚"
            else      -> "Needs Work ⚠"
        }
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    private fun selectTab(weeklyTest: Boolean) {
        val ctx         = requireContext()
        val activeBg    = ContextCompat.getDrawable(ctx, R.drawable.bg_tab_selected)
        val activeColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val dimColor    = ContextCompat.getColor(ctx, R.color.text_hint)

        binding.tabWeeklyTest.background = if (weeklyTest) activeBg else null
        binding.tabWeeklyTest.setTextColor(if (weeklyTest) activeColor else dimColor)
        binding.tabModelTests.background = if (!weeklyTest) activeBg else null
        binding.tabModelTests.setTextColor(if (!weeklyTest) activeColor else dimColor)

        binding.swipeRefresh.visibility       = if (weeklyTest) View.VISIBLE else View.GONE
        binding.containerModelTests.visibility = if (!weeklyTest) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
