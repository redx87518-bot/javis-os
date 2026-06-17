package com.javis.os.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.FrameLayout
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import com.javis.os.MainActivity
import com.javis.os.R
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastTouchTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        createFloatingBubble()
    }

    private fun createFloatingBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val container = FrameLayout(this).apply {
            val size = 60.dpToPx()

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC00D4FF"))
                setStroke(2.dpToPx(), Color.parseColor("#00D4FF"))
            }
            background = bg

            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params?.x ?: 0
                            initialY = params?.y ?: 0
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            lastTouchTime = System.currentTimeMillis()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params?.x = initialX + (event.rawX - initialTouchX).toInt()
                            params?.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        MotionEvent.ACTION_UP -> {
                            val deltaX = abs(event.rawX - initialTouchX)
                            val deltaY = abs(event.rawY - initialTouchY)
                            val duration = System.currentTimeMillis() - lastTouchTime
                            if (deltaX < 10f && deltaY < 10f && duration < 300) {
                                onBubbleTapped()
                            }
                        }
                    }
                    return true
                }
            })
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_javis)
            val padding = 14.dpToPx()
            setPadding(padding, padding, padding, padding)
        }
        container.addView(iconView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        floatingView = container

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            60.dpToPx(),
            60.dpToPx(),
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200.dpToPx()
        }

        windowManager?.addView(floatingView, params)
    }

    private fun onBubbleTapped() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.javis.os.ACTIVATE_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        floatingView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_SHOW = "com.javis.os.BUBBLE_SHOW"
        const val ACTION_HIDE = "com.javis.os.BUBBLE_HIDE"
    }
}
