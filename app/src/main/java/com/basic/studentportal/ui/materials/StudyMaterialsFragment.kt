package com.basic.studentportal.ui.materials

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.basic.studentportal.data.model.StudyMaterial
import com.basic.studentportal.data.model.StudyMaterialsResponse
import com.basic.studentportal.data.model.SubjectOption
import com.basic.studentportal.data.repository.StudyMaterialRepository
import com.basic.studentportal.databinding.FragmentStudyMaterialsBinding
import com.basic.studentportal.databinding.ItemStudyMaterialBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.showToast
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun subjectIconBg(subject: String?): Int = when {
    subject == null -> R.drawable.bg_card_dark
    subject.contains("math", true) -> R.drawable.bg_card_danger
    subject.contains("phys", true) -> R.drawable.bg_card_danger
    subject.contains("eng", true) -> R.drawable.bg_card_brand
    subject.contains("chem", true) -> R.drawable.bg_card_success
    subject.contains("bio", true) -> R.drawable.bg_card_success
    else -> R.drawable.bg_card_amber
}

private fun subjectLabelBg(subject: String?): Int = when {
    subject == null -> R.drawable.bg_pill_brand
    subject.contains("math", true) -> R.drawable.bg_pill_warning
    subject.contains("phys", true) -> R.drawable.bg_pill_brand
    subject.contains("eng", true) -> R.drawable.bg_pill_success
    subject.contains("chem", true) -> R.drawable.bg_pill_warning
    else -> R.drawable.bg_pill_brand
}

private fun subjectLabelColor(subject: String?): Int = when {
    subject == null -> R.color.brand_light
    subject.contains("math", true) -> R.color.grade_average
    subject.contains("phys", true) -> R.color.brand_light
    subject.contains("eng", true) -> R.color.success
    subject.contains("chem", true) -> R.color.warning_light
    else -> R.color.brand_light
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class StudyMaterialsViewModel @Inject constructor(
    private val repository: StudyMaterialRepository
) : ViewModel() {

    private val _materials = MutableStateFlow<Resource<StudyMaterialsResponse>>(Resource.Loading)
    val materials: StateFlow<Resource<StudyMaterialsResponse>> = _materials

    var selectedSubject: String? = null

    init { loadMaterials() }

    fun loadMaterials(subject: String? = selectedSubject) {
        viewModelScope.launch {
            selectedSubject = subject
            _materials.value = Resource.Loading
            _materials.value = repository.getMaterials(subject)
        }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class StudyMaterialAdapter(
    private val onDownload: (StudyMaterial) -> Unit
) : ListAdapter<StudyMaterial, StudyMaterialAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemStudyMaterialBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StudyMaterial) {
            val ctx = b.root.context
            val subjectKey = item.subject

            b.tvTitle.text       = item.title
            b.tvDescription.text = item.description ?: ""
            b.tvTeacher.text     = item.uploadedByName ?: ""
            b.tvUpdated.text     = item.updatedAt?.take(10)?.let { "· $it" } ?: ""

            b.tvFileSize.text = when {
                (item.fileSizeKb ?: 0) >= 1024 -> "${"%.1f".format((item.fileSizeKb!! / 1024.0))} MB"
                (item.fileSizeKb ?: 0) > 0      -> "${item.fileSizeKb} KB"
                else -> ""
            }

            val label = item.subjectLabel
                ?: item.subject?.uppercase()?.take(4)
                ?: ""
            b.tvSubject.text = label
            b.tvSubject.background = ContextCompat.getDrawable(ctx, subjectLabelBg(subjectKey))
            b.tvSubject.setTextColor(ContextCompat.getColor(ctx, subjectLabelColor(subjectKey)))

            b.cardFileIcon.background = ContextCompat.getDrawable(ctx, subjectIconBg(subjectKey))

            b.tvFileIcon.text = when {
                item.mimeType?.contains("pdf") == true   -> "📄"
                item.mimeType?.contains("image") == true -> "🖼️"
                item.mimeType?.contains("word") == true  -> "📝"
                else -> "📁"
            }

            if (item.downloadUrl != null) {
                b.btnDownload.visible()
                b.btnDownload.setOnClickListener { onDownload(item) }
            } else {
                b.btnDownload.gone()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemStudyMaterialBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<StudyMaterial>() {
        override fun areItemsTheSame(a: StudyMaterial, b: StudyMaterial) = a.id == b.id
        override fun areContentsTheSame(a: StudyMaterial, b: StudyMaterial) = a == b
    }
}

// ─── Fragment ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class StudyMaterialsFragment : Fragment() {

    private var _binding: FragmentStudyMaterialsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StudyMaterialsViewModel by viewModels()
    private lateinit var adapter: StudyMaterialAdapter

    private var allMaterials: List<StudyMaterial> = emptyList()
    private var selectedSubjectKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyMaterialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudyMaterialAdapter { material ->
            material.downloadUrl?.let { url -> downloadFile(material.title, url) }
        }

        binding.recyclerMaterials.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMaterials.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadMaterials(selectedSubjectKey) }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { filterAndSubmit(s?.toString() ?: "") }
        })

        binding.chipAll.setOnClickListener { selectSubject(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.materials.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        allMaterials = state.data.data
                        buildChips(state.data.subjectOptions)

                        binding.tvTotalFiles.text   = allMaterials.size.toString()
                        val subjectCount = allMaterials.map { it.subject }.distinct().size
                        binding.tvSubjectCount.text = subjectCount.toString()
                        binding.tvThisWeek.text     = countThisWeek(allMaterials).toString()

                        filterAndSubmit(binding.etSearch.text?.toString() ?: "")
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

    // ── Direct download via DownloadManager ───────────────────────────────────

    private fun downloadFile(title: String, url: String) {
        try {
            val fileName = title
                .replace(Regex("[^a-zA-Z0-9._\\- ]"), "")
                .trim()
                .replace(" ", "_")
                .take(60)
                .ifBlank { "file" } + if (!title.contains(".")) ".pdf" else ""

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription("ডাউনলোড হচ্ছে…")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val dm = requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

            requireContext().showToast("ডাউনলোড শুরু হয়েছে — নোটিফিকেশন চেক করুন")
        } catch (e: Exception) {
            requireContext().showToast("ডাউনলোড শুরু করতে ব্যর্থ হয়েছে")
        }
    }

    private fun buildChips(options: List<SubjectOption>?) {
        val chipGroup = binding.chipGroup
        while (chipGroup.childCount > 1) chipGroup.removeViewAt(1)

        options?.forEach { opt ->
            val chip = TextView(requireContext()).apply {
                text = opt.label
                tag  = opt.key
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_dark)
                setPadding(dpToPx(18), dpToPx(7), dpToPx(18), dpToPx(7))
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dpToPx(8) }
                setOnClickListener { selectSubject(opt.key) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun selectSubject(key: String?) {
        selectedSubjectKey = key
        val chipGroup = binding.chipGroup
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? TextView ?: continue
            chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_card_dark)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
        }
        if (key == null) {
            binding.chipAll.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_brand)
            binding.chipAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            for (i in 1 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? TextView ?: continue
                if (chip.tag as? String == key) {
                    chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_brand)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    break
                }
            }
        }
        filterAndSubmit(binding.etSearch.text?.toString() ?: "")
    }

    private fun filterAndSubmit(query: String) {
        var list = allMaterials
        if (selectedSubjectKey != null) {
            list = list.filter { it.subject.equals(selectedSubjectKey, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, true) ||
                it.description?.contains(query, true) == true ||
                it.subject?.contains(query, true) == true
            }
        }
        adapter.submitList(list)
        if (list.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
    }

    private fun countThisWeek(materials: List<StudyMaterial>): Int {
        return try {
            val weekAgo = LocalDate.now().minusDays(7)
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            materials.count { m ->
                val d = m.updatedAt?.take(10) ?: return@count false
                LocalDate.parse(d, fmt).isAfter(weekAgo)
            }
        } catch (e: Exception) { 0 }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
