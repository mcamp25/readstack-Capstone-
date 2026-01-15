package com.example.mcamp25.readly.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class BookResponse( val items: List<BookItem>? = emptyList()
)
@Serializable
data class BookItem(
    val id: String, val volumeInfo: VolumeInfo
)
@Serializable
data class VolumeInfo(
    val title: String, val authors: List<String>? = emptyList(),
    val description: String? = "No description available",
    val imageLinks: ImageLinks? = null
)
@Serializable
data class ImageLinks(
    @SerialName("thumbnail") val thumbnailUrl: String
)
