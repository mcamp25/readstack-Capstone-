package com.example.mcamp25.readly.ui.screens.search

import android.text.Html
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readly.R
import com.example.mcamp25.readly.data.network.BookItem
import com.example.mcamp25.readly.shimmerEffect

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
