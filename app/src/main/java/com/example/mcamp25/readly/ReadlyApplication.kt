package com.example.mcamp25.readly

import android.app.Application
import androidx.room.Room
import com.example.mcamp25.readly.data.local.AppDatabase

class ReadlyApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "readly_database"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }
}
