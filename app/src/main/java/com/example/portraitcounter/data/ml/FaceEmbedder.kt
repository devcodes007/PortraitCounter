package com.example.portraitcounter.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceEmbedder(
    context: Context
) {

    companion object {
        private const val MODEL_FILE = "mobilefacenet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
    }

    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context)

        val options = Interpreter.Options().apply {
            numThreads = 4
        }

        interpreter = Interpreter(model, options)
    }


    fun embed(
        bitmap: Bitmap,
        boundingBox: Rect
    ): FloatArray {

        val faceBitmap = cropFace(
            bitmap = bitmap,
            boundingBox = boundingBox
        )

        val resizedBitmap = Bitmap.createScaledBitmap(
            faceBitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        val inputBuffer = ByteBuffer
            .allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)
            .order(ByteOrder.nativeOrder())

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {

                val pixel = resizedBitmap.getPixel(x, y)

                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                inputBuffer.putFloat(
                    red / 127.5f - 1.0f
                )

                inputBuffer.putFloat(
                    green / 127.5f - 1.0f
                )

                inputBuffer.putFloat(
                    blue / 127.5f - 1.0f
                )
            }
        }

        inputBuffer.rewind()

        val output = Array(1) {
            FloatArray(EMBEDDING_SIZE)
        }

        interpreter.run(
            inputBuffer,
            output
        )

        return normalize(output[0])
    }

    private fun cropFace(
        bitmap: Bitmap,
        boundingBox: Rect
    ): Bitmap {

        val paddingX = (boundingBox.width() * 0.25f).toInt()
        val paddingY = (boundingBox.height() * 0.35f).toInt()

        val left = maxOf(
            0,
            boundingBox.left - paddingX
        )

        val top = maxOf(
            0,
            boundingBox.top - paddingY
        )

        val right = minOf(
            bitmap.width,
            boundingBox.right + paddingX
        )

        val bottom = minOf(
            bitmap.height,
            boundingBox.bottom + paddingY
        )

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    }

    private fun normalize(
        embedding: FloatArray
    ): FloatArray {

        var sum = 0.0

        for (value in embedding) {
            sum += value * value
        }

        val magnitude = sqrt(sum).toFloat()

        if (magnitude == 0f) {
            return embedding
        }

        return FloatArray(embedding.size) { index ->
            embedding[index] / magnitude
        }
    }

    private fun loadModelFile(
        context: Context
    ): ByteBuffer {

        val fileDescriptor =
            context.assets.openFd(MODEL_FILE)

        FileInputStream(
            fileDescriptor.fileDescriptor
        ).use { inputStream ->

            val fileChannel = inputStream.channel

            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
        }
    }

    fun getModelInfo(): String {
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputType = interpreter.getInputTensor(0).dataType()

        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputType = interpreter.getOutputTensor(0).dataType()

        return """
        Input shape: ${inputShape.contentToString()}
        Input type: $inputType
        Output shape: ${outputShape.contentToString()}
        Output type: $outputType
    """.trimIndent()
    }

    fun close() {
        interpreter.close()
    }
}