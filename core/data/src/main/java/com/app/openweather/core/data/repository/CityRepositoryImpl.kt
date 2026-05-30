package com.app.openweather.core.data.repository

import com.app.openweather.core.data.local.dao.CityDao
import com.app.openweather.core.data.local.entity.SavedCityEntity
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.CityRepository
import com.app.openweather.core.domain.usecase.MAX_FAVORITES
import com.app.openweather.core.network.api.NominatimApi
import com.app.openweather.core.network.dto.NominatimDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CityRepositoryImpl(
    private val dao: CityDao,
    private val nominatim: NominatimApi,
) : CityRepository {

    override fun getSavedCities(): Flow<List<SavedCity>> =
        dao.observeSavedCities().map { list -> list.map { it.toDomain() } }

    override suspend fun searchCities(query: String): List<SavedCity> {
        if (query.isBlank()) return emptyList()
        return nominatim.search(query)
            .filter { it.address.countryCode != null }   // must at least have a country
            .distinctBy { "%.2f,%.2f".format(it.lat.toDouble(), it.lon.toDouble()) }
            .map { it.toDomain() }
    }

    override suspend fun saveCity(city: SavedCity) {
        dao.insertCity(city.toEntity())
    }

    override suspend fun toggleFavorite(cityId: String): Result<Unit> {
        val entity = dao.getById(cityId) ?: return Result.failure(Exception("City not found"))
        return if (entity.isFavorite) {
            dao.setFavorite(cityId, false)
            Result.success(Unit)
        } else {
            val count = dao.favoriteCount()
            if (count >= MAX_FAVORITES) {
                Result.failure(Exception("最多只能加星 $MAX_FAVORITES 個地點"))
            } else {
                dao.setFavorite(cityId, true)
                Result.success(Unit)
            }
        }
    }

    override suspend fun deleteCity(cityId: String) = dao.deleteCity(cityId)
}

private fun SavedCityEntity.toDomain() = SavedCity(
    id = id, name = name, country = country, state = state,
    lat = lat, lon = lon, isFavorite = isFavorite, savedAt = savedAt,
)

private fun SavedCity.toEntity() = SavedCityEntity(
    id = id, name = name, country = country, state = state,
    lat = lat, lon = lon, isFavorite = isFavorite, savedAt = savedAt,
)

private fun NominatimDto.toDomain(): SavedCity {
    val addr = address
    // city > town > village > county > state > first segment of display_name
    val cityName = addr.city
        ?: addr.town
        ?: addr.village
        ?: addr.county
        ?: addr.state
        ?: displayName.substringBefore(",").trim()
    val latD = lat.toDouble()
    val lonD = lon.toDouble()
    // show state as subtitle only when it differs from cityName
    val subtitle = if (addr.state != cityName) addr.state else null
    return SavedCity(
        id = "%.4f,%.4f".format(latD, lonD),
        name = cityName,
        country = addr.countryCode?.uppercase() ?: addr.country.orEmpty(),
        state = subtitle,
        lat = latD,
        lon = lonD,
        isFavorite = false,
        savedAt = System.currentTimeMillis(),
    )
}
