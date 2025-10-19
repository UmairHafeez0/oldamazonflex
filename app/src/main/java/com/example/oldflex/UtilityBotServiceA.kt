package com.example.oldflex

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class UtilityBotServiceA : Service() {

    private lateinit var handler: Handler
    private var isRunning = false
    private val delay: Long = 1000 // Refresh interval (1 second)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("UtilityBotService", "Service Started")

        isRunning = true
        handler = Handler(Looper.getMainLooper())
        handler.post(runnable)

        // Use START_NOT_STICKY if you don't want the service to restart automatically.
        return START_NOT_STICKY
    }

    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {


                handler.postDelayed(this, delay)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("UtilityBotService", "Service Stopped")
        isRunning = false

        // Check if handler has been initialized before calling removeCallbacks
        if (::handler.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }
}
