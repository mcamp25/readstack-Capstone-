package com.example.mcamp25.readstack.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookResponse(
    val items: List<BookItem>? = emptyList()
)

@Serializable
data class BookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

@Serializable
data class VolumeInfo(
    val title: String,
    val authors: List<String>? = emptyList(),
    val description: String? = "No description available",
    val imageLinks: ImageLinks? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val printedPageCount: Int? = null,
    val categories: List<String>? = emptyList(),
    val language: String? = null
)

@Serializable
data class ImageLinks(
    @SerialName("smallThumbnail") val smallThumbnail: String? = null,
    @SerialName("thumbnail") val thumbnailUrl: String? = null,
    @SerialName("small") val small: String? = null,
    @SerialName("medium") val medium: String? = null,
    @SerialName("large") val large: String? = null,
    @SerialName("extraLarge") val extraLarge: String? = null
)

/**
 * Returns the best available image URL from the imageLinks.
 */
fun ImageLinks?.getBestUrl(): String? {
    if (this == null) return null
    return extraLarge ?: large ?: medium ?: small ?: thumbnailUrl ?: smallThumbnail
}
