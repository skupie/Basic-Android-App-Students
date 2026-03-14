package com.basic.studentportal.ui.fees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.basic.studentportal.R
import com.basic.studentportal.databinding.FragmentFeesBinding
import com.basic.studentportal.utils.Resource
import com.basic.studentportal.utils.gone
import com.basic.studentportal.utils.toCurrency
import com.basic.studentportal.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeesFragment : Fragment() {

    private var _binding: FragmentFeesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeesViewModel by viewModels()
    private val invoiceAdapter = InvoiceAdapter()
    private val paymentAdapter = PaymentAdapter()

    private var isShowingInvoices = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── RecyclerView — was missing LayoutManager ──────────────────────────
        binding.recyclerFees.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFees.adapter = invoiceAdapter

        // ── Pill tab switching ────────────────────────────────────────────────
        selectTab(invoices = true)

        binding.tabInvoices.setOnClickListener {
            if (!isShowingInvoices) {
                isShowingInvoices = true
                selectTab(invoices = true)
                binding.recyclerFees.adapter = invoiceAdapter
            }
        }

        binding.tabPayments.setOnClickListener {
            if (isShowingInvoices) {
                isShowingInvoices = false
                selectTab(invoices = false)
                binding.recyclerFees.adapter = paymentAdapter
            }
        }

        // ── Swipe refresh ─────────────────────────────────────────────────────
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        // ── Invoices ──────────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.invoices.collect { state ->
                when (state) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        invoiceAdapter.submitList(state.data.data)

                        val ds = state.data.dueSummary
                        if (ds != null && ds.totalDue > 0) {
                            binding.cardDueSummary.visible()

                            // Use server's totalDue directly — it already accounts for
                            // partial payments and any adjustments. Do NOT recalculate.
                            binding.tvTotalDue.text = ds.totalDue.toCurrency()

                            binding.tvOverdueBadge.text = "${ds.dueMonthsCount} Month${if (ds.dueMonthsCount != 1) "s" else ""} Overdue"

                            val unpaidLabels = state.data.data
                                .filter { it.status != "paid" }
                                .mapNotNull { it.billingMonthLabel ?: it.billingMonth }
                                .takeLast(2)
                            binding.tvDueMonths.text = if (unpaidLabels.isNotEmpty())
                                "${unpaidLabels.joinToString(" & ")} unpaid"
                            else "${ds.dueMonthsCount} month(s) pending"
                        } else {
                            binding.cardDueSummary.gone()
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.cardDueSummary.gone()
                    }
                }
            }
        }

        // ── Payments ──────────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.payments.collect { state ->
                if (state is Resource.Success) paymentAdapter.submitList(state.data.data)
            }
        }
    }

    private fun selectTab(invoices: Boolean) {
        if (invoices) {
            binding.tabInvoices.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabInvoices.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabPayments.background = null
            binding.tabPayments.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
        } else {
            binding.tabPayments.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabPayments.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabInvoices.background = null
            binding.tabInvoices.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
