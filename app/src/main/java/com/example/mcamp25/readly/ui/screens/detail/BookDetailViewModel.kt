package com.example.mcamp25.readly.ui.screens.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mcamp25.readly.ReadlyApplication
import com.example.mcamp25.readly.data.local.BookDao
import com.example.mcamp25.readly.data.local.BookEntity
import com.example.mcamp25.readly.data.network.BookItem
import com.example.mcamp25.readly.data.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Success(val book: BookItem) : BookDetailUiState
    data object Error : BookDetailUiState
}

class BookDetailViewModel(private val bookDao: BookDao) : ViewModel() {
    var uiState: BookDetailUiState by mutableStateOf(BookDetailUiState.Loading)
        private set

    var currentRating by mutableIntStateOf(0)
        private set

    var isRead by mutableStateOf(false)
        private set

    var isCurrentlyReading by mutableStateOf(false)
        private set

    private var ratingJob: Job? = null
    private var readStatusJob: Job? = null
    private var readingStatusJob: Job? = null

    fun getBook(id: String) {
        viewModelScope.launch {
            uiState = BookDetailUiState.Loading
            uiState = try {
                val book = RetrofitClient.apiService.getBook(id)
                observeRating(id)
                observeReadStatus(id)
                observeReadingStatus(id)
                BookDetailUiState.Success(book)
            } catch (_: IOException) {
                BookDetailUiState.Error
            } catch (_: Exception) {
                BookDetailUiState.Error
            }
        }
    }

    private fun observeRating(bookId: String) {
        ratingJob?.cancel()
        ratingJob = viewModelScope.launch {
            bookDao.getRating(bookId).collectLatest { rating ->
                if (rating != null) {
                    currentRating = rating
                }
            }
        }
    }

    private fun observeReadStatus(bookId: String) {
        readStatusJob?.cancel()
        readStatusJob = viewModelScope.launch {
            bookDao.getReadStatus(bookId).collectLatest { status ->
                isRead = status ?: false
            }
        }
    }

    private fun observeReadingStatus(bookId: String) {
        readingStatusJob?.cancel()
        readingStatusJob = viewModelScope.launch {
            bookDao.getReadingStatus(bookId).collectLatest { status ->
                isCurrentlyReading = status ?: false
            }
        }
    }

    fun updateRating(bookId: String, rating: Int) {
        currentRating = rating
        viewModelScope.launch {
            bookDao.updateRating(bookId, rating)
        }
    }

    fun toggleReadStatus(bookId: String) {
        val newStatus = !isRead
        isRead = newStatus
        viewModelScope.launch {
            bookDao.updateReadStatus(bookId, newStatus)
        }
    }

    fun toggleCurrentlyReading(bookId: String) {
        val newStatus = !isCurrentlyReading
        isCurrentlyReading = newStatus
        viewModelScope.launch {
            bookDao.updateReadingStatus(bookId, newStatus)
        }
    }

    fun addToReadingList(book: BookItem, initialPages: Int? = null, initialDate: String? = null) {
        viewModelScope.launch {
            // Aggressively find the best metadata available
            val finalPages = when {
                (book.volumeInfo.pageCount ?: 0) > 0 -> book.volumeInfo.pageCount
                (book.volumeInfo.printedPageCount ?: 0) > 0 -> book.volumeInfo.printedPageCount
                (initialPages ?: 0) > 0 -> initialPages
                else -> null
            }
            
            val finalDate = book.volumeInfo.publishedDate ?: initialDate
            val finalCategory = book.volumeInfo.categories?.firstOrNull()
            
            val entity = BookEntity(
                id = book.id,
                title = book.volumeInfo.title,
                author = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                description = book.volumeInfo.description ?: "",
                thumbnail = book.volumeInfo.imageLinks?.thumbnailUrl?.replace("http:", "https:") ?: "",
                rating = currentRating,
                publishedDate = finalDate,
                pageCount = finalPages,
                category = finalCategory,
                isRead = isRead,
                isCurrentlyReading = isCurrentlyReading
            )
            bookDao.insert(entity)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ReadlyApplication)
                BookDetailViewModel(application.database.bookDao())
            }
        }
    }
}
