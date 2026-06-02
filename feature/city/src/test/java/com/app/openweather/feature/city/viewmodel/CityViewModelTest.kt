package com.app.openweather.feature.city.viewmodel

import app.cash.turbine.test
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.CityUseCases
import com.app.openweather.core.domain.usecase.GetWeatherForCitiesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCases: CityUseCases = mockk()
    private val getWeatherForCities: GetWeatherForCitiesUseCase = mockk()
    private lateinit var viewModel: CityViewModel

    private val savedCitiesFlow = MutableStateFlow<List<SavedCity>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { useCases.getSavedCities() } returns savedCitiesFlow
        coEvery { getWeatherForCities(any()) } returns emptyMap()
        viewModel = CityViewModel(useCases, getWeatherForCities)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should load saved cities`() = runTest {
        val cities = listOf(
            SavedCity("1", "Taipei", "Taiwan", null, 25.0, 121.0, true, 0L),
            SavedCity("2", "Tokyo", "Japan", null, 35.0, 139.0, false, 0L)
        )
        savedCitiesFlow.value = cities
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.favoriteCities.size)
            assertEquals("Taipei", state.favoriteCities[0].name)
            assertEquals(1, state.otherCities.size)
            assertEquals("Tokyo", state.otherCities[0].name)
        }
    }

    @Test
    fun `onQueryChange should trigger search after debounce`() = runTest {
        val searchResults = listOf(
            SavedCity("3", "London", "UK", null, 51.5, -0.12, false, 0L)
        )
        coEvery { useCases.searchCities("London") } returns searchResults

        viewModel.onQueryChange("London")
        
        // Before debounce (400ms)
        assertFalse(viewModel.uiState.value.isSearching)
        
        testDispatcher.scheduler.advanceTimeBy(500)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.searchResults.isNotEmpty())
        assertEquals("London", viewModel.uiState.value.searchResults[0].name)
    }

    @Test
    fun `onQueryChange with short query should clear search results`() = runTest {
        viewModel.onQueryChange("L")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
        assertEquals("L", viewModel.uiState.value.query)
    }
}
