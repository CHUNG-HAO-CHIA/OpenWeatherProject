package com.app.openweather.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.openweather.core.data.local.entity.SavedCityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {

    // Favorites first, then by savedAt desc
    @Query("SELECT * FROM saved_city ORDER BY isFavorite DESC, savedAt DESC")
    fun observeSavedCities(): Flow<List<SavedCityEntity>>

    @Query("SELECT COUNT(*) FROM saved_city WHERE isFavorite = 1")
    suspend fun favoriteCount(): Int

    @Query("SELECT * FROM saved_city WHERE id = :id")
    suspend fun getById(id: String): SavedCityEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCity(entity: SavedCityEntity)

    @Query("UPDATE saved_city SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM saved_city WHERE id = :id")
    suspend fun deleteCity(id: String)
}
