package com.example.mcamp25.readstack.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.mcamp25.readstack.ui.navigation.Destination
import com.example.mcamp25.readstack.ui.screens.detail.BookDetailScreen
import com.example.mcamp25.readstack.ui.screens.detail.BookDetailViewModel
import com.example.mcamp25.readstack.ui.screens.list.ReadingListScreen
import com.example.mcamp25.readstack.ui.screens.list.ReadingListViewModel
import com.example.mcamp25.readstack.ui.screens.search.SearchScreen
import kotlinx.coroutines.launch

@Composable
fun ReadstackApp(onSync: () -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Search,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            composable<Destination.Search> {
                SearchScreen(
                    viewModel = viewModel(),
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                    onBookClick = { book ->
                        navController.navigate(Destination.BookDetail(
                            bookId = book.id,
                            initialPages = book.volumeInfo.pageCount ?: book.volumeInfo.printedPageCount,
                            initialDate = book.volumeInfo.publishedDate
                        ))
                    }
                )
            }
            composable<Destination.ReadingList> {
                val vm: ReadingListViewModel = viewModel(factory = ReadingListViewModel.Factory)
                ReadingListScreen(
                    vm = vm,
                    bottomPadding = innerPadding.calculateBottomPadding(),
                    onBookClick = { bookId -> navController.navigate(Destination.BookDetail(bookId)) },
                    onSync = onSync,
                    onDeleted = { book ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Removed ${book.title}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.addBook(book)
                            }
                        }
                    }
                )
            }
            composable<Destination.BookDetail> { backStackEntry ->
                val detail: Destination.BookDetail = backStackEntry.toRoute()
                BookDetailScreen(
                    bookId = detail.bookId,
                    initialPages = detail.initialPages,
                    initialDate = detail.initialDate,
                    vm = viewModel(factory = BookDetailViewModel.Factory),
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        BottomNavigationItem("Search", Destination.Search, Icons.Default.Search),
        BottomNavigationItem("Reading List", Destination.ReadingList, Icons.AutoMirrored.Filled.List)
    )

    Box {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.matchParentSize().blur(10.dp).border(BorderStroke(0.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))))
        ) {}

        NavigationBar(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimary, tonalElevation = 0.dp) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, item.label) },
                    label = { Text(item.label) },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } == true ||
                            (item.destination is Destination.Search && currentDestination?.hierarchy?.any { it.hasRoute(Destination.BookDetail::class) } == true),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    ),
                    onClick = {
                        navController.navigate(item.destination) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

data class BottomNavigationItem(val label: String, val destination: Destination, val icon: ImageVector)
