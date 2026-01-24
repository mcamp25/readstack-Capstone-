package com.example.mcamp25.readstack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.work.*
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.mcamp25.readstack.data.local.BookEntity
import com.example.mcamp25.readstack.data.network.BookItem
import com.example.mcamp25.readstack.data.worker.LibrarySyncWorker
import com.example.mcamp25.readstack.ui.navigation.Destination
import com.example.mcamp25.readstack.ui.screens.detail.BookDetailScreen
import com.example.mcamp25.readstack.ui.screens.detail.BookDetailViewModel
import com.example.mcamp25.readstack.ui.screens.list.ReadingListViewModel
import com.example.mcamp25.readstack.ui.screens.search.SearchScreen
import com.example.mcamp25.readstack.ui.screens.search.SearchViewModel
import com.example.mcamp25.readstack.ui.theme.ReadstackTheme
import com.example.mcamp25.readstack.ui.SharpenAndContrastTransformation
import com.example.mcamp25.readstack.ui.toHighResBookUrl
import kotlinx.coroutines.launch

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    )
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkNotificationPermission()
        
        enableEdgeToEdge()
        setContent {
            ReadstackTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val haptic = LocalHapticFeedback.current
                
                val items = listOf(
                    BottomNavigationItem("Search", Destination.Search, Icons.Default.Search),
                    BottomNavigationItem("Reading List", Destination.ReadingList,
                        Icons.AutoMirrored.Filled.List
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        Box {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(10.dp)
                                    .border(
                                        width = 0.5.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                        ),
                                        shape = androidx.compose.ui.graphics.RectangleShape
                                    )
                            ) {}

                            NavigationBar(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                tonalElevation = 0.dp
                            ) {
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                val currentDestination = navBackStackEntry?.destination
                                items.forEach { item ->
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } == true ||
                                                (item.destination is Destination.Search && currentDestination?.hierarchy?.any { it.hasRoute(
                                                    Destination.BookDetail::class) } == true),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        ),
                                        onClick = {
                                            navController.navigate(item.destination) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Destination.Search,
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(400)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                        popExitTransition = { fadeOut(animationSpec = tween(400)) }
                    ) {
                        composable<Destination.Search> {
                            val viewModel: SearchViewModel = viewModel()
                            SearchScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                                onBookClick = { book: BookItem ->
                                    val pages = book.volumeInfo.pageCount ?: book.volumeInfo.printedPageCount
                                    val date = book.volumeInfo.publishedDate
                                    navController.navigate(Destination.BookDetail(
                                        bookId = book.id,
                                        initialPages = pages,
                                        initialDate = date
                                    ))
                                }
                            )
                        }
                        composable<Destination.ReadingList> {
                            val viewModel: ReadingListViewModel = viewModel(factory = ReadingListViewModel.Factory)
                            
                            ReadingListScreen(
                                viewModel = viewModel,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                onBookClick = { bookId ->
                                    navController.navigate(Destination.BookDetail(bookId))
                                },
                                onSyncClick = { 
                                    scheduleLibrarySync()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Library sync started")
                                    }
                                },
                                onBookDeleted = { book ->
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Removed ${book.title} from reading list",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            // UNDO FEEDBACK
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.addBook(book)
                                        }
                                    }
                                }
                            )
                        }
                        composable<Destination.BookDetail> { backStackEntry ->
                            val bookDetail: Destination.BookDetail = backStackEntry.toRoute()
                            val viewModel: BookDetailViewModel = viewModel(factory = BookDetailViewModel.Factory)
                            BookDetailScreen(
                                bookId = bookDetail.bookId,
                                initialPages = bookDetail.initialPages,
                                initialDate = bookDetail.initialDate,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun scheduleLibrarySync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(immediateRequest)
    }
}

data class BottomNavigationItem(
    val label: String,
    val destination: Destination,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    viewModel: ReadingListViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onBookClick: (String) -> Unit,
    onSyncClick: () -> Unit,
    onBookDeleted: (BookEntity) -> Unit
) {
    val books by viewModel.readingList.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }
    
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = { Text("Reading List") },
                    actions = {
                        FilledIconButton(
                            onClick = {
                                onSyncClick()
                                isRefreshing.value = true
                                scope.launch {
                                    kotlinx.coroutines.delay(1500)
                                    isRefreshing.value = false
                                }
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Library",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing.value,
            state = state,
            onRefresh = {
                isRefreshing.value = true
                onSyncClick()
                scope.launch {
                    kotlinx.coroutines.delay(1500)
                    isRefreshing.value = false
                }
            },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), bottom = bottomPadding)
        ) {
            val readingList = books
            if (readingList == null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(5) { BookSkeletonItem() }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            } else if (readingList.isEmpty()) {
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your reading journey starts here.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Search for a book to fill your list!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 40.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(readingList, key = { it.id }) { book ->
                            var isVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { isVisible = true }
                            
                            @Suppress("DEPRECATION")
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        // DELETE FEEDBACK
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.removeFromReadingList(book)
                                        onBookDeleted(book)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            // RESET state if it was dismissed but the book is still here (Undo)
                            LaunchedEffect(book) {
                                if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                    dismissState.reset()
                                }
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                modifier = Modifier.animateItem(),
                                enter = fadeIn(animationSpec = tween(500)) +
                                        slideInVertically(initialOffsetY = { it / 2 }),
                                exit = fadeOut()
                            ) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val color = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                            else -> Color.Transparent
                                        }
                                        if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(MaterialTheme.shapes.medium)
                                                    .background(color)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        LocalBookListItem(
                                            book = book,
                                            onClick = { onBookClick(book.id) },
                                            onRatingChanged = { newRating -> viewModel.updateRating(book.id, newRating) }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Sticky Blurred Header for Reading List
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .zIndex(2f)
                            .blur(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        tonalElevation = 4.dp
                    ) {}

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .zIndex(2f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "${readingList.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    val haptic = LocalHapticFeedback.current
    val cleanDescription = remember(book.description) {
        android.text.Html.fromHtml(book.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
    }
    val noCoverPainter = painterResource(id = R.drawable.no_cover)

    // Provide custom gold ripple
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.colorScheme.secondary)
    ) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(if (book.thumbnail.isNullOrBlank() || book.thumbnail == "null") null else book.thumbnail.toHighResBookUrl())
                        .crossfade(true)
                        .transformations(SharpenAndContrastTransformation())
                        .build(),
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(MaterialTheme.shapes.small)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                )
                            )
                        },
                    contentScale = ContentScale.Crop
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (book.isCurrentlyReading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = "Currently Reading",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (book.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Finished",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    RatingBarMini(
                        rating = book.rating,
                        onRatingChanged = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRatingChanged(it) 
                        }
                    )

                    Row(
                        modifier = Modifier
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        book.publishedDate?.let { date ->
                            val year = if (date.length >= 4) date.take(4) else date
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(4.dp))
                                Text(year, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        book.pageCount?.let { count ->
                            if (count > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.width(4.dp))
                                    Text("$count p", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
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
                Column(modifier = Modifier.align(Alignment.Top)) {
                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out \"${book.title}\" by ${book.author}!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Book"))
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBarMini(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            // 1. Create an animated size for each star
            val starSize by animateDpAsState(
                targetValue = if (i <= rating) 28.dp else 24.dp,
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
                    .size(starSize) // 2. Use the animated size here
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Removes the grey circle ripple to keep it clean
                    ) {
                        onRatingChanged(i)
                    }
            )
        }
    }
}
@Composable
fun BookSkeletonItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .clip(MaterialTheme.shapes.small)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .shimmerEffect())
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(20.dp)
                    .shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                // Reservation for metadata chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(30.dp).height(14.dp).clip(MaterialTheme.shapes.small).shimmerEffect())
                    Box(modifier = Modifier.width(30.dp).height(14.dp).clip(MaterialTheme.shapes.small).shimmerEffect())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .shimmerEffect())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .shimmerEffect())
            }
        }
    }
}
