package com.example.portraitcounter.data.repository

import android.net.Uri
import android.util.Log
import com.example.portraitcounter.data.ml.AppearanceTracker
import com.example.portraitcounter.data.ml.CosineSimilarity
import com.example.portraitcounter.data.ml.FaceDetector
import com.example.portraitcounter.data.ml.FaceEmbedder
import com.example.portraitcounter.data.video.VideoFrameExtractor
import com.example.portraitcounter.domain.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoAnalysisRepository(
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedder: FaceEmbedder
) {

    suspend fun analyzeVideo(
        videoUri: Uri,
        onProgress: (ProcessingState.Processing) -> Unit
    ): ProcessingState {

        return withContext(Dispatchers.Default) {
            try {
                val frames = frameExtractor.extractFrames(
                    videoUri = videoUri,
                    framesPerSecond = 5
                )

                if (frames.isEmpty()) {
                    return@withContext ProcessingState.Error(
                        "Could not extract frames from this video."
                    )
                }

                val appearanceTracker = AppearanceTracker()

                var facesDetected = 0
                var embeddingTested = false

                var firstEmbedding: FloatArray? = null

                frames.forEachIndexed { index, frame ->

                    val timestampMs = frame.first
                    val bitmap = frame.second

                    val faces = faceDetector.detectFaces(
                        bitmap = bitmap,
                        timestampMs = timestampMs
                    )

                    facesDetected += faces.size

                    appearanceTracker.processFrame(faces)

                    if (faces.isNotEmpty()) {

                        val embedding = faceEmbedder.embed(
                            bitmap = bitmap,
                            boundingBox = faces.first().boundingBox
                        )

                        if (!embeddingTested) {

                            firstEmbedding = embedding
                            embeddingTested = true

                            Log.d(
                                "FaceSimilarity",
                                "First embedding generated: ${embedding.size} dimensions"
                            )

                        } else if (firstEmbedding != null) {

                            val similarity = CosineSimilarity.calculate(
                                first = firstEmbedding!!,
                                second = embedding
                            )

                            Log.d(
                                "FaceSimilarity",
                                "Frame $index | " +
                                        "timestamp=${timestampMs}ms | " +
                                        "similarity=$similarity"
                            )

                            // We only need a few comparisons for now.
                            if (index >= 10) {
                                firstEmbedding = null
                            }
                        }
                    }

                    onProgress(
                        ProcessingState.Processing(
                            framesProcessed = index + 1,
                            totalFrames = frames.size,
                            facesDetected = facesDetected
                        )
                    )

                    bitmap.recycle()
                }

                val appearances = appearanceTracker.finish()

                ProcessingState.Success(
                    framesProcessed = frames.size,
                    facesDetected = facesDetected,
                    appearances = appearances
                )

            } catch (exception: Exception) {

                Log.e(
                    "VideoAnalysis",
                    "Video analysis failed",
                    exception
                )

                ProcessingState.Error(
                    exception.message
                        ?: "An unexpected error occurred."
                )
            }
        }
    }

    fun close() {
        faceDetector.close()
        faceEmbedder.close()
    }
}

