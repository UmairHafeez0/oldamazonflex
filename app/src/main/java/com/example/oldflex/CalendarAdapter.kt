package com.example.oldflex

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(
    private val days: List<CalendarDay>,
    private val listener: OnDateClickListener
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    interface OnDateClickListener {
        fun onDateClick(date: String)
    }

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.calendarDayText)
        val indicator: ImageView = itemView.findViewById(R.id.indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]

        if (day.day.isNotEmpty()) {
            holder.dayText.text = day.day
            holder.dayText.visibility = View.VISIBLE
        } else {
            holder.dayText.visibility = View.INVISIBLE
        }

        if (day.hasIndicator) {
            holder.indicator.visibility = View.VISIBLE
        } else {
            holder.indicator.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (day.day.isNotEmpty()) {
                listener.onDateClick(day.day)
            }
        }
    }

    override fun getItemCount() = days.size
}