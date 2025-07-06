package com.krithikha.esp32swipe

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class SwipeAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SwipeService", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("COMMAND")?.let { command ->
            when (command) {
                "SWIPE_DOWN" -> performSwipeDown()
                "SWIPE_UP" -> performSwipeUp()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun performSwipeDown() {
        val path = Path().apply {
            moveTo(500f, 500f)
            lineTo(500f, 1500f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, null, null)
        Log.d("SwipeService", "Swipe Down Performed")
    }

    private fun performSwipeUp() {
        val path = Path().apply {
            moveTo(500f, 1500f)
            lineTo(500f, 500f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, null, null)
        Log.d("SwipeService", "Swipe Up Performed")
    }
}
