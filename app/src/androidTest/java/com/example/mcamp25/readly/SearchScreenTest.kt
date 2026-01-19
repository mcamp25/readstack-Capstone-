package com.example.mcamp25.readly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.mcamp25.readly.ui.screens.search.SearchScreen
import com.example.mcamp25.readly.ui.screens.search.SearchViewModel
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchScreen_initialState_showsWelcomeText() {
        composeTestRule.setContent {
            SearchScreen(
                viewModel = SearchViewModel(),
                onBookClick = {}
            )
        }

        // Checks if the welcome text is visible
        composeTestRule
            .onNodeWithText("Start your collection.")
            .assertIsDisplayed()
    }
}
