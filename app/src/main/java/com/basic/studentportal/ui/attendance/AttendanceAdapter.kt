package com.basic.studentportal.ui.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.basic.studentportal.R
import com.basic.studentportal.data.model.AttendanceRecord
import com.basic.studentportal.databinding.ItemAttendanceBinding

class AttendanceAdapter : ListAdapter<AttendanceRecord, AttendanceAdapter.VH>(DiffCb()) {

    inner class VH(private val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AttendanceRecord) {
            binding.tvDate.text = item.attendanceDate
            binding.tvStatus.text = item.status.uppercase()
            binding.tvCategory.text = item.category?.replace("_", " ")?.uppercase() ?: ""
            binding.tvNote.text = item.note ?: ""

            val color = when (item.status.lowercase()) {
                "present" -> R.color.status_active
                "absent" -> R.color.status_inactive
                "late" -> R.color.status_warning
                else -> R.color.colorOnSurfaceVariant
            }
            binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, color))
            val bg = when (item.status.lowercase()) {
                "present" -> R.color.status_active_bg
                "absent" -> R.color.status_inactive_bg
                "late" -> R.color.status_warning_bg
                else -> R.color.surface_variant
            }
            binding.ivStatusDot.setColorFilter(ContextCompat.getColor(binding.root.context, color))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class DiffCb : DiffUtil.ItemCallback<AttendanceRecord>() {
        override fun areItemsTheSame(a: AttendanceRecord, b: AttendanceRecord) = a.id == b.id
        override fun areContentsTheSame(a: AttendanceRecord, b: AttendanceRecord) = a == b
    }
}
