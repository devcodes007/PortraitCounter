package com.example.portraitcounter.domain.model

import kotlin.math.abs

data class Appearance(
    val trackingId: Int?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val detections: List<DetectedFace>
) {

    val durationMs: Long
        get() = endTimeMs - startTimeMs

    /**
     * The timestamp of the strongest frame in this appearance.
     *
     * We prefer:
     * - frontal head pose
     * - open eyes
     * - pleasant expression
     * - larger face
     */
    val representativeTimestampMs: Long
        get() {
            return detections
                .maxByOrNull { detection ->
                    qualityScore(detection)
                }
                ?.timestampMs
                ?: startTimeMs
        }

    private fun qualityScore(
        detection: DetectedFace
    ): Float {

        // Frontal face is preferred.
        val poseScore =
            (1f - abs(detection.headEulerY) / 45f)
                .coerceIn(0f, 1f) *
                    0.35f

        // Prefer faces with both eyes open.
        val eyeScore = if (
            detection.leftEyeOpenProbability != null &&
            detection.rightEyeOpenProbability != null
        ) {
            (
                    (detection.leftEyeOpenProbability +
                            detection.rightEyeOpenProbability) / 2f
                    ) * 0.30f
        } else {
            0f
        }

        // Prefer a pleasant / smiling expression,
        // but don't require smiling.
        val smileScore =
            (detection.smilingProbability ?: 0f) * 0.15f

        // Prefer a frontal roll angle as well.
        val rollScore =
            (1f - abs(detection.headEulerZ) / 45f)
                .coerceIn(0f, 1f) *
                    0.20f

        return poseScore +
                eyeScore +
                smileScore +
                rollScore
    }
}