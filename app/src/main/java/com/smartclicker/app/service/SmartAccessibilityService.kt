package com.smartclicker.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.smartclicker.app.model.AutomationRule
import com.smartclicker.app.model.LogEntry
import com.smartclicker.app.vision.VisionAutomationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmartAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val visionEngine = VisionAutomationEngine()

    private var lastClickTimestamp = 0L
    private val clickDebounceCache = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceBound.value = true
        if (_automationRules.value.isEmpty()) {
            _automationRules.value = AutomationRule.loadRules(this)
        }
        emitLog("Accessibility Service connected and ready")
        Log.i(TAG, "SmartAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !_isAutomationRunning.value) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleNodeTreeScan()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "SmartAccessibilityService interrupted.")
        emitLog("Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceBound.value = false
        _isAutomationRunning.value = false
        serviceScope.cancel()
        Log.i(TAG, "SmartAccessibilityService destroyed.")
    }

    /**
     * Recursively traverses accessibility node hierarchy and executes programmatic clicks
     * on matching trigger elements.
     */
    private fun handleNodeTreeScan() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTimestamp < _clickCooldownMs.value) {
            return
        }

        val root = rootInActiveWindow ?: return

        var clickedAny = false
        try {
            if (_isNodeEngineEnabled.value) {
                clickedAny = evaluateNodeHierarchy(root)
            }

            // Vision fallback for canvas/Unity games when no accessibility elements matched
            if (!clickedAny && _isVisionEngineEnabled.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                evaluateVisionFallback()
            }
        } finally {
            try {
                @Suppress("DEPRECATION")
                root.recycle()
            } catch (e: Exception) {
                // Ignore recycling exceptions across Android versions
            }
        }
    }

    private fun evaluateNodeHierarchy(root: AccessibilityNodeInfo): Boolean {
        val activeRules = _automationRules.value.filter { it.isEnabled }
        val matchedNodes = mutableListOf<Pair<AccessibilityNodeInfo, AutomationRule>>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            val nodeText = node.text?.toString() ?: ""
            val nodeDesc = node.contentDescription?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""

            for (rule in activeRules) {
                val matchesText = rule.triggerTexts.any { trigger ->
                    nodeText.contains(trigger, ignoreCase = true) ||
                    nodeDesc.contains(trigger, ignoreCase = true)
                }
                val matchesId = rule.targetViewId != null && viewId.contains(rule.targetViewId, ignoreCase = true)

                if (matchesText || matchesId) {
                    matchedNodes.add(Pair(node, rule))
                    return
                }
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        traverse(root)

        val currentTime = System.currentTimeMillis()
        for ((node, rule) in matchedNodes) {
            val cacheKey = "${rule.id}_${node.text}_${node.viewIdResourceName}"
            val lastClicked = clickDebounceCache[cacheKey] ?: 0L

            if (currentTime - lastClicked >= rule.clickCooldownMs) {
                val clicked = performProgrammaticClick(node, rule)
                if (clicked) {
                    clickDebounceCache[cacheKey] = currentTime
                    lastClickTimestamp = currentTime
                    return true
                }
            }
        }

        return false
    }

    /**
     * Executes programmatic click (AccessibilityNodeInfo.ACTION_CLICK) or fallback coordinate tap.
     */
    private fun performProgrammaticClick(node: AccessibilityNodeInfo, rule: AutomationRule): Boolean {
        var targetNode: AccessibilityNodeInfo? = node
        while (targetNode != null && !targetNode.isClickable) {
            targetNode = targetNode.parent
        }

        val clicked = if (targetNode != null && targetNode.isClickable) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // Coordinate-based programmatic tap fallback via GestureDescription
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val tapX = rect.centerX().toFloat()
                val tapY = rect.centerY().toFloat()
                dispatchTapGesture(tapX, tapY)
                true
            } else {
                false
            }
        }

        if (clicked) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val desc = node.text ?: node.contentDescription ?: node.viewIdResourceName ?: "element"
            emitLog("Trigger [${rule.name}] clicked on '$desc' at (${rect.centerX()}, ${rect.centerY()})")
        }

        return clicked
    }

    /**
     * Vision fallback: captures screenshot (API 30+) for custom OpenGL/Unity canvas rendering.
     */
    private fun evaluateVisionFallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                applicationContext.mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        val hardwareBuffer = screenshotResult.hardwareBuffer
                        val colorSpace = screenshotResult.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()

                        if (bitmap != null) {
                            serviceScope.launch {
                                processScreenshotWithVision(bitmap)
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.d(TAG, "Screenshot capture skipped or failed: code $errorCode")
                    }
                }
            )
        }
    }

    private fun processScreenshotWithVision(bitmap: Bitmap) {
        // Fast template or trigger search
        // For demonstration, scan for action buttons by target high-contrast regions
        emitLog("Vision engine analyzed screenshot (${bitmap.width}x${bitmap.height})")
    }

    /**
     * Dispatches simulated tap gesture at screen coordinates (x, y).
     */
    fun dispatchTapGesture(x: Float, y: Float) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 50))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Gesture completed at ($x, $y)")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Gesture cancelled at ($x, $y)")
            }
        }, mainHandler)
    }

    companion object {
        private const val TAG = "SmartAccessibility"

        var instance: SmartAccessibilityService? = null
            private set

        private val _isServiceBound = MutableStateFlow(false)
        val isServiceBound = _isServiceBound.asStateFlow()

        private val _isAutomationRunning = MutableStateFlow(false)
        val isAutomationRunning = _isAutomationRunning.asStateFlow()

        private val _isNodeEngineEnabled = MutableStateFlow(true)
        val isNodeEngineEnabled = _isNodeEngineEnabled.asStateFlow()

        private val _isVisionEngineEnabled = MutableStateFlow(false)
        val isVisionEngineEnabled = _isVisionEngineEnabled.asStateFlow()

        private val _clickCooldownMs = MutableStateFlow(500L)
        val clickCooldownMs = _clickCooldownMs.asStateFlow()

        private val _automationRules = MutableStateFlow<List<AutomationRule>>(emptyList())
        val automationRules = _automationRules.asStateFlow()

        private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 100)
        val logFlow = _logFlow.asSharedFlow()

        fun updateRules(rules: List<AutomationRule>) {
            _automationRules.value = rules
        }

        fun setAutomationRunning(running: Boolean) {
            _isAutomationRunning.value = running
            emitLog(if (running) "Automation started" else "Automation stopped")
        }

        fun setNodeEngineEnabled(enabled: Boolean) {
            _isNodeEngineEnabled.value = enabled
        }

        fun setVisionEngineEnabled(enabled: Boolean) {
            _isVisionEngineEnabled.value = enabled
        }

        fun setClickCooldown(cooldownMs: Long) {
            _clickCooldownMs.value = cooldownMs
        }

        fun emitLog(message: String, isSuccess: Boolean = true) {
            _logFlow.tryEmit(LogEntry(message = message, isSuccess = isSuccess))
        }
    }
}
