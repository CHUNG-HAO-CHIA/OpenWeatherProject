package com.app.openweather.core.network.interceptor

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import okhttp3.Interceptor
import okhttp3.Response

class LanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        // 獲取目前的語系設定（優先取 App 設定，若無則取系統設定）
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val primaryLocale = if (!appLocales.isEmpty) {
            appLocales.get(0)
        } else {
            // 使用 getAdjustedDefault() 獲取經過系統調整後的預設語系
            LocaleListCompat.getAdjustedDefault().get(0)
        }

        val lang = primaryLocale?.language ?: "en"
        val country = primaryLocale?.country ?: ""

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
