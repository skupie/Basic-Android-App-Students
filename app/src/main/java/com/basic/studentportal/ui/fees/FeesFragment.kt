package com.basic.studentportal.ui.fees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.basic.studentportal.databinding.FragmentFeesBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toCurrency
import com.basic.studentportal.utils.visible
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeesFragment : Fragment() {

    private var _binding: FragmentFeesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeesViewModel by viewModels()
    private val invoiceAdapter = InvoiceAdapter()
    private val paymentAdapter = PaymentAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerFees.adapter = invoiceAdapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> binding.recyclerFees.adapter = invoiceAdapter
                    1 -> binding.recyclerFees.adapter = paymentAdapter
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.invoices.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        invoiceAdapter.submitList(state.data.data)
                        state.data.dueSummary?.let { ds ->
                            binding.tvTotalDue.text = ds.totalDue.toCurrency()
                            binding.tvDueMonths.text = "${ds.dueMonthsCount} month(s) pending"
                            binding.cardDueSummary.visible()
                        } ?: binding.cardDueSummary.gone()
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.payments.collect { state ->
                if (state is Resource.Success) paymentAdapter.submitList(state.data.data)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
