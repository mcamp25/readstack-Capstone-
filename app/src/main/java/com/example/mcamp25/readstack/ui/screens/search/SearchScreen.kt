package com.example.mcamp25.readstack.ui.screens.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.mcamp25.readstack.data.network.BookItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBookClick: (BookItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val uiState by viewModel.searchUiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.handleImportedFile(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SearchHeader(
            query = query,
            onQueryChange = { 
                query = it
                viewModel.onQueryChanged(it)
            },
            suggestions = suggestions,
            selectedGenre = selectedGenre,
            onGenreSelected = {
                selectedGenre = it
                viewModel.search(query, it)
            },
            onSearch = { viewModel.search(query, selectedGenre) },
            onImageSearchTrigger = { viewModel.searchByImage(context, selectedImageUri!!) },
            onImagePickerLaunch = { filePickerLauncher.launch("image/*") },
            selectedImageUri = selectedImageUri,
            focusRequester = focusRequester,
            keyboardController = keyboardController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp)
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> WelcomeNudge()
                is SearchUiState.Loading -> LoadingSkeletons()
                is SearchUiState.Success -> FoundResults(
                    books = state.books,
                    currentQuery = query,
                    onBookClick = {
                        onBookClick(it)
                        query = ""
                        selectedGenre = null
                        selectedImageUri = null
                        viewModel.resetSearch()
                    }
                )
                is SearchUiState.Error -> ErrorMessage()
            }
        }
    }
}
