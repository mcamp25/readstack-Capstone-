package com.example.mcamp25.readly.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable object Search : Destination
    @Serializable object ReadingList : Destination
    @Serializable data class BookDetail( val bookId : String ) : Destination
}