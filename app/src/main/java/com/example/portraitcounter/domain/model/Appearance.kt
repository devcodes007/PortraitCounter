package com.example.portraitcounter.domain.model

data class Appearance(
    val trackingId: Int?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val detections: List<DetectedFace>
) {
    val durationMs: Long
        get() = endTimeMs - startTimeMs
}