package com.example.mcamp25.readstack.ui.screens.detail

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Html
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readstack.R
import com.example.mcamp25.readstack.ui.SharpenAndContrastTransformation
import com.example.mcamp25.readstack.ui.toHighResBookUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    initialPages: Int? = null,
    initialDate: String? = null,
    viewModel: BookDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState = viewModel.uiState
    val currentRating = viewModel.currentRating
    val isRead = viewModel.isRead
    val isCurrentlyReading = viewModel.isCurrentlyReading
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(bookId) {
        viewModel.getBook(bookId)
    }

    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        // 1. Main Content (Scrollable)
        Box(modifier = Modifier.fillMaxSize()) {
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
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Combined spacer for Status Bar + Header height
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        Spacer(modifier = Modifier.height(64.dp + 16.dp))

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.volumeInfo.imageLinks?.thumbnailUrl?.toHighResBookUrl())
                                .crossfade(true)
                                .transformations(SharpenAndContrastTransformation())
                                .build(),
                            contentDescription = book.volumeInfo.title,
                            modifier = Modifier
                                .height(250.dp)
                                .width(180.dp),
                            contentScale = ContentScale.FillBounds,
                            error = noCoverPainter,
                            fallback = noCoverPainter,
                            placeholder = noCoverPainter
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        RatingBar(
                            rating = currentRating,
                            onRatingChanged = { newRating ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.updateRating(bookId, newRating)
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            val displayDate = book.volumeInfo.publishedDate ?: initialDate
                            displayDate?.let { date ->
                                val year = if (date.length >= 4) date.take(4) else date
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.width(4.dp))
                                    Text(year, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(50)
                                    }
                                    viewModel.addToReadingList(book, initialPages, initialDate)
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(0.7f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }

                            val readingButtonColor by animateColorAsState(
                                targetValue = if (isCurrentlyReading) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                label = "readingButtonColor"
                            )

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleCurrentlyReading(bookId)
                                },
                                modifier = Modifier.weight(1.3f).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = readingButtonColor,
                                    contentColor = if (isCurrentlyReading) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = if (isCurrentlyReading) null else ButtonDefaults.outlinedButtonBorder(true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(if (isCurrentlyReading) "Reading" else "Read", style = MaterialTheme.typography.labelMedium)
                            }

                            val buttonColor by animateColorAsState(
                                targetValue = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                label = "buttonColor"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isRead) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "contentColor"
                            )

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(30)
                                    }
                                    viewModel.toggleReadStatus(bookId)
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                modifier = Modifier.weight(0.9f).height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = buttonColor,
                                    contentColor = contentColor
                                ),
                                border = if (isRead) null else ButtonDefaults.outlinedButtonBorder(true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = if (isRead) "Done" else "Finish",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
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
                        
                        // Bottom spacer for navigation bar
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                is BookDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(top = 80.dp),
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

        // 2. Full-bleed Sticky Blurred Header (covers Status Bar)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .zIndex(2f)
                .blur(10.dp), // Frosted glass effect
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            tonalElevation = 8.dp
        ) {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // 3. Header Interactive Content (Back Button & Title - Overlayed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .zIndex(3f)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Book Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun RatingBar(
    modifier: Modifier = Modifier,
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = modifier) {
        for (i in 1..5) {
            val starSize by animateDpAsState(
                targetValue = if (i <= rating) 44.dp else 40.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "starSize"
            )

            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MaterialTheme.colorScheme.secondary else Color.Gray,
                modifier = Modifier
                    .size(starSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRatingChanged(i) 
                    }
            )
        }
    }
}
