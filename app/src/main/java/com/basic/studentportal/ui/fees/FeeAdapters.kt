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

class InvoiceAdapter(
    private var monthlyFee: Double = 0.0
) : ListAdapter<FeeInvoice, InvoiceAdapter.VH>(DiffCb()) {

    fun setMonthlyFee(fee: Double) {
        monthlyFee = fee
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemInvoiceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeeInvoice) {
            val ctx = b.root.context

            b.tvMonth.text = item.billingMonthLabel ?: item.billingMonth

            // Always show full monthly fee in invoice due field
            b.tvAmountDue.text = monthlyFee.toCurrency()

            // Paid amount stays actual from server
            b.tvAmountPaid.text = item.amountPaid.toCurrency()

            // Balance = actual invoice amount - paid amount
            val balance = (item.amountDue - item.amountPaid).coerceAtLeast(0.0)
            b.tvOutstanding.text = balance.toCurrency()

            if (!item.dueDate.isNullOrBlank()) {
                b.tvDueDate.text = "Due: ${item.dueDate}"
                b.tvDueDate.visibility = View.VISIBLE
            } else {
                b.tvDueDate.visibility = View.GONE
            }

            when (item.status.lowercase()) {
                "paid" -> {
                    b.tvStatus.text = "PAID ✓"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.success))
                    b.tvStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_pill_success)
                    b.tvOutstanding.setTextColor(
                        ContextCompat.getColor(ctx, R.color.text_hint)
                    )
                }

                "partial" -> {
                    b.tvStatus.text = "PARTIAL"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.warning))
                    b.tvStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_pill_warning)
                    b.tvOutstanding.setTextColor(
                        ContextCompat.getColor(ctx, R.color.warning)
                    )
                }

                else -> {
                    b.tvStatus.text = "PENDING"
                    b.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.danger))
                    b.tvStatus.background =
                        ContextCompat.getDrawable(ctx, R.drawable.bg_pill_danger)
                    b.tvOutstanding.setTextColor(
                        ContextCompat.getColor(ctx, R.color.danger)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<FeeInvoice>() {
        override fun areItemsTheSame(a: FeeInvoice, b: FeeInvoice) = a.id == b.id
        override fun areContentsTheSame(a: FeeInvoice, b: FeeInvoice) = a == b
    }
}

class PaymentAdapter : ListAdapter<FeePayment, PaymentAdapter.VH>(DiffCb()) {

    inner class VH(private val b: ItemPaymentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeePayment) {
            b.tvAmount.text = item.amount.toCurrency()
            b.tvDate.text = item.paymentDate
            b.tvMode.text = item.paymentMode?.replace("_", " ")?.uppercase() ?: "N/A"
            b.tvReceipt.text = item.receiptNumber?.let { "Receipt: $it" } ?: ""
            b.tvRef.text = item.reference?.let { "Ref: $it" } ?: ""
            b.tvInvoiceMonth.text = item.invoice?.billingMonthLabel
                ?: item.invoice?.billingMonth
                ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<FeePayment>() {
        override fun areItemsTheSame(a: FeePayment, b: FeePayment) = a.id == b.id
        override fun areContentsTheSame(a: FeePayment, b: FeePayment) = a == b
    }
}
