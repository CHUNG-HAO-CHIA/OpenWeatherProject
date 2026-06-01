package com.app.openweather.core.domain.usecase

import app.cash.turbine.test
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.CityRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSavedCitiesUseCaseTest {

    private val repository: CityRepository = mockk()
    private val useCase = GetSavedCitiesUseCase(repository)

    @Test
    fun `invoke should return Flow of saved cities from repository`() = runTest {
        // Arrange
        val mockCities = listOf(
            SavedCity(
                id = "Taipei,TW",
                name = "Taipei",
                country = "TW",
                state = null,
                lat = 25.0330,
                lon = 121.5654,
                isFavorite = true,
                savedAt = 123456789L
            )
        )
        every { repository.getSavedCities() } returns flowOf(mockCities)

        // Act & Assert
        useCase().test {
            val result = awaitItem()
            assertEquals(mockCities, result)
            awaitComplete()
        }

        // Verify that the repository method was called
        verify(exactly = 1) { repository.getSavedCities() }
    }
}
