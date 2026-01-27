package com.example.mcamp25.readstack.ui.screens.search

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcamp25.readstack.data.RetrofitClient
import com.example.mcamp25.readstack.data.network.BookItem
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
    
    // We track the last query that was actually searched to avoid showing suggestions for it
    private var lastSearchedQuery: String? = null

    init {
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                // If it matches our last search or is too short, we don't need suggestions
                if (query.length < 3 || query == lastSearchedQuery) {
                    _suggestions.value = emptyList()
                    return@onEach
                }

                // We only fetch suggestions if we aren't busy loading a full search
                if (_searchUiState.value !is SearchUiState.Loading) {
                    fetchSuggestions(query)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        // If the user clears the search bar, we clear everything immediately
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            lastSearchedQuery = null
        }
    }

    private suspend fun fetchSuggestions(query: String) {
        try {
            val result = RetrofitClient.apiService.searchBooks(query)
            val titles = result.items?.map { it.volumeInfo.title.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinctBy { it.lowercase() }
                ?.take(5) ?: emptyList()
            
            // We check again if the query is still what the user is typing
            if (_searchQuery.value == query && query != lastSearchedQuery) {
                _suggestions.value = titles
            }
        } catch (_: Exception) {
            _suggestions.value = emptyList()
        }
    }

    fun search(query: String, genre: String?) {
        val finalQuery = buildString {
            if (query.isNotBlank()) append(query)
            if (genre != null) {
                if (isNotEmpty()) append(" ")
                val subject = if (genre == "Sci-Fi") "Science Fiction" else genre
                append("subject:\"$subject\"")
            }
        }
        // Once a search is triggered, we hide suggestions for this query
        lastSearchedQuery = query
        _suggestions.value = emptyList()
        searchBooks(finalQuery)
    }

    private fun searchBooks(query: String) {
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
        lastSearchedQuery = null
    }

    fun handleImportedFile(uri: Uri) {
        println("Imported file: $uri")
    }
    
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }
}
