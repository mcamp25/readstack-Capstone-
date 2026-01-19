package com.example.mcamp25.readly.ui.screens.search

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcamp25.readly.data.RetrofitClient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
    }

    fun handleImportedFile(uri: Uri) {
        // Implementation for handling the imported file
        println("Imported file: $uri")
    }
}
