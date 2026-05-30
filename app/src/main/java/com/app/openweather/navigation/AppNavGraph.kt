package com.app.openweather.navigation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.openweather.feature.city.ui.CityListScreen
import com.app.openweather.feature.map.ui.MapScreen
import com.app.openweather.feature.weather.ui.WeatherScreen
import com.app.openweather.ui.MainViewModel
import com.app.openweather.ui.settings.SettingsScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel

private const val ROUTE_WEATHER = "weather"
private const val ROUTE_CITY_LIST = "city_list"
private const val ROUTE_MAP = "map"
private const val ROUTE_SETTINGS = "settings"

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavGraph(viewModel: MainViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val navState by viewModel.navState.collectAsStateWithLifecycle()
    val favoriteCities by viewModel.favoriteCities.collectAsStateWithLifecycle()

    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) { granted ->
        if (!granted) viewModel.setLocationReady()
    }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val loc = getLastLocation(context)
            if (loc != null) viewModel.updateLocation(loc.first, loc.second)
            else viewModel.setLocationReady()
        } else if (!navState.locationReady) {
            locationPermission.launchPermissionRequest()
        }
    }

    if (!navState.locationReady) return

    NavHost(navController = navController, startDestination = ROUTE_WEATHER) {
        composable(ROUTE_WEATHER) {
            WeatherScreen(
                lat = navState.lat,
                lon = navState.lon,
                favoriteCities = favoriteCities,
                onCityListClick = { navController.navigate(ROUTE_CITY_LIST) },
                onFavoriteCityClick = { city -> viewModel.updateLocation(city.lat, city.lon) },
                onMapClick = { navController.navigate(ROUTE_MAP) },
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        composable(ROUTE_CITY_LIST) {
            CityListScreen(
                onCitySelected = { city ->
                    viewModel.updateLocation(city.lat, city.lon)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(ROUTE_MAP) {
            MapScreen(
                initialLat = navState.lat,
                initialLon = navState.lon,
                onBackClick = { navController.popBackStack() },
                onViewDetailsClick = { city ->
                    viewModel.updateLocation(city.lat, city.lon)
                    navController.navigate(ROUTE_WEATHER) {
                        popUpTo(ROUTE_WEATHER) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getLastLocation(context: Context): Pair<Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        // 嘗試獲取最後已知位置 (快但不一定有)
        var loc = client.lastLocation.await()
        
        // 如果沒有最後已知位置，嘗試獲取當前位置 (較慢但較準確)
        if (loc == null) {
            loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        }
        
        if (loc != null) Pair(loc.latitude, loc.longitude) else null
    } catch (e: Exception) {
        null
    }
}
