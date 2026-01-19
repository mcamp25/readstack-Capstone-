package com.example.mcamp25.readly

import com.example.mcamp25.readly.ui.screens.search.SearchUiState
import com.example.mcamp25.readly.ui.screens.search.SearchViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchViewModelTest {
    @Test
    fun viewModel_initialState_isIdle() {
        val viewModel = SearchViewModel()
        // Verifies that when the app starts, it shows the welcome screen (Idle)
        assertTrue(viewModel.searchUiState.value is SearchUiState.Idle)
    }
}
