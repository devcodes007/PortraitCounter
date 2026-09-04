package com.example.portraitcounter.domain.model

import android.graphics.Rect

data class DetectedFace(
    val boundingBox: Rect,
    val trackingId: Int?,
    val timestampMs: Long,
    val headEulerX: Float,
    val headEulerY: Float,
    val headEulerZ: Float,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)