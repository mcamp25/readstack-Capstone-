package com.example.mcamp25.readly.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcamp25.readly.data.BookItem
import com.example.mcamp25.readly.data.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface BookDetailUiState {
    object Loading : BookDetailUiState
    data class Success(val book: BookItem) : BookDetailUiState
    object Error : BookDetailUiState
}

class BookDetailViewModel : ViewModel() {
    var uiState: BookDetailUiState by mutableStateOf(BookDetailUiState.Loading)
        private set

    fun getBook(id: String) {
        viewModelScope.launch {
            uiState = BookDetailUiState.Loading
            uiState = try {
                val book = RetrofitClient.apiService.getBook(id)
                BookDetailUiState.Success(book)
            } catch (e: IOException) {
                BookDetailUiState.Error
            } catch (e: Exception) {
                BookDetailUiState.Error
            }
        }
    }
}
