package com.example.portraitcounter.presentation.viewmodel

import android.app.Application
import com.example.portraitcounter.data.ml.FaceEmbedder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.portraitcounter.data.ml.FaceDetector
import com.example.portraitcounter.data.repository.VideoAnalysisRepository
import com.example.portraitcounter.data.video.VideoFrameExtractor
import com.example.portraitcounter.domain.model.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoAnalysisViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = VideoAnalysisRepository(
        frameExtractor = VideoFrameExtractor(application),
        faceDetector = FaceDetector(),
        faceEmbedder = FaceEmbedder(application)
    )

    private val _processingState =
        MutableStateFlow<ProcessingState>(ProcessingState.Idle)

    val processingState: StateFlow<ProcessingState> =
        _processingState.asStateFlow()

    fun analyzeVideo(videoUri: Uri) {
        viewModelScope.launch {

            _processingState.value = ProcessingState.Processing(
                framesProcessed = 0,
                totalFrames = 0,
                facesDetected = 0
            )

            val result = repository.analyzeVideo(
                videoUri = videoUri,
                onProgress = { progress ->
                    _processingState.value = progress
                }
            )

            _processingState.value = result
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}