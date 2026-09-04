package com.example.portraitcounter.data.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

class VideoFrameExtractor(
    private val context: Context
) {

    fun extractFrames(
        videoUri: Uri,
        framesPerSecond: Int = 5
    ): List<Pair<Long, Bitmap>> {

        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: return emptyList()

            val intervalMs = 1000L / framesPerSecond
            val frames = mutableListOf<Pair<Long, Bitmap>>()

            var timestampMs = 0L

            while (timestampMs < durationMs) {

                val bitmap = retriever.getFrameAtTime(
                    timestampMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

                if (bitmap != null) {
                    frames.add(timestampMs to bitmap)
                }

                timestampMs += intervalMs
            }

            return frames

        } finally {
            retriever.release()
        }
    }
}