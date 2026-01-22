package com.example.mcamp25.readly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BookEntity::class], version = 4)
abstract class AppDatabase:
        RoomDatabase() {
    abstract fun bookDao(): BookDao
}