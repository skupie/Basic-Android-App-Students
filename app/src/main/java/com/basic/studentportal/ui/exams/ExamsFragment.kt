package com.basic.studentportal.ui.exams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
import com.basic.studentportal.data.model.ModelTestResult
import com.basic.studentportal.data.model.WeeklyExamAssignment
import com.basic.studentportal.data.model.WeeklyExamMark
import com.basic.studentportal.databinding.*
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toPercent
import com.basic.studentportal.utils.toSubjectLabel
import com.basic.studentportal.utils.visible
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// ─── Weekly Marks Adapter ─────────────────────────────────────────────────────

class WeeklyMarkAdapter : ListAdapter<WeeklyExamMark, WeeklyMarkAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemWeeklyMarkBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WeeklyExamMark) {
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvDate.text = item.examDate
            b.tvScore.text = "${item.marksObtained} / ${item.maxMarks}"
            val pct = item.percentage ?: if (item.maxMarks > 0) (item.marksObtained / item.maxMarks * 100) else 0.0
            b.tvPercent.text = pct.toPercent()
            b.progressScore.max = 100
            b.progressScore.progress = pct.toInt()
            b.tvRemarks.text = item.remarks ?: ""
            val color = when {
                pct >= 80 -> R.color.grade_excellent
                pct >= 65 -> R.color.grade_good
                pct >= 50 -> R.color.grade_average
                else -> R.color.status_inactive
            }
            b.tvPercent.setTextColor(ContextCompat.getColor(b.root.context, color))
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
            b.tvTeacher.text = item.teacherName?.let { "Teacher: $it" } ?: ""
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

// ─── Model Test Result Adapter ────────────────────────────────────────────────

class ModelResultAdapter : ListAdapter<ModelTestResult, ModelResultAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemModelResultBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ModelTestResult) {
            b.tvTestName.text = item.testName ?: "Model Test"
            b.tvSubject.text = item.subject.toSubjectLabel()
            b.tvGrade.text = item.grade
            b.tvGradePoint.text = "GPA: ${item.gradePoint}"
            b.tvTotal.text = "Total: ${item.totalMark}"
            b.tvMcq.text = item.mcqMark?.let { "MCQ: $it/${item.mcqMax}" } ?: ""
            b.tvCq.text = item.cqMark?.let { "CQ: $it/${item.cqMax}" } ?: ""
            b.tvYear.text = item.year ?: ""
            val color = when (item.grade) {
                "A+" -> R.color.grade_excellent
                "A", "A-" -> R.color.grade_good
                "B", "C" -> R.color.grade_average
                else -> R.color.status_inactive
            }
            b.tvGrade.setTextColor(ContextCompat.getColor(b.root.context, color))
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerExams.adapter = marksAdapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> binding.recyclerExams.adapter = marksAdapter
                    1 -> binding.recyclerExams.adapter = assignmentAdapter
                    2 -> binding.recyclerExams.adapter = modelResultAdapter
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.marks.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading
                if (state is Resource.Success) marksAdapter.submitList(state.data.data)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
