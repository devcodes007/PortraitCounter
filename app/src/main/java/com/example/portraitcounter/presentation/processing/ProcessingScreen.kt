package com.example.portraitcounter.presentation.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.portraitcounter.domain.model.ProcessingState

@Composable
fun ProcessingScreen(
    state: ProcessingState.Processing
) {
    val progress = if (state.totalFrames > 0) {
        state.framesProcessed.toFloat() / state.totalFrames
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Analyzing video",
            style = MaterialTheme.typography.headlineMedium
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            text = "${(progress * 100).toInt()}%"
        )

        Text(
            text = "Frames: ${state.framesProcessed} / ${state.totalFrames}",
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "Faces detected: ${state.facesDetected}",
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}