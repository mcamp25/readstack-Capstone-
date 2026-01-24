package com.example.mcamp25.readstack.ui.screens.search

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readstack.BookSkeletonItem
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

    val performSearch = { searchQuery: String ->
        val finalQuery = buildString {
            if (searchQuery.isNotBlank()) append(searchQuery)
            if (selectedGenre != null) {
                if (isNotEmpty()) append(" ")
                val subject = if (selectedGenre == "Sci-Fi") "Science Fiction" else selectedGenre
                append("subject:\"$subject\"")
            }
        }
        if (finalQuery.isNotBlank()) {
            viewModel.searchBooks(finalQuery)
        } else {
            viewModel.resetSearch()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract  = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            selectedImageUri = selectedUri
            viewModel.handleImportedFile(selectedUri)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Top Search Bar with Suggestions
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .zIndex(3f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { 
                            query = it 
                            viewModel.onQueryChanged(it)
                        },
                        placeholder = { Text("Search Books") },
                        leadingIcon = {
                            IconButton(onClick = { filePickerLauncher.launch("image/*") }) {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedImageUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Selected Image",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload Image"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            FilledIconButton(
                                onClick = {
                                    if (selectedImageUri != null) {
                                        viewModel.searchByImage(context, selectedImageUri!!)
                                    } else {
                                        performSearch(query)
                                    }
                                    keyboardController?.hide()
                                },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (selectedImageUri != null) {
                                    viewModel.searchByImage(context, selectedImageUri!!)
                                } else {
                                    performSearch(query)
                                }
                                keyboardController?.hide()
                            }
                        ),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                // Suggestions Dropdown
                if (suggestions.isNotEmpty() && query.length >= 3) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            suggestions.forEach { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion) },
                                    modifier = Modifier.clickable {
                                        query = suggestion
                                        performSearch(suggestion)
                                        keyboardController?.hide()
                                    },
                                    leadingContent = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                GenreFilterRow(
                    selectedGenre = selectedGenre,
                    onGenreSelected = { genre ->
                        selectedGenre = genre
                        performSearch(query)
                    }
                )
            }
        }

        // 2. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp) // Adjusted for Search bar height
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> { 
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Start your collection.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Search for the title that inspires you",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
                        )
                    }
                }

                is SearchUiState.Loading -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        repeat(5) {
                            BookSkeletonItem()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                is SearchUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Scrolling List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 44.dp, // Leave room for sticky results header
                                bottom = 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.books) { book ->
                                BookListItem(
                                    book = book,
                                    query = query,
                                    onClick = {
                                        onBookClick(book)
                                        query = ""
                                        selectedGenre = null
                                        selectedImageUri = null
                                        viewModel.resetSearch()
                                    }
                                )
                            }
                        }

                        // Sticky Blurred "Results" Header
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .zIndex(2f)
                                .blur(10.dp), // Frosted glass effect
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            tonalElevation = 4.dp
                        ) { }
                        
                        // Results Text (Overlay so it isn't blurred)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .zIndex(2f)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Search Results",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.books.size} found",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is SearchUiState.Error -> { 
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error fetching books. Please try again.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreFilterRow(
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val genres = listOf("Fiction", "Mystery", "Fantasy", "Sci-Fi", "History", "Romance")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(genres) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = {
                    if (selectedGenre == genre) onGenreSelected(null)
                    else onGenreSelected(genre)
                },
                label = { Text(genre) },
                leadingIcon = if (selectedGenre == genre) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
