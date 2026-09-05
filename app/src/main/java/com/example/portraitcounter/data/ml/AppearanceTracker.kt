package com.example.portraitcounter.data.ml

import android.graphics.Rect
import com.example.portraitcounter.domain.model.Appearance
import com.example.portraitcounter.domain.model.DetectedFace
import kotlin.math.max

class AppearanceTracker(
    private val maxMissingFrames: Int = 2,
    private val minimumIoU: Float = 0.20f
) {

    private data class ActiveAppearance(
        val id: Int,
        var trackingId: Int?,
        val detections: MutableList<DetectedFace>,
        var missingFrames: Int = 0
    )

    private val activeAppearances = mutableListOf<ActiveAppearance>()
    private val completedAppearances = mutableListOf<Appearance>()

    private var nextAppearanceId = 1

    fun processFrame(
        detections: List<DetectedFace>
    ) {

        // Keep track of which active appearances have already
        // been matched to a detection in this frame.
        val matchedAppearanceIds = mutableSetOf<Int>()

        /*
         * ---------------------------------------------------------
         * STEP 1
         * Try exact ML Kit tracking-ID matching.
         * ---------------------------------------------------------
         */

        detections.forEach { detection ->

            val trackingId = detection.trackingId

            if (trackingId == null) {
                return@forEach
            }

            val existing = activeAppearances.firstOrNull {
                it.id !in matchedAppearanceIds &&
                        it.trackingId == trackingId
            }

            if (existing != null) {

                existing.detections.add(detection)
                existing.trackingId = trackingId
                existing.missingFrames = 0

                matchedAppearanceIds.add(existing.id)
            }
        }

        /*
         * ---------------------------------------------------------
         * STEP 2
         * For unmatched detections, use spatial matching.
         *
         * This handles cases where ML Kit temporarily loses or
         * changes a tracking ID while the person remains visible.
         * ---------------------------------------------------------
         */

        detections.forEach { detection ->

            if (
                detection.trackingId != null &&
                activeAppearances.any {
                    it.id in matchedAppearanceIds &&
                            it.trackingId == detection.trackingId
                }
            ) {
                return@forEach
            }

            val candidate = activeAppearances
                .filter { it.id !in matchedAppearanceIds }
                .maxByOrNull { appearance ->
                    calculateIoU(
                        appearance.detections.last().boundingBox,
                        detection.boundingBox
                    )
                }

            if (
                candidate != null &&
                calculateIoU(
                    candidate.detections.last().boundingBox,
                    detection.boundingBox
                ) >= minimumIoU
            ) {

                candidate.detections.add(detection)

                // If ML Kit has now provided a new tracking ID,
                // remember it for future frames.
                candidate.trackingId = detection.trackingId
                candidate.missingFrames = 0

                matchedAppearanceIds.add(candidate.id)
            }
        }

        /*
         * ---------------------------------------------------------
         * STEP 3
         * Any active appearance that wasn't matched this frame
         * becomes temporarily missing.
         * ---------------------------------------------------------
         */

        val unmatchedAppearances = activeAppearances
            .filter { it.id !in matchedAppearanceIds }
            .toList()

        unmatchedAppearances.forEach { appearance ->

            appearance.missingFrames++

            if (appearance.missingFrames > maxMissingFrames) {
                finishAppearance(appearance)
            }
        }

        /*
         * ---------------------------------------------------------
         * STEP 4
         * Any detection that still hasn't been matched starts
         * a new appearance.
         * ---------------------------------------------------------
         */

        detections.forEach { detection ->

            val alreadyMatched = activeAppearances.any { appearance ->
                appearance.id in matchedAppearanceIds &&
                        appearance.detections.last() === detection
            }

            if (alreadyMatched) {
                return@forEach
            }

            // Check using tracking ID as a final safeguard.
            val sameTrackingIdAlreadyUsed =
                detection.trackingId != null &&
                        activeAppearances.any {
                            it.id in matchedAppearanceIds &&
                                    it.trackingId == detection.trackingId
                        }

            if (sameTrackingIdAlreadyUsed) {
                return@forEach
            }

            val newAppearance = ActiveAppearance(
                id = nextAppearanceId++,
                trackingId = detection.trackingId,
                detections = mutableListOf(detection)
            )

            activeAppearances.add(newAppearance)
            matchedAppearanceIds.add(newAppearance.id)
        }
    }

    fun finish(): List<Appearance> {

        activeAppearances
            .toList()
            .forEach { appearance ->
                finishAppearance(appearance)
            }

        return completedAppearances
            .sortedBy { it.startTimeMs }
    }

    private fun finishAppearance(
        active: ActiveAppearance
    ) {

        activeAppearances.remove(active)

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

    private fun calculateIoU(
        first: Rect,
        second: Rect
    ): Float {

        val intersectionLeft =
            max(first.left, second.left)

        val intersectionTop =
            max(first.top, second.top)

        val intersectionRight =
            minOf(first.right, second.right)

        val intersectionBottom =
            minOf(first.bottom, second.bottom)

        val intersectionWidth =
            (intersectionRight - intersectionLeft)
                .coerceAtLeast(0)

        val intersectionHeight =
            (intersectionBottom - intersectionTop)
                .coerceAtLeast(0)

        val intersectionArea =
            intersectionWidth * intersectionHeight

        if (intersectionArea <= 0) {
            return 0f
        }

        val firstArea =
            first.width() * first.height()

        val secondArea =
            second.width() * second.height()

        val unionArea =
            firstArea + secondArea - intersectionArea

        if (unionArea <= 0) {
            return 0f
        }

        return intersectionArea.toFloat() /
                unionArea.toFloat()
    }
}