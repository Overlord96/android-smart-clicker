package com.smartclicker.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smartclicker.app.R
import com.smartclicker.app.databinding.ItemLogBinding
import com.smartclicker.app.model.LogEntry

class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val logs = mutableListOf<LogEntry>()

    fun addLog(entry: LogEntry) {
        logs.add(0, entry)
        if (logs.size > 200) {
            logs.removeAt(logs.size - 1)
        }
        notifyItemInserted(0)
    }

    fun clear() {
        val size = logs.size
        logs.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.tvTimestamp.text = entry.timestamp
            binding.tvMessage.text = entry.message
            val colorRes = if (entry.isSuccess) R.color.text_primary else R.color.status_red
            binding.tvMessage.setTextColor(
                ContextCompat.getColor(binding.root.context, colorRes)
            )
        }
    }
}
