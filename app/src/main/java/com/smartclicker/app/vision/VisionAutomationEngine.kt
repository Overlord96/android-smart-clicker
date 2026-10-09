package com.smartclicker.app.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.util.Log

/**
 * Lightweight computer vision engine for detecting UI elements in custom canvas,
 * OpenGL, or Unity-based games where accessibility node trees are unavailable.
 */
class VisionAutomationEngine {

    companion object {
        private const val TAG = "VisionEngine"
    }

    data class MatchResult(
        val found: Boolean,
        val point: Point,
        val confidence: Float
    )

    /**
     * Fast template matching using pixel intensity difference.
     * Searches for [template] within [sourceBitmap].
     *
     * @param sourceBitmap The screenshot bitmap of the current game screen.
     * @param template Target UI element template bitmap.
     * @param matchThreshold Minimum confidence (0.0 to 1.0) to consider a valid match.
     * @param step Step size for scan sampling (higher = faster, lower = more accurate).
     */
    fun findTemplate(
        sourceBitmap: Bitmap,
        template: Bitmap,
        matchThreshold: Float = 0.85f,
        step: Int = 2
    ): MatchResult {
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height
        val tplW = template.width
        val tplH = template.height

        if (srcW < tplW || srcH < tplH || tplW == 0 || tplH == 0) {
            return MatchResult(found = false, point = Point(0, 0), confidence = 0f)
        }

        var bestScore = Float.MAX_VALUE
        var bestX = -1
        var bestY = -1

        // Pre-extract sample pixels from template for ultra-fast matching
        val samplePoints = mutableListOf<Triple<Int, Int, Int>>() // x, y, color
        val sampleStep = (step * 2).coerceAtLeast(1)
        for (y in 0 until tplH step sampleStep) {
            for (x in 0 until tplW step sampleStep) {
                samplePoints.add(Triple(x, y, template.getPixel(x, y)))
            }
        }

        if (samplePoints.isEmpty()) {
            return MatchResult(found = false, point = Point(0, 0), confidence = 0f)
        }

        val maxAllowedDiffPerPixel = 45.0 // Color difference tolerance

        for (y in 0..(srcH - tplH) step step) {
            for (x in 0..(srcW - tplW) step step) {
                var totalDiff = 0.0
                var rejected = false

                for ((tx, ty, tplColor) in samplePoints) {
                    val srcColor = sourceBitmap.getPixel(x + tx, y + ty)
                    val diff = colorDifference(tplColor, srcColor)
                    if (diff > maxAllowedDiffPerPixel * 2) {
                        rejected = true
                        break
                    }
                    totalDiff += diff
                }

                if (!rejected) {
                    val avgDiff = (totalDiff / samplePoints.size).toFloat()
                    if (avgDiff < bestScore) {
                        bestScore = avgDiff
                        bestX = x
                        bestY = y
                    }
                }
            }
        }

        // Convert difference score to confidence (0.0 to 1.0)
        val maxDiff = 255.0f * 3
        val confidence = (1.0f - (bestScore / maxDiff)).coerceIn(0f, 1f)

        return if (confidence >= matchThreshold && bestX >= 0 && bestY >= 0) {
            val centerX = bestX + tplW / 2
            val centerY = bestY + tplH / 2
            Log.d(TAG, "Match found at ($centerX, $centerY) with confidence $confidence")
            MatchResult(found = true, point = Point(centerX, centerY), confidence = confidence)
        } else {
            MatchResult(found = false, point = Point(0, 0), confidence = confidence)
        }
    }

    /**
     * Finds a color trigger within a target screen region.
     * Useful for health bars, energy meters, or static notification icons.
     */
    fun findColorTrigger(
        sourceBitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 30,
        startX: Int = 0,
        startY: Int = 0,
        width: Int = sourceBitmap.width,
        height: Int = sourceBitmap.height,
        step: Int = 4
    ): Point? {
        val endX = (startX + width).coerceAtMost(sourceBitmap.width)
        val endY = (startY + height).coerceAtMost(sourceBitmap.height)

        for (y in startY until endY step step) {
            for (x in startX until endX step step) {
                val pixelColor = sourceBitmap.getPixel(x, y)
                if (colorDifference(targetColor, pixelColor) <= tolerance) {
                    return Point(x, y)
                }
            }
        }
        return null
    }

    private fun colorDifference(c1: Int, c2: Int): Double {
        val rDiff = Color.red(c1) - Color.red(c2)
        val gDiff = Color.green(c1) - Color.green(c2)
        val bDiff = Color.blue(c1) - Color.blue(c2)
        return Math.sqrt((rDiff * rDiff + gDiff * gDiff + bDiff * bDiff).toDouble())
    }
}
