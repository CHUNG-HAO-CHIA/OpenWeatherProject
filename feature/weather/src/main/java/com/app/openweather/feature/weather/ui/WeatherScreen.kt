package com.app.openweather.feature.weather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.feature.weather.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    lat: Double,
    lon: Double,
    onCityListClick: () -> Unit,
    viewModel: WeatherViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lat, lon) {
        viewModel.loadWeather(lat, lon)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentWeather?.cityName ?: "Weather") },
                actions = {
                    TextButton(onClick = onCityListClick) { Text("Cities") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> WeatherContent(
                    currentWeather = uiState.currentWeather,
                    weeklyForecast = uiState.weeklyForecast,
                )
            }
        }
    }
}

@Composable
private fun WeatherContent(
    currentWeather: CurrentWeather?,
    weeklyForecast: List<DailyForecast>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        currentWeather?.let { weather ->
            item { CurrentWeatherCard(weather) }
        }
        if (weeklyForecast.isNotEmpty()) {
            item {
                Text(
                    text = "7-Day Forecast",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(weeklyForecast) { daily ->
                DailyForecastRow(daily)
            }
        }
    }
}

@Composable
private fun CurrentWeatherCard(weather: CurrentWeather) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "${weather.temperature.toInt()}°C", style = MaterialTheme.typography.displayMedium)
            Text(text = weather.description.replaceFirstChar { it.uppercase() })
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Feels like ${weather.feelsLike.toInt()}°C")
                Text("Humidity ${weather.humidity}%")
                Text("Wind ${weather.windSpeed} m/s")
            }
        }
    }
}

@Composable
private fun DailyForecastRow(daily: DailyForecast) {
    val dayLabel = remember(daily.date) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            .format(Date(daily.date * 1000))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(dayLabel, modifier = Modifier.weight(1f))
            Text(daily.description.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
            Text("${daily.tempMin.toInt()}° / ${daily.tempMax.toInt()}°")
        }
    }
}
