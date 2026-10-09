package com.smartclicker.app.model

data class TargetPoint(
    val id: Int,
    var x: Float,
    var y: Float,
    var delayMs: Long = 500L,
    var clickDurationMs: Long = 50L
)
