package com.basic.studentportal.ui.notices

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
import com.basic.studentportal.data.model.ListResponse
import com.basic.studentportal.data.model.StudentNotice
import com.basic.studentportal.data.repository.NoticeRepository
import com.basic.studentportal.databinding.FragmentNoticesBinding
import com.basic.studentportal.databinding.ItemNoticeBinding
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
class NoticesViewModel @Inject constructor(private val repository: NoticeRepository) : ViewModel() {

    private val _notices = MutableStateFlow<Resource<ListResponse<StudentNotice>>>(Resource.Loading)
    val notices: StateFlow<Resource<ListResponse<StudentNotice>>> = _notices

    init { loadNotices() }

    fun loadNotices() {
        viewModelScope.launch {
            _notices.value = Resource.Loading
            _notices.value = repository.getNotices()
        }
    }

    fun acknowledge(noticeId: Int) {
        viewModelScope.launch { repository.acknowledgeNotice(noticeId) }
    }
}

// ─── Adapter ─────────────────────────────────────────────────────────────────

class NoticeAdapter(
    private val onAcknowledge: (Int) -> Unit
) : ListAdapter<StudentNotice, NoticeAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemNoticeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: StudentNotice) {
            b.tvTitle.text = item.title
            b.tvBody.text = item.body
            b.tvDate.text = item.noticeDate
            if (item.isAcknowledged) {
                b.btnAck.gone()
                b.tvAcked.visible()
            } else {
                b.btnAck.visible()
                b.tvAcked.gone()
                b.btnAck.setOnClickListener { onAcknowledge(item.id) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<StudentNotice>() {
        override fun areItemsTheSame(a: StudentNotice, b: StudentNotice) = a.id == b.id
        override fun areContentsTheSame(a: StudentNotice, b: StudentNotice) = a == b
    }
}

// ─── Fragment ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class NoticesFragment : Fragment() {

    private var _binding: FragmentNoticesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoticesViewModel by viewModels()
    private lateinit var adapter: NoticeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNoticesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NoticeAdapter { noticeId ->
            viewModel.acknowledge(noticeId)
            viewModel.loadNotices()
        }
        binding.recyclerNotices.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadNotices() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notices.collect { state ->
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
