package com.example.mcamp25.readly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val thumbnail: String?,
    val rating: Int = 0,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val category: String? = null,
    val isRead: Boolean = false,
    val isCurrentlyReading: Boolean = false
)
