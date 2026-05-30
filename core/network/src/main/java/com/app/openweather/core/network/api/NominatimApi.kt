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
        @Query("namedetails") namedetails: Int = 1,
        @Query("zoom") zoom: Int = 10,
        //@Query("accept-language") language: String = "en,zh-TW,zh-CN,zh,ja,ko,fr,de,es,pt,ar,ru",
    ): List<NominatimDto>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("namedetails") namedetails: Int = 1,
        @Query("zoom") zoom: Int = 10,
        //@Query("accept-language") language: String = "en,zh-TW,zh-CN,zh,ja,ko,fr,de,es,pt,ar,ru",
    ): NominatimDto
}
