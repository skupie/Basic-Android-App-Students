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

                        // Canonical monthly fee = maximum amountDue seen across all invoices.
                        // This guards against the server returning a reduced amountDue on
                        // individual invoices (e.g. 1,000 for one month vs 2,000 for others).
                        val monthlyFee = state.data.data
                            .maxOfOrNull { it.amountDue } ?: 0.0

                        // IMPORTANT: setMonthlyFee() MUST be called before submitList().
                        // submitList() kicks off an async DiffUtil diff which will call
                        // onBindViewHolder for each item. If monthlyFee isn't set yet,
                        // the DUE column and month label bind correctly on first pass.
                        // Calling notifyDataSetChanged() after submitList() races with
                        // that diff and can blank out tvMonth on the first card.
                        invoiceAdapter.setMonthlyFee(monthlyFee)
                        invoiceAdapter.submitList(state.data.data)

                        // ── Hero outstanding card ─────────────────────────────
                        val ds = state.data.dueSummary
                        if (ds != null && ds.dueMonthsCount > 0) {
                            binding.cardDueSummary.visible()

                            val totalOutstanding = ds.dueMonthsCount * monthlyFee
                            binding.tvTotalDue.text = totalOutstanding.toCurrency()

                            binding.tvOverdueBadge.text =
                                "${ds.dueMonthsCount} Month${if (ds.dueMonthsCount != 1) "s" else ""} Overdue"

                            val unpaidLabels = state.data.data
                                .filter { it.status != "paid" }
                                .map { inv ->
                                    // billingMonthLabel is human-readable when the server sends it
                                    // (e.g. "February 2026"). When it is null, billingMonth is a
                                    // raw "yyyy-MM" string — parse and format it ourselves.
                                    inv.billingMonthLabel
                                        ?: formatBillingMonth(inv.billingMonth)
                                }
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

    /**
     * Converts a raw "yyyy-MM" billing month string (e.g. "2026-02") into
     * a readable label (e.g. "February 2026").
     * Returns the original string unchanged if it can't be parsed.
     */
    private fun formatBillingMonth(raw: String): String {
        return try {
            val ym = java.time.YearMonth.parse(raw,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
            val month = ym.month.getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
            "$month ${ym.year}"
        } catch (e: Exception) {
            raw   // fall back to raw value if format is unexpected
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
