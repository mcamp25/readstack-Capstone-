package com.example.mcamp25.readly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mcamp25.readly.data.local.BookEntity
import com.example.mcamp25.readly.data.worker.LibrarySyncWorker
import com.example.mcamp25.readly.ui.navigation.Destination
import com.example.mcamp25.readly.ui.screens.detail.BookDetailScreen
import com.example.mcamp25.readly.ui.screens.detail.BookDetailViewModel
import com.example.mcamp25.readly.ui.screens.list.ReadingListViewModel
import com.example.mcamp25.readly.ui.screens.search.SearchScreen
import com.example.mcamp25.readly.ui.screens.search.SearchViewModel
import com.example.mcamp25.readly.ui.theme.ReadlyTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkNotificationPermission()
        
        enableEdgeToEdge()
        setContent {
            ReadlyTheme(dynamicColor = false) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                
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
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Destination.Search,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(400)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                        popExitTransition = { fadeOut(animationSpec = tween(400)) }
                    ) {
                        composable<Destination.Search> {
                            val viewModel: SearchViewModel = viewModel()
                            SearchScreen(
                                viewModel = viewModel,
                                onBookClick = { book ->
                                    navController.navigate(Destination.BookDetail(book.id))
                                }
                            )
                        }
                        composable<Destination.ReadingList> {
                            val viewModel: ReadingListViewModel = viewModel(factory = ReadingListViewModel.Factory)
                            ReadingListScreen(
                                viewModel = viewModel,
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
    onBookClick: (String) -> Unit,
    onSyncClick: () -> Unit,
    onBookDeleted: (BookEntity) -> Unit
) {
    val books by viewModel.readingList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading List") },
                actions = {
                    FilledIconButton(
                        onClick = onSyncClick,
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
    ) { innerPadding ->
        if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Your reading list is empty")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(books, key = { it.id }) { book ->
                    LocalBookListItem(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onDelete = { 
                            viewModel.removeFromReadingList(book)
                            onBookDeleted(book)
                        },
                        onRatingChanged = { newRating -> viewModel.updateRating(book.id, newRating) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun LocalBookListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanDescription = remember(book.description) {
        android.text.Html.fromHtml(book.description, android.text.Html.FROM_HTML_MODE_COMPACT).toString()
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(intrinsicSize = IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(book.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = book.title,
                modifier = Modifier
                    .width(90.dp)
                    .height(130.dp)
                    .padding(end = 4.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(id = android.R.drawable.ic_menu_report_image),
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                RatingBarMini(
                    rating = book.rating,
                    onRatingChanged = onRatingChanged
                )
                
                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(modifier = Modifier.align(Alignment.Top)) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
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

@Composable
fun RatingBarMini(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) MaterialTheme.colorScheme.secondary else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}
