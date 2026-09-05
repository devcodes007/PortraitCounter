package com.example.portraitcounter.data.repository

import android.net.Uri
import android.util.Log
import com.example.portraitcounter.data.ml.AppearanceTracker
import com.example.portraitcounter.data.ml.FaceDetector
import com.example.portraitcounter.data.ml.FaceEmbedder
import com.example.portraitcounter.data.video.VideoFrameExtractor
import com.example.portraitcounter.domain.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.portraitcounter.domain.model.AppearanceEmbedding
import com.example.portraitcounter.data.ml.AppearanceClusterer
import com.example.portraitcounter.domain.model.PersonCluster
import com.example.portraitcounter.data.ml.CosineSimilarity

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

                // ---------------------------------------------------------
                // STEP 1: Extract video frames
                // ---------------------------------------------------------

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

                // ---------------------------------------------------------
                // STEP 2: Detect faces and build appearances
                // ---------------------------------------------------------

                frames.forEachIndexed { index, frame ->

                    val timestampMs = frame.first
                    val bitmap = frame.second

                    val faces = faceDetector.detectFaces(
                        bitmap = bitmap,
                        timestampMs = timestampMs
                    )

                    facesDetected += faces.size

                    appearanceTracker.processFrame(faces)

                    onProgress(
                        ProcessingState.Processing(
                            framesProcessed = index + 1,
                            totalFrames = frames.size,
                            facesDetected = facesDetected
                        )
                    )

                    bitmap.recycle()
                }

                // Finish appearances that were still active
                // when the video ended.
                val appearances = appearanceTracker.finish()
                val appearanceEmbeddings = mutableListOf<AppearanceEmbedding>()

                Log.d(
                    "AppearanceEmbedding",
                    "Total appearances: ${appearances.size}"
                )

                // ---------------------------------------------------------
                // STEP 3: Generate one embedding for each appearance
                // ---------------------------------------------------------

                appearances.forEachIndexed { index, appearance ->

                    val representativeTimestamp =
                        appearance.representativeTimestampMs

                    val representativeDetection =
                        appearance.detections.maxByOrNull { detection ->
                            if (detection.timestampMs ==
                                representativeTimestamp
                            ) {
                                1
                            } else {
                                0
                            }
                        }

                    if (representativeDetection == null) {
                        return@forEachIndexed
                    }

                    val bitmap = frameExtractor.extractFrameAt(
                        videoUri = videoUri,
                        timestampMs = representativeTimestamp
                    )

                    if (bitmap == null) {
                        Log.w(
                            "AppearanceEmbedding",
                            "Could not extract representative frame " +
                                    "for appearance $index"
                        )
                        return@forEachIndexed
                    }

                    try {

                        val embedding = faceEmbedder.embed(
                            bitmap = bitmap,
                            boundingBox =
                                representativeDetection.boundingBox
                        )
                        appearanceEmbeddings.add(
                            AppearanceEmbedding(
                                appearance = appearance,
                                embedding = embedding
                            )
                        )

                        Log.d(
                            "AppearanceEmbedding",
                            "Appearance ${index + 1}: " +
                                    "timestamp=${representativeTimestamp}ms, " +
                                    "embeddingDimensions=${embedding.size}"
                        )

                    } finally {
                        bitmap.recycle()
                    }
                }
                Log.d(
                    "SimilarityMatrix",
                    "===== APPEARANCE SIMILARITY MATRIX ====="
                )

                for (i in appearanceEmbeddings.indices) {

                    val row = StringBuilder()

                    for (j in appearanceEmbeddings.indices) {

                        val similarity = CosineSimilarity.calculate(
                            first = appearanceEmbeddings[i].embedding,
                            second = appearanceEmbeddings[j].embedding
                        )

                        row.append(
                            String.format("%.3f", similarity)
                        )

                        if (j < appearanceEmbeddings.lastIndex) {
                            row.append(" | ")
                        }
                    }

                    Log.d(
                        "SimilarityMatrix",
                        "A${i + 1}: $row"
                    )
                }

                Log.d(
                    "SimilarityMatrix",
                    "========================================"
                )


                val clusterer = AppearanceClusterer()

                val personClusters = clusterer.cluster(
                    appearanceEmbeddings
                )

                Log.d(
                    "IdentityClustering",
                    "Unique people detected: ${personClusters.size}"
                )

                personClusters.forEach { cluster ->
                    Log.d(
                        "IdentityClustering",
                        "Person ${cluster.id}: " +
                                "${cluster.appearances.size} appearances"
                    )
                }

                // ---------------------------------------------------------
                // STEP 4: Return analysis result
                // ---------------------------------------------------------

                ProcessingState.Success(
                    framesProcessed = frames.size,
                    facesDetected = facesDetected,
                    appearances = appearances,
                    appearanceEmbeddings = appearanceEmbeddings,
                    personClusters = personClusters
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

