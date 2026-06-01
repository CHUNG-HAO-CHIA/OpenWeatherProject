package com.app.openweather.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class LanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        // Locale.getDefault() 在任何執行緒都能正確反映 AppCompatDelegate.setApplicationLocales() 的設定
        val locale = Locale.getDefault()
        val lang = locale.language.ifEmpty { "en" }
        val country = locale.country

        // 轉換為各 API 支援的格式
        // OWM 需要: zh_tw, zh_cn, en, ja...
        // Nominatim 需要: zh-TW, zh-CN, en, ja...
        val owmTag = if (lang == "zh") {
            if (country.equals("TW", ignoreCase = true) || country.equals("HK", ignoreCase = true)) "zh_tw" else "zh_cn"
        } else {
            lang
        }

        val nominatimTag = if (lang == "zh") {
            if (country.equals("TW", ignoreCase = true) || country.equals("HK", ignoreCase = true)) "zh-TW" else "zh-CN"
        } else {
            lang
        }

        val newUrl = originalUrl.newBuilder().apply {
            if (originalUrl.host.contains("openweathermap.org")) {
                setQueryParameter("lang", owmTag)
            } else if (originalUrl.host.contains("openstreetmap.org")) {
                setQueryParameter("accept-language", nominatimTag)
            }
        }.build()

        return chain.proceed(originalRequest.newBuilder().url(newUrl).build())
    }
}
