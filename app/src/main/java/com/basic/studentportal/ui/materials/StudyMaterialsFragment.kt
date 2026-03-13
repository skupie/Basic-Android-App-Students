package com.basic.studentportal.ui.materials

import android.content.Intent
import android.net.Uri
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
import com.basic.studentportal.data.model.StudyMaterial
import com.basic.studentportal.data.model.StudyMaterialsResponse
import com.basic.studentportal.data.repository.StudyMaterialRepository
import com.basic.studentportal.databinding.FragmentStudyMaterialsBinding
import com.basic.studentportal.databinding.ItemStudyMaterialBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            b.tvTitle.text = item.title
            b.tvDescription.text = item.description ?: ""
            b.tvSubject.text = item.subjectLabel ?: item.subject?.replace("_", " ")?.uppercase() ?: ""
            b.tvTeacher.text = item.uploadedByName?.let { "By: $it" } ?: ""
            b.tvFileSize.text = item.fileSizeKb?.let { "${it} KB" } ?: ""
            b.tvFileName.text = item.fileName ?: ""
            b.tvUpdated.text = item.updatedAt?.take(10) ?: ""

            val icon = when {
                item.mimeType?.contains("pdf") == true -> "📄"
                item.mimeType?.contains("image") == true -> "🖼️"
                item.mimeType?.contains("word") == true -> "📝"
                else -> "📁"
            }
            b.tvFileIcon.text = icon

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudyMaterialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StudyMaterialAdapter { material ->
            material.downloadUrl?.let { url ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        binding.recyclerMaterials.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadMaterials() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.materials.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        adapter.submitList(state.data.data)
                        if (state.data.data.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
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
