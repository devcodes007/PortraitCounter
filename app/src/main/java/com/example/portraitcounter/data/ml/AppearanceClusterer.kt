package com.example.portraitcounter.data.ml

import com.example.portraitcounter.domain.model.AppearanceEmbedding
import com.example.portraitcounter.domain.model.PersonCluster

class AppearanceClusterer(
    private val similarityThreshold: Float = 0.65f
) {

    fun cluster(
        appearances: List<AppearanceEmbedding>
    ): List<PersonCluster> {

        if (appearances.isEmpty()) {
            return emptyList()
        }

        // Initially every appearance is its own cluster.
        val clusters = appearances.mapIndexed { index, appearance ->
            MutableCluster(
                id = index + 1,
                appearances = mutableListOf(appearance)
            )
        }.toMutableList()

        while (true) {

            var bestFirstIndex = -1
            var bestSecondIndex = -1
            var bestSimilarity = Float.NEGATIVE_INFINITY

            // Find the two most similar clusters.
            for (i in 0 until clusters.size) {
                for (j in i + 1 until clusters.size) {

                    val similarity = averageSimilarity(
                        clusters[i],
                        clusters[j]
                    )

                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestFirstIndex = i
                        bestSecondIndex = j
                    }
                }
            }

            // Stop when no pair is similar enough.
            if (
                bestFirstIndex == -1 ||
                bestSecondIndex == -1 ||
                bestSimilarity < similarityThreshold
            ) {
                break
            }

            val firstCluster = clusters[bestFirstIndex]
            val secondCluster = clusters[bestSecondIndex]

            firstCluster.appearances.addAll(
                secondCluster.appearances
            )

            // Remove the second cluster.
            clusters.removeAt(bestSecondIndex)

            // Keep the smaller index stable.
            clusters[bestFirstIndex] = firstCluster
        }

        return clusters.mapIndexed { index, cluster ->
            PersonCluster(
                id = index + 1,
                appearances = cluster.appearances.toList()
            )
        }
    }

    private fun averageSimilarity(
        first: MutableCluster,
        second: MutableCluster
    ): Float {

        var totalSimilarity = 0.0
        var comparisons = 0

        for (firstAppearance in first.appearances) {
            for (secondAppearance in second.appearances) {

                totalSimilarity += CosineSimilarity.calculate(
                    first = firstAppearance.embedding,
                    second = secondAppearance.embedding
                )

                comparisons++
            }
        }

        if (comparisons == 0) {
            return 0f
        }

        return (totalSimilarity / comparisons).toFloat()
    }

    private data class MutableCluster(
        val id: Int,
        val appearances: MutableList<AppearanceEmbedding>
    )
}