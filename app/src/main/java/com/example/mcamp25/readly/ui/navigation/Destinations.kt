package com.example.mcamp25.readly.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable
    data object Search : Destination
    
    @Serializable
    data object ReadingList : Destination
    
    @Serializable
    data class BookDetail(
        val bookId: String,
        val initialPages: Int? = null,
        val initialDate: String? = null
    ) : Destination
}
