package com.example.portraitcounter.data.ml

import android.graphics.Bitmap
import com.example.portraitcounter.domain.model.DetectedFace
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.05f)
            .enableTracking()
            .build()
    )

    suspend fun detectFaces(
        bitmap: Bitmap,
        timestampMs: Long
    ): List<DetectedFace> {

        val image = InputImage.fromBitmap(bitmap, 0)

        val faces = detector.process(image).await()

        return faces.map { face ->
            DetectedFace(
                boundingBox = face.boundingBox,
                trackingId = face.trackingId,
                timestampMs = timestampMs,
                headEulerX = face.headEulerAngleX,
                headEulerY = face.headEulerAngleY,
                headEulerZ = face.headEulerAngleZ,
                leftEyeOpenProbability = face.leftEyeOpenProbability,
                rightEyeOpenProbability = face.rightEyeOpenProbability,
                smilingProbability = face.smilingProbability
            )
        }
    }

    fun close() {
        detector.close()
    }
}