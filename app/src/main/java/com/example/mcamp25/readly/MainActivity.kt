package com.example.mcamp25.readly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.mcamp25.readly.ui.*
import com.example.mcamp25.readly.ui.theme.ReadlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadlyTheme {
                val navController = rememberNavController()
                val items = listOf(
                    BottomNavigationItem("Search", Destination.Search, Icons.Default.Search),
                    BottomNavigationItem("Reading List", Destination.ReadingList,
                        Icons.AutoMirrored.Filled.List
                    )
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } == true,
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
                        modifier = Modifier.padding(innerPadding)
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
                            ReadingListScreen()
                        }
                        composable<Destination.BookDetail> { backStackEntry ->
                            val bookDetail: Destination.BookDetail = backStackEntry.toRoute()
                            BookDetailScreen(
                                bookId = bookDetail.bookId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavigationItem(
    val label: String,
    val destination: Destination,
    val icon: ImageVector
)

@Composable
fun ReadingListScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Reading List Screen")
    }
}
