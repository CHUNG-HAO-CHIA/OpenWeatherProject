package com.app.openweather.feature.weather.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.ui.AppColors
import com.app.openweather.core.ui.WeatherEffectCanvas
import com.app.openweather.feature.weather.R
import com.app.openweather.feature.weather.viewmodel.CurrentWeatherUiModel
import com.app.openweather.feature.weather.viewmodel.DailyForecastUiModel
import com.app.openweather.feature.weather.viewmodel.HourlyForecastUiModel
import com.app.openweather.feature.weather.viewmodel.WeatherViewModel
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    lat: Double,
    lon: Double,
    favoriteCities: List<SavedCity>,
    onCityListClick: () -> Unit,
    onFavoriteCityClick: (SavedCity) -> Unit,
    onMapClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: WeatherViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lat, lon,) {
        viewModel.loadWeather(lat, lon)
    }

    val iconCode = uiState.currentWeather?.iconCode ?: ""
    val (targetTop, targetBottom) = remember(iconCode) { AppColors.weatherGradient(iconCode) }
    val animTop by animateColorAsState(targetTop, animationSpec = tween(1200), label = "bgTop")
    val animBottom by animateColorAsState(targetBottom, animationSpec = tween(1200), label = "bgBottom")
    val bgBrush = Brush.verticalGradient(listOf(animTop, animBottom))

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = bgBrush),
        ) {
            // Weather particle effect layer
            if (iconCode.isNotEmpty()) {
                WeatherEffectCanvas(iconCode = iconCode)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                                pop = uiState.pop,
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
            } // end Column
        } // end outer Box
    }
}

@Composable
private fun WeatherContent(
    currentWeather: CurrentWeatherUiModel?,
    hourlyForecast: List<HourlyForecastUiModel>,
    weeklyForecast: List<DailyForecastUiModel>,
    pop: String,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Current weather hero
        currentWeather?.let { weather ->
            item { CurrentWeatherHero(weather, pop) }
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
private fun CurrentWeatherHero(weather: CurrentWeatherUiModel, pop: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Temperature + icon row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = weather.iconUrl,
                contentDescription = weather.description,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.width(4.dp))
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

        Spacer(Modifier.height(16.dp))

        // 2 × 3 info grid
        val cardBg = Color.White.copy(alpha = 0.12f)
        val cardBorder = Color.White.copy(alpha = 0.18f)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Thermostat,
                    label = stringResource(R.string.stat_feels_like),
                    value = weather.feelsLike,
                    bg = cardBg, border = cardBorder,
                )
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.WaterDrop,
                    label = stringResource(R.string.stat_humidity),
                    value = weather.humidity,
                    bg = cardBg, border = cardBorder,
                )
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Air,
                    label = stringResource(R.string.stat_wind_speed),
                    value = weather.windSpeed,
                    bg = cardBg, border = cardBorder,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Cloud,
                    label = stringResource(R.string.stat_pop),
                    value = pop,
                    bg = cardBg, border = cardBorder,
                )
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.WbSunny,
                    label = stringResource(R.string.stat_sunrise),
                    value = weather.sunrise,
                    bg = cardBg, border = cardBorder,
                )
                WeatherInfoCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.WbSunny,
                    label = stringResource(R.string.stat_sunset),
                    value = weather.sunset,
                    bg = cardBg, border = cardBorder,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun WeatherInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    bg: Color,
    border: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    color = AppColors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = AppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HourlySection(hourly: List<HourlyForecastUiModel>) {
    val scrollState = rememberScrollState()
    val nowLabel = stringResource(R.string.label_now)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.label_hourly_forecast),
                    color = AppColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 0.5.dp)

            // Scrollable hourly columns
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                hourly.forEachIndexed { index, item ->
                    HourlyColumn(
                        item = item,
                        timeLabel = if (item.isFirst) nowLabel else item.timeLabel,
                        showDayLabel = item.isNewDay && !item.isFirst,
                    )
                    // Thin separator between days
                    if (item.isNewDay && !item.isFirst) {
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(80.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyColumn(
    item: HourlyForecastUiModel,
    timeLabel: String,
    showDayLabel: Boolean,
) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Date badge for new day
        if (showDayLabel) {
            Text(
                text = item.dateLabel,
                color = AppColors.AccentBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Spacer(Modifier.height(13.dp))   // same height as date text so rows align
        }

        // Time label
        Text(
            text = timeLabel,
            color = if (item.isFirst) AppColors.TextPrimary else AppColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (item.isFirst) FontWeight.SemiBold else FontWeight.Normal,
        )

        // Weather icon — white circle backing so night icons stay visible on dark bg
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = item.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
            )
        }

        // Temperature
        Text(
            text = item.tempLabel,
            color = AppColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )

        // Precipitation probability
        Text(
            text = item.popLabel,
            color = AppColors.AccentBlue,
            fontSize = 12.sp,
        )
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = daily.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
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
    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
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
            .background(Color.Black.copy(alpha = 0.30f))
            .navigationBarsPadding(),
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
