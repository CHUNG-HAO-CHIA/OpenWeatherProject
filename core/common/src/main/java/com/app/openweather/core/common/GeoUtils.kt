package com.app.openweather.core.common

import java.util.Locale

/**
 * 統一管理經緯度精度
 *
 * COORD_DECIMALS = 4
 *   → 精度約 ±11 公尺，足以區分城市層級，又不會因 GPS 抖動產生多筆 cache
 */
const val COORD_DECIMALS = 4

/** 將 Double 四捨五入至統一精度，存入 DB / 作為 key 前統一呼叫 */
fun Double.roundCoord(): Double =
    String.format(Locale.US, "%.${COORD_DECIMALS}f", this).toDouble()

/** 產生統一格式的 "lat,lon" 字串，用於 DB cityKey / 城市 ID / 去重 */
fun coordKey(lat: Double, lon: Double): String =
    String.format(Locale.US, "%.${COORD_DECIMALS}f,%.${COORD_DECIMALS}f", lat, lon)
