package com.example.oldflex

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        // Initialize SharedPreferences
        val sharedPreferences = getSharedPreferences("DeliveryPrefs", MODE_PRIVATE)

        // Retrieve values from SharedPreferences
        val location = sharedPreferences.getString("scheduled_location", "Unknown location")
        val timeRange = sharedPreferences.getString("scheduled_time", "Unknown time")
        val price = sharedPreferences.getString("scheduled_price", "$0.00")
        val address = sharedPreferences.getString("scheduled_address", "Unknown address")
        val date = sharedPreferences.getString("scheduled_date", "Unknown date")

        // Split the timeRange into time and duration parts
        val timeParts = timeRange?.split(" • ") ?: listOf("", "")
        val time = timeParts.getOrElse(0) { "" }
        val duration = timeParts.getOrElse(1) { "" }

        val titleText = findViewById<TextView>(R.id.titleText)
        val dateText = findViewById<TextView>(R.id.dateText)
        val locationText = findViewById<TextView>(R.id.locationText)
        val scheduleTypeText = findViewById<TextView>(R.id.scheduleTypeText)
        val timeRangeText = findViewById<TextView>(R.id.timeRangeText)
        val durationText = findViewById<TextView>(R.id.durationText)
        val payText = findViewById<TextView>(R.id.payText)

        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        menuIcon.setOnClickListener {
            // Open drawer, show menu, or do whatever you need here
        }
        if(location != null)
        {
            val locationParts = location.split(" - ")

            val formattedLocation = if (locationParts.size > 1) locationParts[0] + " -" else location
            val scheduleType = if (locationParts.size > 1) locationParts[1] else "Sub Same-Day"
            titleText.text = "Your Schedule"
            dateText.text = date
            locationText.text = formattedLocation
            scheduleTypeText.text = scheduleType
            timeRangeText.text = time
            durationText.text = duration.replace(Regex("[()]"), "") // Remove parentheses if present
            payText.text = price
        }


        // Optional: If you want to use the address somewhere
        // val addressText = findViewById<TextView>(R.id.addressText) // if you have this view
        // addressText.text = address
    }
}