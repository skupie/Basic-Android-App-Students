package com.basic.studentportal.ui.exams

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
import com.basic.studentportal.data.model.ModelTestResult
import com.basic.studentportal.data.model.WeeklyExamAssignment
import com.basic.studentportal.data.model.WeeklyExamMark
import com.basic.studentportal.databinding.FragmentExamsBinding
import com.basic.studentportal.databinding.ItemAssignmentBinding
import com.basic.studentportal.databinding.ItemModelResultBinding
import com.basic.studentportal.databinding.ItemWeeklyMarkBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.toSubjectLabel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun Double.toPercent1() = "${this.toInt()}%"

private fun gradeColor(pct: Double): Int = when {
    pct >= 80 -> R.color.grade_excellent
    pct >= 65 -> R.color.grade_good
    pct >= 50 -> R.color.grade_average
    else -> R.color.danger
}

private fun gradeCardBg(pct: Double): Int = when {
    pct >= 80 -> R.drawable.bg_card_success
    pct >= 65 -> R.drawable.bg_card_brand
    pct >= 50 -> R.drawable.bg_card_amber
    else -> R.drawable.bg_card_danger
}

private fun gradeLetterCardBg(grade: String): Int = when (grade) {
    "A+" -> R.drawable.bg_card_success
    "A", "A-" -> R.drawable.bg_card_brand
    "B", "C" -> R.drawable.bg_card_amber
    else -> R.drawable.bg_card_danger
}

private fun gradeLetterColor(grade: String): Int = when (grade) {
    "A+" -> R.color.grade_excellent
    "A", "A-" -> R.color.grade_good
    "B", "C" -> R.color.grade_average
    else -> R.color.danger
}

// ─── Weekly Marks Adapter ─────────────────────────────────────────────────────

class WeeklyMarkAdapter : ListAdapter<WeeklyExamMark, WeeklyMarkAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemWeeklyMarkBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamMark) {
            val pct = item.percentage
                ?: if (item.maxMarks > 0) (item.marksObtained / item.maxMarks * 100) else 0.0

            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvDate.text = item.examDate
            b.tvScore.text = "${item.marksObtained.toInt()}/${item.maxMarks.toInt()}"
            b.tvPercent.text = pct.toInt().toString() + "%"

            val ctx = b.root.context
            val color = gradeColor(pct)
            val bgRes = gradeCardBg(pct)
            b.cardScore.background = ContextCompat.getDrawable(ctx, bgRes)
            b.tvPercent.setTextColor(ContextCompat.getColor(ctx, color))

            b.progressScore.max = 100
            b.progressScore.progress = pct.toInt()
            b.progressScore.progressTintList = ContextCompat.getColorStateList(ctx, color)

            b.tvRemarks.text = item.remarks ?: ""
            b.tvRemarks.visibility = if (!item.remarks.isNullOrBlank()) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemWeeklyMarkBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<WeeklyExamMark>() {
        override fun areItemsTheSame(a: WeeklyExamMark, b: WeeklyExamMark) = a.id == b.id
        override fun areContentsTheSame(a: WeeklyExamMark, b: WeeklyExamMark) = a == b
    }
}

// ─── Assignment Adapter ───────────────────────────────────────────────────────

class AssignmentAdapter : ListAdapter<WeeklyExamAssignment, AssignmentAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemAssignmentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamAssignment) {
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvDate.text = item.examDate
            b.tvName.text = item.examName ?: "Exam"
            b.tvTeacher.text = item.teacherName ?: ""
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAssignmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    class DiffCb : DiffUtil.ItemCallback<WeeklyExamAssignment>() {
        override fun areItemsTheSame(a: WeeklyExamAssignment, b: WeeklyExamAssignment) = a.id == b.id
        override fun areContentsTheSame(a: WeeklyExamAssignment, b: WeeklyExamAssignment) = a == b
    }
}

// ─── Model Result Adapter ─────────────────────────────────────────────────────

class ModelResultAdapter : ListAdapter<ModelTestResult, ModelResultAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemModelResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ModelTestResult) {
            b.tvTestName.text = item.testName ?: "Model Test"
            b.tvSubject.text = item.subject.toSubjectLabel()

            val ctx = b.root.context
            val bgRes = gradeLetterCardBg(item.grade)
            val colorRes = gradeLetterColor(item.grade)
            b.cardGrade.background = ContextCompat.getDrawable(ctx, bgRes)
            b.tvGrade.text = item.grade
            b.tvGrade.setTextColor(ContextCompat.getColor(ctx, colorRes))
            b.tvGradePoint.text = "${item.gradePoint} GPA"

            // MCQ / CQ in clean "26/30" format matching mockup
            b.tvMcq.text = item.mcqMark?.let { "${it.toInt()}/${item.mcqMax?.toInt() ?: "?"}" } ?: "—"
            b.tvCq.text = item.cqMark?.let { "${it.toInt()}/${item.cqMax?.toInt() ?: "?"}" } ?: "—"

            val totalMax = (item.mcqMax ?: 0.0) + (item.cqMax ?: 0.0) + (item.practicalMax ?: 0.0)
            b.tvTotal.text = "${item.totalMark.toInt()}/${totalMax.toInt()}"
            b.tvTotal.setTextColor(ContextCompat.getColor(ctx, colorRes))

            if (!item.year.isNullOrBlank()) {
                b.tvYear.text = item.year
                b.tvYear.visibility = View.VISIBLE
            } else {
                b.tvYear.visibility = View.GONE
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemModelResultBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
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
    private val marksAdapter = WeeklyMarkAdapter()
    private val assignmentAdapter = AssignmentAdapter()
    private val modelResultAdapter = ModelResultAdapter()

    private var selectedTab = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── RecyclerView ──────────────────────────────────────────────────────
        binding.recyclerExams.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerExams.adapter = marksAdapter

        // ── Pill tabs ─────────────────────────────────────────────────────────
        selectTab(0)
        binding.tabMarks.setOnClickListener { selectTab(0) }
        binding.tabAssignments.setOnClickListener { selectTab(1) }
        binding.tabModelTests.setOnClickListener { selectTab(2) }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        // ── Marks + hero stats ────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.marks.collect { state ->
                binding.swipeRefresh.isRefreshing = state is Resource.Loading
                if (state is Resource.Success) {
                    marksAdapter.submitList(state.data.data)
                    bindHeroCard(state.data.data)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assignments.collect { state ->
                if (state is Resource.Success) assignmentAdapter.submitList(state.data.data)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.modelResults.collect { state ->
                if (state is Resource.Success) modelResultAdapter.submitList(state.data.data)
            }
        }
    }

    private fun bindHeroCard(marks: List<WeeklyExamMark>) {
        if (marks.isEmpty()) return
        val percents = marks.map { m ->
            m.percentage ?: if (m.maxMarks > 0) m.marksObtained / m.maxMarks * 100 else 0.0
        }
        val avg = percents.average()
        val best = percents.maxOrNull() ?: 0.0
        val lowest = percents.minOrNull() ?: 0.0

        binding.tvAvgPercent.text = avg.toInt().toString()
        binding.tvTotalExams.text = marks.size.toString()
        binding.tvBestScore.text = "${best.toInt()}%"
        binding.tvLowestScore.text = "${lowest.toInt()}%"

        val label = when {
            avg >= 85 -> "Excellent 🏆"
            avg >= 70 -> "Good 📈"
            avg >= 55 -> "Average 📚"
            else -> "Needs Work ⚠"
        }
        binding.tvPerformanceLabel.text = label
        binding.tvTrendDelta.text = "" // trend data not in API yet
    }

    private fun selectTab(idx: Int) {
        selectedTab = idx
        val ctx = requireContext()
        val activeDrawable = ContextCompat.getDrawable(ctx, R.drawable.bg_tab_selected)
        val activeTxt = ContextCompat.getColor(ctx, R.color.text_primary)
        val inactiveTxt = ContextCompat.getColor(ctx, R.color.text_hint)

        listOf(binding.tabMarks, binding.tabAssignments, binding.tabModelTests)
            .forEachIndexed { i, tv ->
                tv.background = if (i == idx) activeDrawable else null
                tv.setTextColor(if (i == idx) activeTxt else inactiveTxt)
            }

        binding.recyclerExams.adapter = when (idx) {
            1 -> assignmentAdapter
            2 -> modelResultAdapter
            else -> marksAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
