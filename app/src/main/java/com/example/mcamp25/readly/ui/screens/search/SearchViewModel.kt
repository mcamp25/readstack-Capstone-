package com.example.mcamp25.readly.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcamp25.readly.data.RetrofitClient
import com.example.mcamp25.readly.ui.screens.search.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class SearchViewModel : ViewModel() {
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    fun searchBooks(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val result = RetrofitClient.apiService.searchBooks(query)
                _searchUiState.value = SearchUiState.Success(result.items ?: emptyList())
            } catch (_: IOException) {
                _searchUiState.value = SearchUiState.Error
            } catch (_: Exception) {
                _searchUiState.value = SearchUiState.Error
            }
        }
    }
}