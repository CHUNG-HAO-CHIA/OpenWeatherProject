package com.app.openweather.feature.weather.viewmodel

import app.cash.turbine.test
import com.app.openweather.core.common.AppError
import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.Forecast
import com.app.openweather.core.domain.usecase.WeatherUseCases
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val useCases: WeatherUseCases = mockk()
    private lateinit var viewModel: WeatherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WeatherViewModel(useCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadWeather should update state with success when use cases return data`() = runTest {
        val weather = CurrentWeather(
            cityName = "Taipei",
            temperature = 25.0,
            feelsLike = 27.0,
            humidity = 80,
            windSpeed = 5.0,
            description = "clear sky",
            iconCode = "01d",
            sunrise = 1600000000L,
            sunset = 1600043200L
        )
        val forecast = Forecast(hourly = emptyList(), daily = emptyList(), timezoneOffsetSec = 28800)

        every { useCases.getCurrentWeather(any(), any()) } returns flowOf(Result.Success(weather))
        every { useCases.getForecast(any(), any()) } returns flowOf(Result.Success(forecast))

        viewModel.loadWeather(25.0, 121.0)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Taipei", state.currentWeather?.cityName)
            assertEquals("25°C", state.currentWeather?.temperature)
            assertFalse(state.isLoading)
            assertEquals(null, state.error)
        }
    }

    @Test
    fun `loadWeather should show full screen error when no previous data and API fails`() = runTest {
        every { useCases.getCurrentWeather(any(), any()) } returns flowOf(Result.Error(AppError.NetworkError))
        every { useCases.getForecast(any(), any()) } returns flowOf(Result.Loading)

        viewModel.loadWeather(25.0, 121.0)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(AppError.NetworkError, state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `loadWeather should show snackbar event when background update fails with existing data`() = runTest {
        // 1. Success load
        val weather = CurrentWeather(
            cityName = "Taipei",
            temperature = 25.0,
            feelsLike = 27.0,
            humidity = 80,
            windSpeed = 5.0,
            description = "clear sky",
            iconCode = "01d",
            sunrise = 1600000000L,
            sunset = 1600043200L
        )
        val forecast = Forecast(hourly = emptyList(), daily = emptyList(), timezoneOffsetSec = 28800)

        every { useCases.getCurrentWeather(any(), any()) } returns flowOf(Result.Success(weather))
        every { useCases.getForecast(any(), any()) } returns flowOf(Result.Success(forecast))

        viewModel.loadWeather(25.0, 121.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // 2. Mock Error for background update
        every { useCases.getCurrentWeather(any(), any()) } returns flowOf(Result.Error(AppError.NetworkError))
        
        viewModel.event.test {
            viewModel.loadWeather(25.0, 121.0)
            val event = awaitItem()
            assertTrue(event is WeatherUiEvent.ShowSnackbar)
            assertEquals(AppError.NetworkError, (event as WeatherUiEvent.ShowSnackbar).error)
        }
        
        // Ensure UI state still has previous data and no full screen error
        assertEquals("Taipei", viewModel.uiState.value.currentWeather?.cityName)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `loadWeather should show offline status when use cases return offline data`() = runTest {
        val weather = CurrentWeather(
            cityName = "Taipei",
            temperature = 20.0,
            feelsLike = 21.0,
            humidity = 70,
            windSpeed = 3.0,
            description = "clouds",
            iconCode = "02d",
            sunrise = 1600000000L,
            sunset = 1600043200L
        )
        val forecast = Forecast(hourly = emptyList(), daily = emptyList(), timezoneOffsetSec = 28800)
        val cachedAt = 1600050000L

        every { useCases.getCurrentWeather(any(), any()) } returns flowOf(Result.Offline(weather, cachedAt))
        every { useCases.getForecast(any(), any()) } returns flowOf(Result.Offline(forecast, cachedAt))

        viewModel.loadWeather(25.0, 121.0)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOffline)
            assertEquals(cachedAt, state.lastUpdated)
            assertEquals("Taipei", state.currentWeather?.cityName)
        }
    }
}
