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
                val bookDao = database.bookDao()
                val localBooks = bookDao.getAllBooks().first()
                
                Log.d("LibrarySyncWorker", "Found ${localBooks.size} local books to sync")
                
                // Simulate work
                kotlinx.coroutines.delay(3000)
                
                // Show success notification
                makeStatusNotification("Library synced successfully!", applicationContext)
                
                Result.success()
            } catch (e: Exception) {
                Log.e("LibrarySyncWorker", "Error during library sync", e)
                makeStatusNotification("Sync failed. Check your connection.", applicationContext)
                Result.failure()
            }
        }
    }
}
