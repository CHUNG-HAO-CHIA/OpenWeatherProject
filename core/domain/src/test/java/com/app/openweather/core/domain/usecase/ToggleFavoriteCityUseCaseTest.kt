package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.CityRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFavoriteCityUseCaseTest {

    private val repository: CityRepository = mockk()
    private val useCase = ToggleFavoriteCityUseCase(repository)

    @Test
    fun `when city is not found, should return failure`() = runTest {
        // Arrange
        val cityId = "UnknownCity"
        coEvery { repository.getCityById(cityId) } returns null

        // Act
        val result = useCase(cityId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals("City not found", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.setFavorite(any(), any()) }
    }

    @Test
    fun `when city is already favorite, should unfavorite and return success`() = runTest {
        // Arrange
        val cityId = "Taipei,TW"
        val mockCity = createMockCity(cityId, isFavorite = true)
        coEvery { repository.getCityById(cityId) } returns mockCity
        coEvery { repository.setFavorite(cityId, false) } returns Unit

        // Act
        val result = useCase(cityId)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.setFavorite(cityId, false) }
    }

    @Test
    fun `when city is not favorite and max favorites reached, should return failure`() = runTest {
        // Arrange
        val cityId = "Taipei,TW"
        val mockCity = createMockCity(cityId, isFavorite = false)
        coEvery { repository.getCityById(cityId) } returns mockCity
        coEvery { repository.getFavoriteCount() } returns MAX_FAVORITES

        // Act
        val result = useCase(cityId)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MaxFavoritesReachedException)
        coVerify(exactly = 0) { repository.setFavorite(any(), any()) }
    }

    @Test
    fun `when city is not favorite and under max favorites, should set favorite and return success`() = runTest {
        // Arrange
        val cityId = "Taipei,TW"
        val mockCity = createMockCity(cityId, isFavorite = false)
        coEvery { repository.getCityById(cityId) } returns mockCity
        coEvery { repository.getFavoriteCount() } returns MAX_FAVORITES - 1
        coEvery { repository.setFavorite(cityId, true) } returns Unit

        // Act
        val result = useCase(cityId)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.setFavorite(cityId, true) }
    }

    private fun createMockCity(id: String, isFavorite: Boolean): SavedCity {
        return SavedCity(
            id = id,
            name = "TestCity",
            country = "TC",
            state = null,
            lat = 0.0,
            lon = 0.0,
            isFavorite = isFavorite,
            savedAt = 0L
        )
    }
}
