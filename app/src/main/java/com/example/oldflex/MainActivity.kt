package com.example.oldflex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.oldflex.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var windowManager: WindowManager
    private var overlayButton: Button? = null
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager


        // Set an OnClickListener for the root view to capture click positions
        binding.root.setOnClickListener { view ->
            val x = view.width / 2 // Center of the view
            val y = view.height / 2 // Center of the view
 //           Toast.makeText(this, "Clicked at position: ($x, $y)", Toast.LENGTH_SHORT).show()
        }

        // Alternatively, you can use OnTouchListener to get exact touch positions
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt() // Get X coordinate of touch
                val y = event.y.toInt() // Get Y coordinate of touch
      //          Toast.makeText(this, "Clicked at position: ($x, $y)", Toast.LENGTH_SHORT).show()
            }
            false // Return false to allow other touch events to be handled
        }
    }

    private fun showToastWithCardViewPosition(cardView: CardView) {
        val position = IntArray(2)
        cardView.getLocationOnScreen(position)
        val x = position[0]
        val y = position[1]
            //     Toast.makeText(this, "CardView is clicked at position: ($x, $y)", Toast.LENGTH_SHORT).show()
    }

    private fun showOverlayButton() {
        if (overlayButton == null) {
            overlayButton = Button(this).apply {
                text = "Start AI"
                setBackgroundResource(R.drawable.rounded_button) // Use the custom drawable
                setTextColor(resources.getColor(android.R.color.white))
                setPadding(30, 20, 30, 20) // Adjust padding for better appearance
                textSize = 13f // Increase the text size for better readability
                elevation = 10f // Add a shadow effect for depth
            }

            // Set layout parameters for the overlay button
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FORMAT_CHANGED
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 300 // Adjust this value to control the distance from the bottom
            }

            // Add the overlay button to the window
            windowManager.addView(overlayButton, params)

            overlayButton?.setOnClickListener {
                if (overlayButton?.text == "Start AI") {
                    overlayButton?.text = "AI Working..."
                    startService(Intent(this, UtilityBotServiceA::class.java))
                } else {
                    overlayButton?.text = "Start AI"
                    stopService(Intent(this, UtilityBotServiceA::class.java))
                    Toast.makeText(this, "AI stopped.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideOverlayButton() {
        overlayButton?.let {
            windowManager.removeView(it)
            overlayButton = null
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            showOverlayButton()
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                showOverlayButton()
            } else {
                Toast.makeText(this, "Overlay permission is required!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlayButton()
    }
}
