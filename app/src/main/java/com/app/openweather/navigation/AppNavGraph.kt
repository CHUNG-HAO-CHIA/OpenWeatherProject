package com.app.openweather.navigation

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.openweather.BuildConfig
import com.app.openweather.feature.city.ui.CityListScreen
import com.app.openweather.feature.map.ui.MapScreen
import com.app.openweather.feature.weather.ui.WeatherScreen
import com.app.openweather.feature.widget.WeatherWidget
import com.app.openweather.ui.MainViewModel
import com.app.openweather.ui.settings.SettingsScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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

    // 若已有位置權限，靜默更新到實際座標；否則維持預設台北
    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val loc = getLastLocation(context)
            if (loc != null) viewModel.updateLocation(loc.first, loc.second)
        }
    }

    fun onMyLocationClick() {
        if (locationPermission.status.isGranted) {
            coroutineScope.launch {
                val loc = getLastLocation(context)
                if (loc != null) viewModel.updateLocation(loc.first, loc.second)
            }
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_WEATHER) {
        composable(ROUTE_WEATHER) {
            WeatherScreen(
                lat = navState.lat,
                lon = navState.lon,
                favoriteCities = favoriteCities,
                onCityListClick = ({ navController.navigate(ROUTE_CITY_LIST) })
                    .takeIf { BuildConfig.FEATURE_CITY_ENABLED },
                onFavoriteCityClick = { city ->
                    coroutineScope.launch {
                        WeatherWidget().updateAll(context)
                    }
                    viewModel.updateLocation(city.lat, city.lon, city.name) },
                onMapClick = ({ navController.navigate(ROUTE_MAP) })
                    .takeIf { BuildConfig.FEATURE_MAP_ENABLED },
                onMyLocationClick = ::onMyLocationClick,
                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
            )
        }
        if (BuildConfig.FEATURE_CITY_ENABLED) {
            composable(ROUTE_CITY_LIST) {
                CityListScreen(
                    onCitySelected = { city ->
                        viewModel.updateLocation(city.lat, city.lon)
                        coroutineScope.launch {
                            WeatherWidget().updateAll(context)
                        }
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }
        }
        if (BuildConfig.FEATURE_MAP_ENABLED) {
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
        var loc = withTimeoutOrNull(2000L) {
            client.lastLocation.await()
        }
        if (loc == null) {
            loc = withTimeoutOrNull(5000L) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            }
        }
        if (loc != null) Pair(loc.latitude, loc.longitude) else null
    } catch (e: Exception) {
        null
    }
}
