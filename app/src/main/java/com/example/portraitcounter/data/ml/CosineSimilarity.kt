package com.example.portraitcounter.data.ml

import kotlin.math.sqrt

object CosineSimilarity {

    fun calculate(
        first: FloatArray,
        second: FloatArray
    ): Float {

        require(first.size == second.size) {
            "Embeddings must have the same dimensions."
        }

        var dotProduct = 0.0
        var firstMagnitude = 0.0
        var secondMagnitude = 0.0

        for (i in first.indices) {
            dotProduct += first[i] * second[i]
            firstMagnitude += first[i] * first[i]
            secondMagnitude += second[i] * second[i]
        }

        if (firstMagnitude == 0.0 || secondMagnitude == 0.0) {
            return 0f
        }

        return (
                dotProduct /
                        (sqrt(firstMagnitude) * sqrt(secondMagnitude))
                ).toFloat()
    }
}