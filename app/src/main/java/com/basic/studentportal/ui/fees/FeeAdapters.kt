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

class InvoiceAdapter : ListAdapter<FeeInvoice, InvoiceAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemInvoiceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeeInvoice) {
            val ctx = b.root.context

            b.tvMonth.text = item.billingMonthLabel ?: item.billingMonth

            // ── DUE: always the full monthly fee ─────────────────────────────
            // amountDue is the original invoice amount and never changes,
            // regardless of whether a partial payment has been made.
            b.tvAmountDue.text = item.amountDue.toCurrency()

            // ── PAID: actual amount received from the student ────────────────
            // Comes directly from the server. Shows 0 ৳ until any payment is made.
            b.tvAmountPaid.text = item.amountPaid.toCurrency()

            // ── BALANCE: computed locally = full fee − paid ──────────────────
            // We do NOT use outstandingAmount from the server because it can
            // differ if the server applies discounts or rounding adjustments.
            val balance = (item.amountDue - item.amountPaid).coerceAtLeast(0.0)
            b.tvOutstanding.text = balance.toCurrency()

            // ── Due date ──────────────────────────────────────────────────────
            if (!item.dueDate.isNullOrBlank()) {
                b.tvDueDate.text = "Due: ${item.dueDate}"
                b.tvDueDate.visibility = View.VISIBLE
            } else {
                b.tvDueDate.visibility = View.GONE
            }

            // ── Status badge + balance colour ─────────────────────────────────
            // Status comes from the server:
            //   "paid"    → only when the full amount has been received
            //   "partial" → some payment made but not full amount
            //   anything else → nothing paid yet (pending)
            when (item.status.lowercase()) {
                "paid" -> {
                    b.tvStatus.text = "PAID ✓"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_success)
                    // Balance is 0 — show in muted colour
                    b.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
                }
                "partial" -> {
                    b.tvStatus.text = "PARTIAL"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_warning)
                    // Remaining balance in amber
                    b.tvOutstanding.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                }
                else -> {
                    b.tvStatus.text = "PENDING"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
                    b.tvStatus.background = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_danger)
                    // Full amount still owed — show in red
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

// ─── Payment adapter (unchanged — shows actual payment records) ───────────────

class PaymentAdapter : ListAdapter<FeePayment, PaymentAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemPaymentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeePayment) {
            b.tvAmount.text       = item.amount.toCurrency()
            b.tvDate.text         = item.paymentDate
            b.tvMode.text         = item.paymentMode?.replace("_", " ")?.uppercase() ?: "N/A"
            b.tvReceipt.text      = item.receiptNumber?.let { "Receipt: $it" } ?: ""
            b.tvRef.text          = item.reference?.let { "Ref: $it" } ?: ""
            b.tvInvoiceMonth.text = item.invoice?.billingMonthLabel
                ?: item.invoice?.billingMonth
                ?: ""
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
