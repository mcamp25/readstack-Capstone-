package com.example.mcamp25.readly.ui.screens.search

import android.net.Uri
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readly.BookSkeletonItem
import com.example.mcamp25.readly.R
import com.example.mcamp25.readly.data.network.BookItem
import com.example.mcamp25.readly.shimmerEffect

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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val performSearch = {
        val combinedQuery = buildString {
            if (query.isNotBlank()) append(query)
            if (selectedGenre != null) {
                if (isNotEmpty()) append(" ")
                val subject = if (selectedGenre == "Sci-Fi") "Science Fiction" else selectedGenre
                append("subject:\"$subject\"")
            }
        }
        if (combinedQuery.isNotBlank()) {
            viewModel.searchBooks(combinedQuery)
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
        // 1. Top Search Bar with Blur
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
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
                                    performSearch()
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
                                performSearch()
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

                Spacer(modifier = Modifier.height(8.dp))

                GenreFilterRow(
                    selectedGenre = selectedGenre,
                    onGenreSelected = { genre ->
                        selectedGenre = genre
                        performSearch()
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

@Composable
fun BookListItem(book: BookItem, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cleanDescription = remember(book.volumeInfo.description) {
        book.volumeInfo.description?.let {
            Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: ""
    }

    val noCoverPainter = painterResource(id = R.drawable.no_cover)
    val highlightColor = Color(0xFFD35400)
    
    val highlightedTitle = remember(book.volumeInfo.title, query, highlightColor) {
        getHighlightedText(
            text = book.volumeInfo.title,
            query = query,
            color = highlightColor
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.volumeInfo.imageLinks?.thumbnailUrl?.replace("http:", "https:"))
                    .crossfade(true)
                    .build(),
                contentDescription = book.volumeInfo.title,
                modifier = Modifier
                    .width(80.dp)
                    .height(120.dp)
                    .shimmerEffect(),
                contentScale = ContentScale.FillBounds,
                error = noCoverPainter,
                fallback = noCoverPainter,
                placeholder = null
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    book.volumeInfo.publishedDate?.let { date ->
                        val year = if (date.length >= 4) date.take(4) else date
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(4.dp))
                            Text(year, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    val displayPages = book.volumeInfo.pageCount ?: book.volumeInfo.printedPageCount
                    if (displayPages != null && displayPages > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(4.dp))
                            Text("$displayPages p", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun getHighlightedText(text: String, query: String, color: Color): AnnotatedString {
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = 0
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index == -1 || lowerQuery.isBlank()) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Black)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
}
