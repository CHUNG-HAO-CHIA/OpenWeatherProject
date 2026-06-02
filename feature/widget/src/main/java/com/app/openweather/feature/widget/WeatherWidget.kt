package com.app.openweather.feature.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.app.openweather.core.data.local.AppDatabase
import com.app.openweather.core.common.coordKey
import com.app.openweather.core.ui.iconCodeToDrawable
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.glance.appwidget.action.actionStartActivity as glanceActionStartActivity

class WeatherWidget : GlanceAppWidget(), KoinComponent {

    private val db: AppDatabase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData()
        provideContent {
            WidgetContent(data = data, context = context)
        }
    }

    private suspend fun loadWidgetData(): WidgetData? {
        // 取第一個已儲存城市（favorites 優先）
        val firstCity = db.cityDao().getAllCities().firstOrNull()
            ?: return null
        val cityKey = coordKey(firstCity.lat, firstCity.lon)
        // 讀取 Room 快取天氣
        val weather = db.weatherDao().getCurrentWeather(cityKey) ?: return null
        return WidgetData(
            cityName = firstCity.name,
            temperature = "${weather.temperature.toInt()}°",
            description = weather.description.replaceFirstChar { it.uppercase() },
            iconCode = weather.iconCode,
        )
    }
}

data class WidgetData(
    val cityName: String,
    val temperature: String,
    val description: String,
    val iconCode: String,
)

@Composable
private fun WidgetContent(data: WidgetData?, context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp)
            .clickable(glanceActionStartActivity(launchIntent(context))),
        contentAlignment = Alignment.Center,
    ) {
        if (data == null) {
            EmptyState()
        } else {
            WeatherState(data)
        }
    }
}

@Composable
private fun WeatherState(data: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 城市名稱
        Text(
            text = data.cityName,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )

        Spacer(GlanceModifier.height(4.dp))

        // 圖示 + 溫度
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconCodeToDrawable(data.iconCode)),
                contentDescription = data.description,
                modifier = GlanceModifier.size(40.dp),
            )
            Text(
                text = data.temperature,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(GlanceModifier.height(2.dp))

        // 天氣描述
        Text(
            text = data.description,
            style = TextStyle(
                color = ColorProvider(Color(0xFFB0BEC5)),
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    Text(
        text = context.getString(R.string.widget_no_city),
        style = TextStyle(
            color = ColorProvider(Color.White),
            fontSize = 13.sp,
        ),
    )
}

private fun launchIntent(context: Context): Intent {
    return Intent().apply {
        component = ComponentName(context.packageName, "com.app.openweather.MainActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
}
