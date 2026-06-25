package com.example.notificationhistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var notifications = emptyList<NotificationEntity>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        holder.tvAppName.text = item.appName
        holder.tvTitle.text = item.title ?: "No Title"
        holder.tvText.text = item.text ?: "No Content"
        
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(item.timestamp))
    }

    override fun getItemCount() = notifications.size

    fun submitList(list: List<NotificationEntity>) {
        this.notifications = list
        notifyDataSetChanged()
    }
}
