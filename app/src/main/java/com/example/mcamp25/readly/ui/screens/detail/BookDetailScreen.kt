package com.example.mcamp25.readly.ui.screens.detail

import android.text.Html
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readly.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    viewModel: BookDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val currentRating = viewModel.currentRating

    LaunchedEffect(bookId) {
        viewModel.getBook(bookId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is BookDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is BookDetailUiState.Success -> {
                    val book = uiState.book
                    val noCoverPainter = painterResource(id = R.drawable.no_cover)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.volumeInfo.imageLinks?.thumbnailUrl?.replace("http:", "https:"))
                                .crossfade(true)
                                .build(),
                            contentDescription = book.volumeInfo.title,
                            modifier = Modifier
                                .height(250.dp)
                                .width(180.dp),
                            contentScale = ContentScale.FillBounds,
                            error = noCoverPainter,
                            fallback = noCoverPainter,
                            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        RatingBar(
                            rating = currentRating,
                            onRatingChanged = { newRating ->
                                viewModel.updateRating(bookId, newRating)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.addToReadingList(book) },
                            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                                )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add to Reading List")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = book.volumeInfo.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Html.fromHtml(book.volumeInfo.description ?: "No description available",
                                Html.FROM_HTML_MODE_COMPACT).toString(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is BookDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error loading book details")
                        Button(onClick = { viewModel.getBook(bookId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MaterialTheme.colorScheme.secondary else Color.Gray,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}
