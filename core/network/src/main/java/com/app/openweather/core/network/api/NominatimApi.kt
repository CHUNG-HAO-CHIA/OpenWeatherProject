package com.app.openweather.core.network.api

import com.app.openweather.core.network.dto.NominatimDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 8,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") language: String = "zh-TW,zh,en",
    ): List<NominatimDto>
}
