package com.example.mcamp25.readly.ui.screens.search

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcamp25.readly.data.RetrofitClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class SearchViewModel : ViewModel() {
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length >= 3) {
                    fetchSuggestions(query)
                } else {
                    _suggestions.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private suspend fun fetchSuggestions(query: String) {
        try {
            val result = RetrofitClient.apiService.searchBooks(query)
            val titles = result.items?.map { it.volumeInfo.title }?.distinct()?.take(5) ?: emptyList()
            _suggestions.value = titles
        } catch (_: Exception) {
            _suggestions.value = emptyList()
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        _suggestions.value = emptyList() // Clear suggestions on formal search

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

    fun searchByImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText -> 
                        val detectedText = visionText.text
                        if (detectedText.isNotBlank()) {
                            searchBooks(detectedText)
                        } else {
                            _searchUiState.value = SearchUiState.Error
                        }
                    }
                    .addOnFailureListener { 
                        _searchUiState.value = SearchUiState.Error
                    }
            } catch (_: Exception) {
                _searchUiState.value = SearchUiState.Error
            }
        }
    }

    fun resetSearch() {
        _searchUiState.value = SearchUiState.Idle
        _suggestions.value = emptyList()
    }

    fun handleImportedFile(uri: Uri) {
        // Implementation for handling the imported file
        println("Imported file: $uri")
    }
}
