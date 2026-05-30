package com.app.openweather.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.openweather.feature.city.ui.CityListScreen
import com.app.openweather.feature.weather.ui.WeatherScreen

private const val ROUTE_WEATHER = "weather"
private const val ROUTE_CITY_LIST = "city_list"

// Default city: Taipei
private const val DEFAULT_LAT = 25.0478
private const val DEFAULT_LON = 121.5318

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    var selectedLat by rememberSaveable { mutableDoubleStateOf(DEFAULT_LAT) }
    var selectedLon by rememberSaveable { mutableDoubleStateOf(DEFAULT_LON) }

    NavHost(navController = navController, startDestination = ROUTE_WEATHER) {
        composable(ROUTE_WEATHER) {
            WeatherScreen(
                lat = selectedLat,
                lon = selectedLon,
                onCityListClick = { navController.navigate(ROUTE_CITY_LIST) },
            )
        }
        composable(ROUTE_CITY_LIST) {
            CityListScreen(
                onCitySelected = { city ->
                    selectedLat = city.lat
                    selectedLon = city.lon
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
