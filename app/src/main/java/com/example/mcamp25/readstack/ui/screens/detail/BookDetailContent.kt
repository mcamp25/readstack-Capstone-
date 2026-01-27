package com.example.mcamp25.readstack.ui.screens.detail

import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mcamp25.readstack.R
import com.example.mcamp25.readstack.data.network.BookItem
import com.example.mcamp25.readstack.ui.components.RatingBar
import com.example.mcamp25.readstack.ui.screens.detail.components.CoverHeader
import com.example.mcamp25.readstack.ui.screens.detail.components.MetaRow
import com.example.mcamp25.readstack.ui.screens.detail.components.ReadingControls
import com.example.mcamp25.readstack.ui.screens.detail.components.parseHtml

@Composable
fun SuccessContent(
    book: BookItem,
    bookId: String,
    currentRating: Int,
    isRead: Boolean,
    inProgress: Boolean,
    initialPages: Int?,
    initialDate: String?,
    vm: BookDetailViewModel,
    haptic: HapticFeedback,
    vibrator: Vibrator
) {
    val noCoverPainter = painterResource(id = R.drawable.no_cover)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(modifier = Modifier.height(64.dp + 16.dp))

        CoverHeader(
            imageLinks = book.volumeInfo.imageLinks,
            title = book.volumeInfo.title,
            noCoverPainter = noCoverPainter
        )

        RatingBar(
            rating = currentRating,
            onRatingChanged = { vm.updateRating(bookId, it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetaRow(book.volumeInfo.publishedDate ?: initialDate)

        Spacer(modifier = Modifier.height(16.dp))

        ReadingControls(
            isRead = isRead,
            inProgress = inProgress,
            onAdd = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.addToReadingList(book, initialPages, initialDate)
            },
            onToggleReading = { vm.toggleCurrentlyReading(bookId) },
            onToggleRead = { vm.toggleReadStatus(bookId) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = book.volumeInfo.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = book.volumeInfo.authors?.joinToString(", ") ?: "Someone wrote this...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = book.volumeInfo.description?.parseHtml ?: "No description available for this one.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
