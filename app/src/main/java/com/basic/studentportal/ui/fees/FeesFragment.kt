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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
                        if (ds != null && ds.dueMonthsCount > 0) {
                            binding.cardDueSummary.visible()

                            // Monthly fee = highest amountDue across all invoices
                            val monthlyFee = state.data.data
                                .maxOfOrNull { it.amountDue } ?: 0.0

                            // Row 1: Monthly Fee
                            binding.tvMonthlyFee.text = monthlyFee.toCurrency()

                            // Row 2: Due Months — badge shows count, names shown beside it
                            binding.tvOverdueBadge.text =
                                "${ds.dueMonthsCount} Month${if (ds.dueMonthsCount != 1) "s" else ""}"

                            val monthNames = state.data.data
                                .filter { it.status != "paid" }
                                .map { inv ->
                                    inv.billingMonthLabel ?: formatBillingMonth(inv.billingMonth)
                                }
                            binding.tvDueMonths.text = when (monthNames.size) {
                                0    -> ""
                                1    -> monthNames[0]
                                2    -> "${monthNames[0]} & ${monthNames[1]}"
                                else -> "${monthNames.take(2).joinToString(" & ")} +${monthNames.size - 2} more"
                            }

                            // Row 3: Total Due = dueMonthsCount × monthly fee
                            binding.tvTotalDue.text = (ds.dueMonthsCount * monthlyFee).toCurrency()

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

    /** Converts "2026-02" → "February 2026". Returns the raw string if parsing fails. */
    private fun formatBillingMonth(raw: String): String {
        return try {
            val ym = YearMonth.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM"))
            "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}"
        } catch (e: Exception) { raw }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
