package com.example.mcamp25.readly.ui.screens.search

import com.example.mcamp25.readly.data.network.BookItem

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val books: List<BookItem>) : SearchUiState
    object Error : SearchUiState
}