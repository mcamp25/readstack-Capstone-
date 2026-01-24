package com.example.mcamp25.readstack.ui.screens.search

import com.example.mcamp25.readstack.data.network.BookItem

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val books: List<BookItem>) : SearchUiState
    data object Error : SearchUiState
}
