package com.example.portraitcounter.domain.model

data class PersonCluster(
    val id: Int,
    val appearances: List<AppearanceEmbedding>
)