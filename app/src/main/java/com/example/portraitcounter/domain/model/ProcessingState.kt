package com.example.portraitcounter.domain.model

sealed interface ProcessingState {

    data object Idle : ProcessingState

    data class Processing(
        val framesProcessed: Int,
        val totalFrames: Int,
        val facesDetected: Int
    ) : ProcessingState

    data class Success(
        val framesProcessed: Int,
        val facesDetected: Int,
        val appearances: List<Appearance>,
        val appearanceEmbeddings: List<AppearanceEmbedding>,
        val personClusters: List<PersonCluster>
    ) : ProcessingState

    data class Error(
        val message: String
    ) : ProcessingState
}