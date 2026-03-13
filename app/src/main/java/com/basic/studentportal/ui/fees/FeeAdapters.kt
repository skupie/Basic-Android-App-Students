package com.basic.studentportal.ui.fees

import android.view.LayoutInflater
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
            b.tvMonth.text = item.billingMonthLabel ?: item.billingMonth
            b.tvAmountDue.text = item.amountDue.toCurrency()
            b.tvAmountPaid.text = item.amountPaid.toCurrency()
            b.tvOutstanding.text = item.outstandingAmount.toCurrency()
            b.tvStatus.text = item.status.uppercase()
            val color = when (item.status) {
                "paid" -> R.color.status_active
                "partial" -> R.color.status_warning
                else -> R.color.status_inactive
            }
            b.tvStatus.setTextColor(ContextCompat.getColor(b.root.context, color))
            b.tvDueDate.text = "Due: ${item.dueDate ?: "-"}"
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

class PaymentAdapter : ListAdapter<FeePayment, PaymentAdapter.VH>(DiffCb()) {
    inner class VH(private val b: ItemPaymentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: FeePayment) {
            b.tvAmount.text = item.amount.toCurrency()
            b.tvDate.text = item.paymentDate
            b.tvMode.text = item.paymentMode?.replace("_", " ")?.uppercase() ?: "N/A"
            b.tvReceipt.text = item.receiptNumber?.let { "Receipt: $it" } ?: ""
            b.tvRef.text = item.reference?.let { "Ref: $it" } ?: ""
            b.tvInvoiceMonth.text = item.invoice?.billingMonthLabel ?: item.invoice?.billingMonth ?: ""
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
