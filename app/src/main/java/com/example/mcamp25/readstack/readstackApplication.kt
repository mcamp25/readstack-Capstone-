package com.example.mcamp25.readstack

import android.app.Application
import androidx.room.Room
import com.example.mcamp25.readstack.data.local.AppDatabase

class ReadstackApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "readstack_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}
