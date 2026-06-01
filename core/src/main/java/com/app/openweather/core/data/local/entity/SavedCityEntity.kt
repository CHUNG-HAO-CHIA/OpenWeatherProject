package com.app.openweather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_city")
data class SavedCityEntity(
    @PrimaryKey val id: String,   // "$name,$country"
    val name: String,
    val country: String,
    val state: String?,
    val lat: Double,
    val lon: Double,
    val isFavorite: Boolean = false,
    val savedAt: Long = System.currentTimeMillis(),
)
