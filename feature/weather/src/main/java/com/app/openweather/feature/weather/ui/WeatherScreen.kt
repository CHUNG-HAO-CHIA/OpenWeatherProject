package com.app.openweather.feature.weather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.app.openweather.core.ui.AppColors
import com.app.openweather.feature.weather.R
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.feature.weather.viewmodel.WeatherViewModel
import com.app.openweather.feature.weather.viewmodel.CurrentWeatherUiModel
import com.app.openweather.feature.weather.viewmodel.DailyForecastUiModel
import com.app.openweather.feature.weather.viewmodel.HourlyForecastUiModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.res.stringResource


@Composable
fun WeatherScreen(
    lat: Double,
    lon: Double,
    favoriteCities: List<SavedCity>,
    onCityListClick: () -> Unit,
    onFavoriteCityClick: (SavedCity) -> Unit,
    onMapClick: () -> Unit,
    viewModel: WeatherViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lat, lon) {
        viewModel.loadWeather(lat, lon)
    }

    Scaffold(
        containerColor = AppColors.BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.BgDark),
        ) {
            // Scrollable weather content — takes all remaining space
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading && uiState.currentWeather == null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppColors.AccentBlue,
                        )
                    }
                    uiState.error != null && uiState.currentWeather == null -> {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = AppColors.TextSecondary,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        WeatherContent(
                            currentWeather = uiState.currentWeather,
                            hourlyForecast = uiState.hourlyForecast,
                            weeklyForecast = uiState.weeklyForecast,
                        )
                    }
                }
            }

            // Fixed bottom bar — never scrolls, sits above nav bar
            FavoriteCitiesBar(
                cityName = uiState.currentWeather?.cityName ?: "—",
                favorites = favoriteCities,
                onCityClick = onFavoriteCityClick,
                onSelectClick = onCityListClick,
                onMapClick = onMapClick,
            )
        }
    }
}

@Composable
private fun WeatherContent(
    currentWeather: CurrentWeatherUiModel?,
    hourlyForecast: List<HourlyForecastUiModel>,
    weeklyForecast: List<DailyForecastUiModel>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Current weather hero
        currentWeather?.let { weather ->
            item { CurrentWeatherHero(weather) }
        }

        // Hourly chart
        if (hourlyForecast.isNotEmpty()) {
            item { HourlySection(hourlyForecast) }
        }

        // Daily forecast
        if (weeklyForecast.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.label_five_day_forecast),
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(weeklyForecast) { daily ->
                DailyForecastRow(daily)
            }
        }
    }
}


@Composable
private fun CurrentWeatherHero(weather: CurrentWeatherUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Weather icon
            AsyncImage(
                model = weather.iconUrl,
                contentDescription = weather.description,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.width(8.dp))
            // Temperature
            Column {
                Text(
                    text = weather.temperature,
                    color = AppColors.TextPrimary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 56.sp,
                )
                Text(
                    text = weather.description,
                    color = AppColors.TextSecondary,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatChip(label = stringResource(R.string.stat_feels_like), value = weather.feelsLike)
            StatChip(label = stringResource(R.string.stat_humidity), value = weather.humidity)
            StatChip(label = stringResource(R.string.stat_wind_speed), value = weather.windSpeed)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(text = label, color = AppColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun HourlySection(hourly: List<HourlyForecastUiModel>) {
    val temps = hourly.map { it.tempValue }
    val minTemp = temps.minOrNull() ?: 0.0
    val maxTemp = temps.maxOrNull() ?: 1.0
    val itemWidthDp = 72.dp
    val chartHeightDp = 80.dp
    val scrollState = rememberScrollState()

    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val chartHeightPx = with(density) { chartHeightDp.toPx() }

    // Find indices where the calendar date changes → draw a day separator
    val dayChangeIndices = remember(hourly) {
        hourly.mapIndexedNotNull { idx, item ->
            if (idx > 0 && item.isNewDay) idx else null
        }.toSet()
    }

    // Section header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.label_hourly_forecast), color = AppColors.TextSecondary, fontSize = 13.sp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgCard)
            .horizontalScroll(scrollState),
    ) {
        val totalWidth = itemWidthDp * hourly.size

        Spacer(Modifier.height(8.dp))

        // Temperature labels
        Row(modifier = Modifier.width(totalWidth)) {
            hourly.forEachIndexed { idx, item ->
                Box(
                    modifier = Modifier
                        .width(itemWidthDp)
                        .then(if (item.isNewDay && idx > 0) Modifier.background(Color.White.copy(alpha = 0.04f)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.tempLabel,
                        color = AppColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Bezier chart canvas — day separators drawn as vertical dashed lines
        Canvas(modifier = Modifier.width(totalWidth).height(chartHeightDp)) {
            if (temps.size < 2) return@Canvas
            val range = (maxTemp - minTemp).coerceAtLeast(1.0)
            val vPad = chartHeightPx * 0.15f

            fun xOf(i: Int) = i * itemWidthPx + itemWidthPx / 2f
            fun yOf(t: Double) = (vPad + (1.0 - (t - minTemp) / range) * (chartHeightPx - 2 * vPad)).toFloat()

            // Day separator lines
            for (idx in dayChangeIndices) {
                val x = xOf(idx) - itemWidthPx / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(x, 0f),
                    end = Offset(x, chartHeightPx),
                    strokeWidth = 1f,
                )
            }

            // Temperature curve
            val path = Path()
            path.moveTo(xOf(0), yOf(temps[0]))
            for (i in 1 until temps.size) {
                val x0 = xOf(i - 1); val y0 = yOf(temps[i - 1])
                val x1 = xOf(i);     val y1 = yOf(temps[i])
                val cx = (x0 + x1) / 2f
                path.cubicTo(cx, y0, cx, y1, x1, y1)
            }
            drawPath(path, color = AppColors.ChartLine, style = Stroke(width = 3f, cap = StrokeCap.Round))
            for (i in temps.indices) {
                drawCircle(color = AppColors.ChartLine, radius = 4f, center = Offset(xOf(i), yOf(temps[i])))
            }
        }

        // Weather icons
        Row(modifier = Modifier.width(totalWidth)) {
            hourly.forEach { item ->
                Box(modifier = Modifier.width(itemWidthDp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = item.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        // Time label row
        Row(modifier = Modifier.width(totalWidth)) {
            hourly.forEach { item ->
                Column(
                    modifier = Modifier.width(itemWidthDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = item.timeLabel,
                        color = if (item.isNewDay) AppColors.AccentBlue else AppColors.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (item.isNewDay) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (item.isNewDay) {
                        Text(
                            text = item.dateLabel,
                            color = AppColors.AccentBlue,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DailyForecastRow(daily: DailyForecastUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = daily.dayLabel,
            color = AppColors.TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1.2f),
        )
        AsyncImage(
            model = daily.iconUrl,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = daily.popLabel,
            color = AppColors.AccentBlue,
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp),
        )
        Spacer(Modifier.weight(0.5f))
        Text(
            text = daily.tempMinLabel,
            color = AppColors.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = daily.tempMaxLabel,
            color = AppColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
        )
    }
    HorizontalDivider(color = AppColors.BgCard, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun FavoriteCitiesBar(
    cityName: String,
    favorites: List<SavedCity>,
    onCityClick: (SavedCity) -> Unit,
    onSelectClick: () -> Unit,
    onMapClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgCard)
            .navigationBarsPadding(),     // above virtual nav buttons
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // Location row — city name + action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = cityName,
                color = AppColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Map Button
                Surface(
                    onClick = onMapClick,
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.AccentBlue.copy(alpha = 0.15f),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Map,
                            null,
                            tint = AppColors.AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // List Button
                Surface(
                    onClick = onSelectClick,
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.AccentBlue,
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Favorite city chips — only shown when there are favorites
        if (favorites.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(favorites) { city ->
                    Surface(
                        onClick = { onCityClick(city) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(Icons.Default.Star, null, tint = AppColors.StarColor, modifier = Modifier.size(12.dp))
                            Text(city.name, color = AppColors.TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
