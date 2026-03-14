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

        binding.recyclerFees.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFees.adapter = invoiceAdapter

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
                        if (ds != null && ds.dueMonthsCount > 0) {
                            binding.cardDueSummary.visible()

                            // Monthly fee = the maximum amountDue seen across ALL invoices.
                            // amountDue is the original full-month fee set when the invoice was
                            // created. Using max() ensures we never accidentally grab a reduced
                            // amount caused by adjustments on any single invoice.
                            val monthlyFee = state.data.data
                                .maxOfOrNull { it.amountDue } ?: 0.0

                            // Total outstanding = number of due months × full monthly fee.
                            // We intentionally do NOT use server's totalDue because it may
                            // subtract partial payments instead of showing the real obligation.
                            val totalOutstanding = ds.dueMonthsCount * monthlyFee
                            binding.tvTotalDue.text = totalOutstanding.toCurrency()

                            binding.tvOverdueBadge.text =
                                "${ds.dueMonthsCount} Month${if (ds.dueMonthsCount != 1) "s" else ""} Overdue"

                            // Label: "February & March unpaid"
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
            binding.tabInvoices.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabInvoices.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabPayments.background = null
            binding.tabPayments.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_hint))
        } else {
            binding.tabPayments.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_selected)
            binding.tabPayments.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary))
            binding.tabInvoices.background = null
            binding.tabInvoices.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_hint))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
