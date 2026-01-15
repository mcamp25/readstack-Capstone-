package com.example.mcamp25.readly.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mcamp25.readly.ReadlyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class LibrarySyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Show starting notification
                makeStatusNotification("Syncing library...", applicationContext)
                
                val database = (applicationContext as ReadlyApplication).database
                val books = database.bookDao().getAllBooks().first()
                Log.d("LibrarySyncWorker", "Syncing library with ${books.size} books")
                
                // Simulate some work
                kotlinx.coroutines.delay(2000)
                
                // Show success notification
                makeStatusNotification("Library synced successfully!", applicationContext)
                
                Result.success()
            } catch (e: Exception) {
                Log.e("LibrarySyncWorker", "Error syncing library", e)
                makeStatusNotification("Failed to sync library.", applicationContext)
                Result.failure()
            }
        }
    }
}
