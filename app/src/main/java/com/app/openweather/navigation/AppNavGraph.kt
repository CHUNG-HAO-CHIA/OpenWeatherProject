package com.app.openweather.navigation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.GetSavedCitiesUseCase
import com.app.openweather.feature.city.ui.CityListScreen
import com.app.openweather.feature.weather.ui.WeatherScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import org.koin.compose.koinInject

private const val ROUTE_WEATHER = "weather"
private const val ROUTE_CITY_LIST = "city_list"

private const val DEFAULT_LAT = 25.0478
private const val DEFAULT_LON = 121.5318

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val getSavedCities = koinInject<GetSavedCitiesUseCase>()

    var selectedLat by remember { mutableDoubleStateOf(DEFAULT_LAT) }
    var selectedLon by remember { mutableDoubleStateOf(DEFAULT_LON) }
    var locationReady by remember { mutableStateOf(false) }

    // Observe favorite cities for the bottom bar
    val savedCities by getSavedCities().collectAsState(initial = emptyList())
    val favoriteCities = remember(savedCities) { savedCities.filter { it.isFavorite } }

    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) { granted ->
        if (!granted) locationReady = true
    }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val loc = getLastLocation(context)
            if (loc != null) {
                selectedLat = loc.first
                selectedLon = loc.second
            }
            locationReady = true
        } else if (!locationReady) {
            locationPermission.launchPermissionRequest()
        }
    }

    if (!locationReady) return

    NavHost(navController = navController, startDestination = ROUTE_WEATHER) {
        composable(ROUTE_WEATHER) {
            WeatherScreen(
                lat = selectedLat,
                lon = selectedLon,
                favoriteCities = favoriteCities,
                onCityListClick = { navController.navigate(ROUTE_CITY_LIST) },
                onFavoriteCityClick = { city ->
                    selectedLat = city.lat
                    selectedLon = city.lon
                },
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

@SuppressLint("MissingPermission")
private suspend fun getLastLocation(context: Context): Pair<Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = client.lastLocation.await()
        if (loc != null) Pair(loc.latitude, loc.longitude) else null
    } catch (e: Exception) {
        null
    }
}
