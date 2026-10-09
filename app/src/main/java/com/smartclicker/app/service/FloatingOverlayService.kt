package com.smartclicker.app.service

import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.smartclicker.app.R
import com.smartclicker.app.model.TargetPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager

    // Floating Control Bar
    private var controlBarView: View? = null
    private lateinit var controlBarParams: WindowManager.LayoutParams
    private var isPlaying = false
    private var clickLoopJob: Job? = null

    // Target Points
    private val targetPoints = mutableListOf<TargetPoint>()
    private val targetViews = mutableListOf<View>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        _isOverlayRunning.value = true

        initControlBar()
        SmartAccessibilityService.emitLog("Floating overlay widget launched")
    }

    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun initControlBar() {
        val inflater = LayoutInflater.from(this)
        controlBarView = inflater.inflate(R.layout.layout_floating_control_bar, null)

        controlBarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        windowManager.addView(controlBarView, controlBarParams)

        setupControlBarTouchListener()
        setupControlBarButtons()
    }

    private fun setupControlBarTouchListener() {
        val root = controlBarView?.findViewById<View>(R.id.floatingBarRoot) ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = controlBarParams.x
                    initialY = controlBarParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    controlBarParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    controlBarParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(controlBarView, controlBarParams)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupControlBarButtons() {
        val view = controlBarView ?: return
        val btnPlayPause = view.findViewById<ImageButton>(R.id.btnPlayPause)
        val btnAddPoint = view.findViewById<ImageButton>(R.id.btnAddPoint)
        val btnRemovePoint = view.findViewById<ImageButton>(R.id.btnRemovePoint)
        val btnSettings = view.findViewById<ImageButton>(R.id.btnSettings)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        btnPlayPause.setOnClickListener {
            togglePlayPause(btnPlayPause)
        }

        btnAddPoint.setOnClickListener {
            addTargetPoint()
        }

        btnRemovePoint.setOnClickListener {
            removeLastTargetPoint()
        }

        btnSettings.setOnClickListener {
            showGlobalSettingsDialog()
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    private fun togglePlayPause(button: ImageButton) {
        if (!SmartAccessibilityService.isServiceBound.value) {
            Toast.makeText(this, R.string.log_permission_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (targetPoints.isEmpty()) {
            Toast.makeText(this, "Add at least 1 target point using (+)", Toast.LENGTH_SHORT).show()
            return
        }

        isPlaying = !isPlaying

        if (isPlaying) {
            button.setImageResource(R.drawable.ic_pause)
            SmartAccessibilityService.emitLog("Point-based automation started with ${targetPoints.size} points")
            startClickLoop()
        } else {
            button.setImageResource(R.drawable.ic_play)
            SmartAccessibilityService.emitLog("Point-based automation paused")
            stopClickLoop()
        }
    }

    private fun startClickLoop() {
        clickLoopJob?.cancel()
        clickLoopJob = serviceScope.launch {
            while (isActive && isPlaying) {
                for (point in targetPoints.toList()) {
                    if (!isActive || !isPlaying) break

                    SmartAccessibilityService.instance?.dispatchTapGesture(point.x, point.y)
                    SmartAccessibilityService.emitLog(
                        "Point #${point.id} tapped at (${point.x.toInt()}, ${point.y.toInt()})"
                    )

                    delay(point.delayMs.coerceAtLeast(50L))
                }
            }
        }
    }

    private fun stopClickLoop() {
        clickLoopJob?.cancel()
        clickLoopJob = null
    }

    private fun addTargetPoint() {
        val newIndex = targetPoints.size + 1
        val inflater = LayoutInflater.from(this)
        val pointView = inflater.inflate(R.layout.layout_target_point, null)

        val tvIndex = pointView.findViewById<TextView>(R.id.tvPointIndex)
        tvIndex.text = newIndex.toString()

        val displayMetrics = resources.displayMetrics
        val initialX = (displayMetrics.widthPixels / 2) + (newIndex * 30) - 100
        val initialY = (displayMetrics.heightPixels / 2) + (newIndex * 30) - 100

        val pointParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val targetPoint = TargetPoint(
            id = newIndex,
            x = (initialX + 50).toFloat(),
            y = (initialY + 50).toFloat(),
            delayMs = 500L
        )

        setupTargetPointTouch(pointView, pointParams, targetPoint)

        windowManager.addView(pointView, pointParams)
        targetPoints.add(targetPoint)
        targetViews.add(pointView)

        SmartAccessibilityService.emitLog("Added target point #$newIndex at ($initialX, $initialY)")
    }

    private fun setupTargetPointTouch(
        view: View,
        params: WindowManager.LayoutParams,
        targetPoint: TargetPoint
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)

                    // Target coordinate is the center of the badge
                    targetPoint.x = (params.x + view.width / 2).toFloat()
                    targetPoint.y = (params.y + view.height / 2).toFloat()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        showPointSettingsDialog(targetPoint)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun removeLastTargetPoint() {
        if (targetPoints.isNotEmpty() && targetViews.isNotEmpty()) {
            val lastView = targetViews.removeAt(targetViews.size - 1)
            val lastPoint = targetPoints.removeAt(targetPoints.size - 1)
            windowManager.removeView(lastView)
            SmartAccessibilityService.emitLog("Removed target point #${lastPoint.id}")
        }
    }

    private fun showPointSettingsDialog(point: TargetPoint) {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_SmartClicker)
        val builder = AlertDialog.Builder(themedContext)
        val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_point_settings, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvPointDialogTitle)
        val etDelay = dialogView.findViewById<EditText>(R.id.etPointDelay)

        tvTitle.text = getString(R.string.target_point_settings, point.id)
        etDelay.setText(point.delayMs.toString())

        builder.setView(dialogView)
        builder.setPositiveButton(R.string.save) { _, _ ->
            val delayValue = etDelay.text.toString().toLongOrNull() ?: 500L
            point.delayMs = delayValue
            SmartAccessibilityService.emitLog("Target point #${point.id} delay updated to ${delayValue}ms")
        }
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()
        dialog.window?.setType(getOverlayType())
        dialog.show()
    }

    private fun showGlobalSettingsDialog() {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_SmartClicker)
        val builder = AlertDialog.Builder(themedContext)
        builder.setTitle(R.string.floating_overlay_title)
        builder.setMessage("Target Points Active: ${targetPoints.size}\nUse (+) to place targets, tap any point to edit delay.")
        builder.setPositiveButton(R.string.save, null)
        val dialog = builder.create()
        dialog.window?.setType(getOverlayType())
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isOverlayRunning.value = false
        stopClickLoop()

        // Clean up all overlay views
        controlBarView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        controlBarView = null

        for (v in targetViews) {
            try {
                windowManager.removeView(v)
            } catch (e: Exception) {}
        }
        targetViews.clear()
        targetPoints.clear()

        serviceScope.cancel()
        SmartAccessibilityService.emitLog("Floating overlay widget closed")
    }

    companion object {
        var instance: FloatingOverlayService? = null
            private set

        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning = _isOverlayRunning.asStateFlow()

        fun stopOverlay(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            context.stopService(intent)
        }
    }
}
