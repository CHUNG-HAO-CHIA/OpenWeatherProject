package com.app.openweather.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

internal class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.newBuilder()
            .addQueryParameter("appid", apiKey)
            .build()
        return chain.proceed(chain.request().newBuilder().url(url).build())
    }
}
