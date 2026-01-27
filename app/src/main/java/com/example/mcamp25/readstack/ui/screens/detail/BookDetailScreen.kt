package com.example.mcamp25.readstack.ui.screens.detail

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String,
    initialPages: Int? = null,
    initialDate: String? = null,
    vm: BookDetailViewModel,
    onBackClick: () -> Unit
) {
    val details = vm.details
    val currentRating = vm.currentRating
    val isRead = vm.isRead
    val inProgress = vm.inProgress
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
        vm.getBook(bookId)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()) {
        
        // 1. Content Layer
        Box(modifier = Modifier.fillMaxSize()) {
            when (details) {
                is BookDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is BookDetailUiState.Success -> {
                    SuccessContent(
                        book = details.book,
                        bookId = bookId,
                        currentRating = currentRating,
                        isRead = isRead,
                        inProgress = inProgress,
                        initialPages = initialPages,
                        initialDate = initialDate,
                        vm = vm,
                        haptic = haptic,
                        vibrator = vibrator
                    )
                }
                is BookDetailUiState.Error -> {
                    ErrorState(onRetry = { vm.getBook(bookId) })
                }
            }
        }

        // 2. Blurred Header Background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .zIndex(2f)
                .blur(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            tonalElevation = 8.dp
        ) {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // 3. Top Action Bar
        DetailTopBar(onBackClick = onBackClick)
    }
}

@Composable
private fun DetailTopBar(onBackClick: () -> Unit) {
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

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Error loading book details", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
