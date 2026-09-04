package com.example.portraitcounter.data.ml

import com.example.portraitcounter.domain.model.Appearance
import com.example.portraitcounter.domain.model.DetectedFace

class AppearanceTracker(
    private val maxMissingFrames: Int = 2
) {

    private data class ActiveAppearance(
        val trackingId: Int?,
        val detections: MutableList<DetectedFace>,
        var missingFrames: Int = 0
    )

    private val activeAppearances = mutableMapOf<Int, ActiveAppearance>()
    private val completedAppearances = mutableListOf<Appearance>()

    fun processFrame(
        detections: List<DetectedFace>
    ) {
        val currentTrackingIds = detections
            .mapNotNull { it.trackingId }
            .toSet()

        // Mark active appearances as missing if their tracking ID
        // was not detected in this frame.
        val missingIds = activeAppearances.keys
            .filter { it !in currentTrackingIds }

        missingIds.forEach { trackingId ->
            val appearance = activeAppearances[trackingId]
                ?: return@forEach

            appearance.missingFrames++

            if (appearance.missingFrames > maxMissingFrames) {
                finishAppearance(trackingId)
            }
        }

        // Add current detections to their corresponding appearance.
        detections.forEach { detection ->
            val trackingId = detection.trackingId
                ?: return@forEach

            val existing = activeAppearances[trackingId]

            if (existing != null) {
                existing.detections.add(detection)
                existing.missingFrames = 0
            } else {
                activeAppearances[trackingId] = ActiveAppearance(
                    trackingId = trackingId,
                    detections = mutableListOf(detection)
                )
            }
        }
    }

    fun finish(): List<Appearance> {
        activeAppearances.keys.toList().forEach { trackingId ->
            finishAppearance(trackingId)
        }

        return completedAppearances.toList()
    }

    private fun finishAppearance(trackingId: Int) {
        val active = activeAppearances.remove(trackingId)
            ?: return

        if (active.detections.isEmpty()) {
            return
        }

        completedAppearances.add(
            Appearance(
                trackingId = active.trackingId,
                startTimeMs = active.detections.first().timestampMs,
                endTimeMs = active.detections.last().timestampMs,
                detections = active.detections.toList()
            )
        )
    }
}