package com.example.portraitcounter

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.example.portraitcounter.presentation.home.HomeScreen
import com.example.portraitcounter.presentation.viewmodel.VideoAnalysisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VideoAnalysisViewModel by viewModels()

    private val videoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.analyzeVideo(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeScreen(
                processingState = viewModel.processingState.collectAsState().value,
                onVideoSelected = {
                    videoPicker.launch("video/*")
                }
            )
        }
    }
}