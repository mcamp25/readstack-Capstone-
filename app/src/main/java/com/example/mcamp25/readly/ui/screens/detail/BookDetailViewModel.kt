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

    private var ratingJob: Job? = null

    fun getBook(id: String) {
        viewModelScope.launch {
            uiState = BookDetailUiState.Loading
            uiState = try {
                val book = RetrofitClient.apiService.getBook(id)
                observeRating(id)
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
                // If it's in the DB, use that. If not, keep our local state if user changed it.
                if (rating != null) {
                    currentRating = rating
                }
            }
        }
    }

    fun updateRating(bookId: String, rating: Int) {
        currentRating = rating
        viewModelScope.launch {
            bookDao.updateRating(bookId, rating)
            // If the book isn't in the DB yet, updateRating (UPDATE) won't work.
            // But we keep currentRating in memory so if they click "Add", it persists.
        }
    }

    fun addToReadingList(book: BookItem) {
        viewModelScope.launch {
            val entity = BookEntity(
                id = book.id,
                title = book.volumeInfo.title,
                author = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                description = book.volumeInfo.description ?: "",
                thumbnail = book.volumeInfo.imageLinks?.thumbnailUrl?.replace("http:", "https:") ?: "",
                rating = currentRating
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
