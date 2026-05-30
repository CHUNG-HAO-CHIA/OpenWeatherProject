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
        return nominatim.search(query).map { it.toDomain() }
    }

    override suspend fun saveCity(city: SavedCity) {
        dao.insertCity(city.toEntity())
    }

    override suspend fun getCityById(cityId: String): SavedCity? {
        return dao.getById(cityId)?.toDomain()
    }

    override suspend fun getFavoriteCount(): Int {
        return dao.favoriteCount()
    }

    override suspend fun setFavorite(cityId: String, isFavorite: Boolean) {
        dao.setFavorite(cityId, isFavorite)
    }

    override suspend fun deleteCity(cityId: String) = dao.deleteCity(cityId)

    override suspend fun reverseGeocode(lat: Double, lon: Double): SavedCity {
        return nominatim.reverse(lat, lon).toDomain()
    }
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
    val cityName = addr?.city
        ?: addr?.town
        ?: addr?.village
        ?: addr?.county
        ?: addr?.state
        ?: displayName.substringBefore(",").trim()
    val latD = lat.toDouble()
    val lonD = lon.toDouble()
    // show state as subtitle only when it differs from cityName
    val subtitle = if (addr?.state != cityName) addr?.state else null
    return SavedCity(
        id = "%.4f,%.4f".format(latD, lonD),
        name = cityName,
        country = addr?.countryCode?.uppercase() ?: addr?.country.orEmpty(),
        state = subtitle,
        lat = latD,
        lon = lonD,
        isFavorite = false,
        savedAt = System.currentTimeMillis(),
    )
}
