package com.example.mcamp25.readstack.ui.screens.search

import android.content.Intent
import android.text.Html
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.size.Precision
import com.example.mcamp25.readstack.R
import com.example.mcamp25.readstack.data.local.BookEntity
import com.example.mcamp25.readstack.data.network.BookItem
import com.example.mcamp25.readstack.data.network.getBestUrl
import com.example.mcamp25.readstack.ui.SharpenAndContrastTransformation
import com.example.mcamp25.readstack.ui.components.shimmerEffect
import com.example.mcamp25.readstack.ui.toHighResBookUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListItem(book: BookItem, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cleanDescription = remember(book.volumeInfo.description) {
        book.volumeInfo.description?.let {
            Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString()
        } ?: ""
    }

    val noCoverPainter = painterResource(id = R.drawable.no_cover)
    val highlightColor = MaterialTheme.colorScheme.secondary
    
    val highlightedTitle = remember(book.volumeInfo.title, query, highlightColor) {
        getHighlightedText(
            text = book.volumeInfo.title,
            query = query,
            color = highlightColor
        )
    }

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = highlightColor)
    ) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(book.volumeInfo.imageLinks.getBestUrl()?.toHighResBookUrl())
                        .transformations(SharpenAndContrastTransformation(contrast = 1.1f, sharpenAmount = 0.5f))
                        .crossfade(true)
                        .allowHardware(false)
                        .precision(Precision.EXACT)
                        .build(),
                    contentDescription = book.volumeInfo.title,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                ) {
                    val state = painter.state
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                        }
                        is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Empty -> {
                            Image(
                                painter = noCoverPainter,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Inside
                            )
                        }
                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = highlightedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.volumeInfo.authors?.joinToString(", ") ?: "Unknown Author",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
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
                IconButton(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out this book: ${book.volumeInfo.title} by ${book.volumeInfo.authors?.joinToString(", ")}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBookListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val noCoverPainter = painterResource(id = R.drawable.no_cover)

    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.secondary)) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (book.thumbnail.isNullOrBlank() || book.thumbnail == "null") null else book.thumbnail.toHighResBookUrl())
                        .transformations(SharpenAndContrastTransformation(contrast = 1.1f, sharpenAmount = 0.5f))
                        .crossfade(true).allowHardware(false).precision(Precision.EXACT).build(),
                    contentDescription = book.title,
                    modifier = Modifier.width(90.dp).height(130.dp).clip(MaterialTheme.shapes.small).drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    },
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High
                ) {
                    val state = painter.state
                    when (state) {
                        is AsyncImagePainter.State.Loading -> Box(
                            modifier = Modifier.fillMaxSize().shimmerEffect()
                        )

                        is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Empty -> Image(
                            noCoverPainter,
                            null,
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Inside
                        )

                        else -> SubcomposeAsyncImageContent()
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                        if (book.isCurrentlyReading) StatusBadge(Icons.Default.AutoStories, MaterialTheme.colorScheme.secondary)
                        if (book.isRead) StatusBadge(Icons.Default.Check, MaterialTheme.colorScheme.primary)
                    }
                    Text(book.author, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RatingBarMini(rating = book.rating, onRatingChanged = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRatingChanged(it) })
                    BookMetadata(book.publishedDate, book.pageCount)
                    Text(Html.fromHtml(book.description, Html.FROM_HTML_MODE_COMPACT).toString(), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out this book: ${book.title} by ${book.author}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(icon: ImageVector, color: Color) {
    Spacer(Modifier.width(8.dp))
    Surface(shape = CircleShape, color = color, modifier = Modifier.size(26.dp), shadowElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun BookMetadata(date: String?, pages: Int?) {
    Row(modifier = Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        date?.take(4)?.let { year -> MetaChip(Icons.Default.CalendarMonth, year) }
        pages?.takeIf { it > 0 }?.let { count -> MetaChip(Icons.AutoMirrored.Filled.MenuBook, "$count p") }
    }
}

@Composable
private fun MetaChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun RatingBarMini(rating: Int, onRatingChanged: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            val starSize by animateDpAsState(if (i <= rating) 28.dp else 24.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "starSize")
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MaterialTheme.colorScheme.secondary else Color.Gray,
                modifier = Modifier.size(starSize).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRatingChanged(i) }
            )
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
