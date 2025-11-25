package com.example.oldflex

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.oldflex.R

class DeliveryOfferFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_offer, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("DeliveryPrefs", Context.MODE_PRIVATE)

        // Show loading overlay immediately
        val loadingOverlay = view.findViewById<FrameLayout>(R.id.loadingOverlay)
        loadingOverlay.visibility = View.GONE
        loadingOverlay.isClickable = false
        loadingOverlay.isFocusable = false

        // Hide after 400ms

        Handler(Looper.getMainLooper()).postDelayed({
            loadingOverlay.visibility = View.GONE
            loadingOverlay.isClickable = false
            loadingOverlay.isFocusable = false
        }, 800)


        setupContent(view)
        return view
    }

    private fun setupContent(view: View) {
        val location = arguments?.getString("location") ?: "Unknown location"
        val timeRange = arguments?.getString("timeRange") ?: "Unknown time"
        val duration = arguments?.getString("duration") ?: "Unknown duration"
        val price = arguments?.getString("price") ?: "$0.00"

        val address = when {
            location.contains("Seattle") -> "6705 E Marginal Way S, Seattle, WA 98108"
            location.contains("Portland") -> "5647 NE Huffman St, Hillsboro, OR 97124"
            location.contains("Eugene") -> "5647 NE Huffman St, Hillsboro, OR 97124"
            location.contains("Salem") -> "5647 NE Huffman St, Hillsboro, OR 97124"
            location.contains("Everett") -> "6611 Associated Blvd, Everett, WA 98203"
            location.contains("Tacoma") -> "2309 Milwaukee Way, Tacoma, WA 98421"
            else -> "Unknown address"
        }
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1) // Add 1 day for tomorrow

        val dateFormat = java.text.SimpleDateFormat("EEEE, M/d", java.util.Locale.ENGLISH)
        val date = dateFormat.format(calendar.time)

        view.findViewById<TextView>(R.id.tvTitle).text = location
        view.findViewById<TextView>(R.id.tvAddress).text = address
        view.findViewById<TextView>(R.id.tvDateTime).text = date
        view.findViewById<TextView>(R.id.tvTime).text = "$timeRange • ($duration)"
        view.findViewById<TextView>(R.id.rvPrice).text = price
        view.findViewById<TextView>(R.id.rvPriceDescription).text =
            "Amazon's contribution is $price for delivering this block, this block is not eligible for in-app customer tips"

        view.findViewById<Button>(R.id.btnDecline).setOnClickListener {
            Log.d("declineHere","clicked")
            requireActivity().onBackPressed()
        }

        view.findViewById<Button>(R.id.button).setOnClickListener {
            val loadingOverlay = view.findViewById<FrameLayout>(R.id.loadingOverlay)


            // Check if fragment is still added before proceeding
            if (!isAdded) {
                loadingOverlay.visibility = View.GONE
                return@setOnClickListener
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if (!isAdded) {
                    loadingOverlay.visibility = View.GONE
                    return@postDelayed
                }

                // Save to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("scheduled_location", location)
                    putString("scheduled_time", "$timeRange • ($duration)")
                    putString("scheduled_price", price)
                    putString("scheduled_address", address)
                    putString("scheduled_date", date)
                    apply() // or commit() if you need immediate write
                }

                val resultBundle = Bundle().apply {
                    putString("scheduled_location", location)
                    putString("scheduled_time", "$timeRange • ($duration)")
                    putString("scheduled_price", price)
                }

                parentFragmentManager.setFragmentResult("delivery_scheduled", resultBundle)
                requireActivity().onBackPressed()
            }, 50)
        }

        view.findViewById<View>(R.id.menuIcon).setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}