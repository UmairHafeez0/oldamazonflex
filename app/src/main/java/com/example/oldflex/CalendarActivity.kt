package com.example.oldflex

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CalendarActivity : AppCompatActivity(), CalendarAdapter.OnDateClickListener {

    private lateinit var monthYearText: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private var currentDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_layout) // Changed to your new layout file

        // Set up views
        monthYearText = findViewById(R.id.monthYearText1)
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView1)



        findViewById<ImageView>(R.id.nextMonth1).setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        // Initialize calendar
        updateCalendar()
    }

    private fun updateCalendar() {
        // Update month/year text
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // Added year
        monthYearText.text = monthYearFormat.format(currentDate.time)

        // Generate days for the current month
        val calendarDays = generateCalendarDays(currentDate)

        // Set up RecyclerView
        val layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.layoutManager = layoutManager

        val calendarAdapter = CalendarAdapter(calendarDays, this)
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun generateCalendarDays(calendar: Calendar): ArrayList<CalendarDay> {
        val days = ArrayList<CalendarDay>()

        // Clone calendar to not affect the original
        val tempCalendar = calendar.clone() as Calendar

        // Set to first day of month
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        // Get the day of week for the first day (1=Sunday, 2=Monday, etc.)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)

        // Add empty days for alignment
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(""))
        }

        // Get last day of month
        val lastDay = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Add all days of month
        for (day in 1..lastDay) {
            // Check if the day should have an indicator (modify this logic as needed)
            val hasIndicator = shouldShowIndicator(tempCalendar, day)
            days.add(CalendarDay(day.toString(), hasIndicator))
        }

        // Add empty days at the end if needed
        val totalCells = if (days.size <= 35) 35 else 42
        while (days.size < totalCells) {
            days.add(CalendarDay(""))
        }

        return days
    }

    private fun shouldShowIndicator(calendar: Calendar, day: Int): Boolean {
        // Create a Calendar instance for tomorrow
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1) // Move to tomorrow

        // Create a Calendar instance for the current date being checked
        val currentDay = calendar.clone() as Calendar
        currentDay.set(Calendar.DAY_OF_MONTH, day)

        // Compare year, month and day
        return currentDay.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                currentDay.get(Calendar.MONTH) == tomorrow.get(Calendar.MONTH) &&
                currentDay.get(Calendar.DAY_OF_MONTH) == tomorrow.get(Calendar.DAY_OF_MONTH)
    }
    override fun onDateClick(date: String) {
        if (date.isNotEmpty()) {
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.putExtra("SELECTED_DATE", "${currentDate.get(Calendar.MONTH) + 1}/$date/${currentDate.get(Calendar.YEAR)}")
            startActivity(intent)
        }
    }
}