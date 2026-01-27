package com.example.mcamp25.readstack.ui.screens.search

import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readstack.data.network.BookItem
import com.example.mcamp25.readstack.ui.components.BookSkeletonItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    onSearch: () -> Unit,
    onImageSearchTrigger: () -> Unit,
    onImagePickerLaunch: () -> Unit,
    selectedImageUri: Uri?,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?
) {
    Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().zIndex(3f)) {
        Surface(
            modifier = Modifier.matchParentSize().blur(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            tonalElevation = 8.dp
        ) { }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Box {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search for books...") },
                    leadingIcon = {
                        IconButton(onClick = onImagePickerLaunch) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(selectedImageUri).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else Icon(Icons.Default.Add, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    trailingIcon = {
                        FilledIconButton(
                            onClick = { if (selectedImageUri != null) onImageSearchTrigger() else onSearch(); keyboardController?.hide() },
                            modifier = Modifier.padding(end = 4.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Icon(Icons.Default.Search, null) }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (selectedImageUri != null) onImageSearchTrigger() else onSearch(); keyboardController?.hide() }),
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Final safety filter for uniqueness in the UI
            val uniqueSuggestions = remember(suggestions) {
                suggestions.distinctBy { it.lowercase().trim() }.take(5)
            }

            if (uniqueSuggestions.isNotEmpty() && query.length >= 3) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column {
                        uniqueSuggestions.forEach { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                modifier = Modifier.clickable {
                                    onQueryChange(suggestion)
                                    onSearch()
                                    keyboardController?.hide()
                                },
                                leadingContent = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GenreFilterRow(selectedGenre, onGenreSelected)
        }
    }
}

@Composable
internal fun WelcomeNudge() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Default.AutoStories, null, Modifier.size(120.dp), MaterialTheme.colorScheme.secondary.copy(0.2f))
        Spacer(Modifier.height(24.dp))
        Text("Find your next great read.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
        Text("Search for a title or upload a cover to begin.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp))
    }
}

@Composable
internal fun LoadingSkeletons() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        repeat(5) { BookSkeletonItem(); Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
internal fun FoundResults(books: List<BookItem>, currentQuery: String, onBookClick: (BookItem) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 44.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(books, key = { it.id }) { book ->
                BookListItem(book = book, query = currentQuery, onClick = { onBookClick(book) })
            }
        }
        Surface(Modifier.fillMaxWidth().height(40.dp).zIndex(2f).blur(10.dp), color = MaterialTheme.colorScheme.surface.copy(0.7f), tonalElevation = 4.dp) { }
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).zIndex(2f).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Search Results", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("${books.size} results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ErrorMessage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("We couldn't fetch those books right now. Try again?", color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GenreFilterRow(selectedGenre: String?, onGenreSelected: (String?) -> Unit, modifier: Modifier = Modifier) {
    val genres = listOf("Fiction", "Mystery", "Fantasy", "Sci-Fi", "History", "Romance")
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        items(genres) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { if (selectedGenre == genre) onGenreSelected(null) else onGenreSelected(genre) },
                label = { Text(genre) },
                leadingIcon = if (selectedGenre == genre) { { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } } else null,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
