package com.basic.studentportal.ui.fees

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
import com.basic.studentportal.data.model.FeeInvoice
import com.basic.studentportal.data.model.FeePayment
import com.basic.studentportal.databinding.ItemInvoiceBinding
import com.basic.studentportal.databinding.ItemPaymentBinding
import com.basic.studentportal.utils.toCurrency

/**
 * [monthlyFee] is the canonical full monthly fee used for every row's DUE and
 * BALANCE columns. Always set this via [setMonthlyFee] BEFORE calling [submitList]
 * so the value is available when DiffUtil dispatches the first bind pass.
 */
class InvoiceAdapter(
    private var monthlyFee: Double = 0.0
) : ListAdapter<FeeInvoice, InvoiceAdapter.VH>(DiffCb()) {

    /**
     * Update the canonical monthly fee.
     * Do NOT call notifyDataSetChanged() here — the caller must call submitList()
     * immediately after, which triggers its own full rebind via DiffUtil.
     * Calling notifyDataSetChanged() + submitList() together causes a race that
     * drops the month-name text on the first card.
     */
    fun setMonthlyFee(fee: Double) {
        monthlyFee = fee
        // No notifyDataSetChanged — submitList() called right after handles rebinding
    }

    inner class VH(private val b: ItemInvoiceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeeInvoice) {
            val ctx = b.root.context

            // Month label — always bound first so it is never blank
            b.tvMonth.text = item.billingMonthLabel ?: item.billingMonth

            // DUE — canonical monthly fee, same for every row
            b.tvAmountDue.text = monthlyFee.toCurrency()

            // PAID — actual payment received from the student
            b.tvAmountPaid.text = item.amountPaid.toCurrency()

            // BALANCE — canonical fee minus actual payment, computed locally
            val balance = (monthlyFee - item.amountPaid).coerceAtLeast(0.0)
            b.tvOutstanding.text = balance.toCurrency()

            // Due date row
            if (!item.dueDate.isNullOrBlank()) {
                b.tvDueDate.text = "Due: ${item.dueDate}"
                b.tvDueDate.visibility = View.VISIBLE
            } else {
                b.tvDueDate.visibility = View.GONE
            }

            // Status badge + balance colour
            when (item.status.lowercase()) {
                "paid" -> {
                    b.tvStatus.text = "PAID ✓"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_success)
                    b.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
                }
                "partial" -> {
                    b.tvStatus.text = "PARTIAL"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_warning)
                    b.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                }
                else -> {
                    b.tvStatus.text = "PENDING"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_danger)
                    b.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<FeeInvoice>() {
        override fun areItemsTheSame(a: FeeInvoice, b: FeeInvoice) = a.id == b.id
        override fun areContentsTheSame(a: FeeInvoice, b: FeeInvoice) = a == b
    }
}

// ─── Payment adapter ─────────────────────────────────────────────────────────

class PaymentAdapter : ListAdapter<FeePayment, PaymentAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemPaymentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeePayment) {
            b.tvAmount.text       = item.amount.toCurrency()
            b.tvDate.text         = item.paymentDate
            b.tvMode.text         = item.paymentMode?.replace("_", " ")?.uppercase() ?: "N/A"
            b.tvReceipt.text      = item.receiptNumber?.let { "Receipt: $it" } ?: ""
            b.tvRef.text          = item.reference?.let { "Ref: $it" } ?: ""
            b.tvInvoiceMonth.text = item.invoice?.billingMonthLabel
                ?: item.invoice?.billingMonth ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<FeePayment>() {
        override fun areItemsTheSame(a: FeePayment, b: FeePayment) = a.id == b.id
        override fun areContentsTheSame(a: FeePayment, b: FeePayment) = a == b
    }
}
