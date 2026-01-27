package com.example.mcamp25.readstack.ui.screens.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.mcamp25.readstack.data.local.BookEntity
import com.example.mcamp25.readstack.ui.components.BookSkeletonItem
import com.example.mcamp25.readstack.ui.screens.search.LocalBookListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    vm: ReadingListViewModel,
    bottomPadding: Dp = 0.dp,
    onBookClick: (String) -> Unit,
    onSync: () -> Unit,
    onDeleted: (BookEntity) -> Unit
) {
    val books by vm.readingList.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { ReadingListTopBar(onSync) { isRefreshing.value = it } }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing.value,
            state = rememberPullToRefreshState(),
            onRefresh = {
                isRefreshing.value = true
                onSync()
                scope.launch {
                    delay(1500)
                    isRefreshing.value = false
                }
            },
            modifier = Modifier.padding(top = padding.calculateTopPadding(), bottom = bottomPadding)
        ) {
            when {
                books == null -> LoadingList()
                books!!.isEmpty() -> EmptyListHint()
                else -> BookList(books!!, vm, onBookClick, onDeleted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingListTopBar(onSync: () -> Unit, setRefreshing: (Boolean) -> Unit) {
    Surface(shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = { 
                Text(
                    text = "My Personal Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            actions = {
                FilledIconButton(
                    onClick = { onSync(); setRefreshing(true) },
                    modifier = Modifier.padding(end = 8.dp).size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Sync, "Refresh my library", modifier = Modifier.size(28.dp))
                }
            }
        )
    }
}

@Composable
private fun BookList(
    books: List<BookEntity>,
    vm: ReadingListViewModel,
    onBookClick: (String) -> Unit,
    onDeleted: (BookEntity) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.id }) { book ->
                SwipeableBookItem(
                    book = book,
                    onClick = { onBookClick(book.id) },
                    onDelete = {
                        vm.removeFromReadingList(book)
                        onDeleted(book)
                    },
                    onRate = { vm.updateRating(book.id, it) }
                )
            }
        }

        // Sticky Header
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp).zIndex(2f).blur(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            tonalElevation = 4.dp
        ) {}

        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).zIndex(2f).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (books.size == 1) "1 book" else "${books.size} books found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBookItem(
    book: BookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRate: (Int) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
                true
            } else false
        }
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)) + slideInVertically { it / 2 },
        exit = fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 
                    MaterialTheme.colorScheme.errorContainer else Color.Transparent
                Box(
                    modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, "Remove this book", tint = MaterialTheme.colorScheme.error)
                }
            },
            content = {
                LocalBookListItem(
                    book = book,
                    onClick = onClick,
                    onRatingChanged = onRate
                )
            }
        )
    }
}

@Composable
private fun LoadingList() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(5) { BookSkeletonItem() }
    }
}

@Composable
private fun EmptyListHint() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoStories, 
            contentDescription = null, 
            modifier = Modifier.size(100.dp), 
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
        Text(
            text = "Your library is waiting.", 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Discover your next adventure and add it here!", 
            style = MaterialTheme.typography.bodyMedium, 
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}
