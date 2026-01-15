package com.example.mcamp25.readly.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity)

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("UPDATE books SET rating = :rating WHERE id = :bookId")
    suspend fun updateRating(bookId: String, rating: Int): Int

    @Query("SELECT rating FROM books WHERE id = :bookId")
    fun getRating(bookId: String): Flow<Int?>

    @Delete
    suspend fun deleteBook(book: BookEntity)
}