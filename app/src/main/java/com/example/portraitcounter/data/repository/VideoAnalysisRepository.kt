package com.example.portraitcounter.data.repository

import android.net.Uri
import com.example.portraitcounter.data.ml.AppearanceTracker
import com.example.portraitcounter.data.ml.FaceDetector
import com.example.portraitcounter.data.video.VideoFrameExtractor
import com.example.portraitcounter.domain.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoAnalysisRepository(
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetector
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

                frames.forEachIndexed { index, (timestampMs, bitmap) ->

                    val faces = faceDetector.detectFaces(
                        bitmap = bitmap,
                        timestampMs = timestampMs
                    )

                    facesDetected += faces.size

                    // Give this frame's detections to the appearance tracker.
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

                // Finish any appearances that were still active
                // when the video ended.
                val appearances = appearanceTracker.finish()

                ProcessingState.Success(
                    framesProcessed = frames.size,
                    facesDetected = facesDetected,
                    appearances = appearances
                )

            } catch (exception: Exception) {

                ProcessingState.Error(
                    exception.message
                        ?: "An unexpected error occurred."
                )
            }
        }
    }

    fun close() {
        faceDetector.close()
    }
}

