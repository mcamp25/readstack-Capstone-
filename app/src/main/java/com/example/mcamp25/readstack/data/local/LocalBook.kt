package com.example.mcamp25.readstack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val thumbnail: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val rating: Int = 0,
    val isRead: Boolean = false,
    val isCurrentlyReading: Boolean = false
)
