package com.example.portraitcounter.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.portraitcounter.domain.model.ProcessingState
import com.example.portraitcounter.presentation.processing.ProcessingScreen

@Composable
fun HomeScreen(
    processingState: ProcessingState,
    onVideoSelected: () -> Unit
) {
    when (processingState) {

        is ProcessingState.Processing -> {
            ProcessingScreen(processingState)
        }

        is ProcessingState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Analysis complete!",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Frames processed: ${processingState.framesProcessed}",
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Faces detected: ${processingState.facesDetected}",
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Appearances detected: ${processingState.appearances.size}",
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Unique people: ${processingState.personClusters.size}",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        is ProcessingState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = processingState.message,
                    modifier = Modifier.padding(
                        top = 16.dp,
                        bottom = 24.dp
                    )
                )

                Button(
                    onClick = onVideoSelected
                ) {
                    Text("Try Again")
                }
            }
        }

        ProcessingState.Idle -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unique Collage",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Turn your video into a collage of unique people.",
                    modifier = Modifier.padding(
                        top = 12.dp,
                        bottom = 24.dp
                    )
                )

                Button(
                    onClick = onVideoSelected
                ) {
                    Text("Select Video")
                }
            }
        }
    }
}