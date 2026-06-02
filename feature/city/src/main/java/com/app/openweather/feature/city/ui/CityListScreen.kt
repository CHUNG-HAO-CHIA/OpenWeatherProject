package com.app.openweather.feature.city.ui

import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.openweather.core.common.toUserMessage
import com.app.openweather.core.domain.model.City
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.MAX_FAVORITES
import com.app.openweather.core.ui.AppColors
import com.app.openweather.core.ui.WeatherEffectCanvas
import com.app.openweather.core.ui.WeatherIcon
import com.app.openweather.feature.city.R
import com.app.openweather.feature.city.viewmodel.CityViewModel
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityListScreen(
    onCitySelected: (City) -> Unit,
    onBackClick: () -> Unit,
    viewModel: CityViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 畫面級單次鎖定：第一個點擊生效後永久忽略後續點擊
    // remember 綁定此畫面實例，pop 後自動銷毀，下次進入自動重設
    var isProcessing by remember { mutableStateOf(false) }
    fun selectCity(city: City) {
        if (isProcessing) return
        isProcessing = true
        keyboardController?.hide()
        onCitySelected(city)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it.toUserMessage(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = AppColors.BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(AppColors.BgDark)) {
                // Back + title
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BgDark),
                    title = { Text(stringResource(R.string.title_location_management), color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.TextPrimary)
                        }
                    }
                )
                // Search bar + my location button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {

                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = { Text(stringResource(R.string.hint_search_city), color = AppColors.TextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppColors.TextSecondary) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, null, tint = AppColors.TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.AccentBlue,
                            unfocusedBorderColor = AppColors.BgCard,
                            focusedTextColor = AppColors.TextPrimary,
                            unfocusedTextColor = AppColors.TextPrimary,
                            cursorColor = AppColors.AccentBlue,
                            focusedContainerColor = AppColors.BgCard,
                            unfocusedContainerColor = AppColors.BgCard,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                HorizontalDivider(color = AppColors.BgCard)
            }
        }
    ) { padding ->

        val isSearching = uiState.query.length >= 2

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (isSearching) {
                // ── Search results ──
                when {
                    uiState.isSearching -> item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.AccentBlue, modifier = Modifier.size(28.dp))
                        }
                    }
                    uiState.searchResults.isEmpty() -> item {
                        Text(
                            stringResource(R.string.msg_no_city_found, uiState.query),
                            color = AppColors.TextSecondary,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                    else -> {
                        item { SectionLabel(stringResource(R.string.label_search_results)) }
                        itemsIndexed(uiState.searchResults, key = { idx, _ -> "search-$idx" }) { _, city ->
                            val alreadySaved = uiState.savedCities.any { it.id == city.id }
                            SearchResultRow(
                                city = city,
                                alreadySaved = alreadySaved,
                                onAdd = { viewModel.onSaveCity(city) },
                                onSelect = {
                                    viewModel.onSaveCity(city)
                                    selectCity(city.toCity())
                                },
                            )
                            HorizontalDivider(color = AppColors.BgCard, thickness = 0.5.dp)
                        }
                    }
                }
            } else {
                // ── Saved cities ──
                val favorites = uiState.favoriteCities
                val others    = uiState.otherCities

                if (favorites.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.label_favorites_count, favorites.size, MAX_FAVORITES)) }
                    items(favorites, key = { "fav-${it.id}" }) { city ->
                        SavedCityRow(
                            city = city,
                            weather = uiState.cityWeather[city.id],
                            onSelect = { selectCity(city.toCity()) },
                            onToggleStar = { viewModel.onToggleFavorite(city.id) },
                            onDelete = { viewModel.onDeleteCity(city.id) },
                        )
                    }
                }

                if (others.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.label_saved_locations)) }
                    items(others, key = { "saved-${it.id}" }) { city ->
                        SavedCityRow(
                            city = city,
                            weather = uiState.cityWeather[city.id],
                            onSelect = { selectCity(city.toCity()) },
                            onToggleStar = { viewModel.onToggleFavorite(city.id) },
                            onDelete = { viewModel.onDeleteCity(city.id) },
                        )
                    }
                }

                if (uiState.savedCities.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.msg_no_saved_locations), color = AppColors.TextSecondary, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.msg_no_saved_hint, MAX_FAVORITES),
                                color = AppColors.TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(AppColors.BgDark)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SearchResultRow(
    city: SavedCity,
    alreadySaved: Boolean,
    onAdd: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgDark)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(city.name, color = AppColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                listOfNotNull(city.state, city.country).joinToString(", "),
                color = AppColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        if (alreadySaved) {
            Text(stringResource(R.string.label_already_saved), color = AppColors.TextSecondary, fontSize = 12.sp)
        } else {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_save), tint = AppColors.AccentBlue)
            }
        }
    }
}

@Composable
private fun SavedCityRow(
    city: SavedCity,
    weather: CurrentWeather?,
    onSelect: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
) {
    val iconCode = weather?.iconCode ?: ""
    val (gradTop, gradBottom) = remember(iconCode) {
        AppColors.weatherGradient(iconCode.ifEmpty { "01d" })
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(gradTop, gradBottom)))
            .clickable(onClick = onSelect),
    ) {
        // Weather particle effect — clipped to card bounds
        if (iconCode.isNotEmpty()) {
            WeatherEffectCanvas(iconCode = iconCode)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: city name + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (city.isFavorite) {
                        Icon(Icons.Filled.Star, null, tint = AppColors.StarColor, modifier = Modifier.size(13.dp))
                    }
                    Text(
                        text = city.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(city.state, city.country).joinToString(", "),
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Right: weather info + action buttons
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Weather icon + temperature
                if (weather != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(
                            iconCode = weather.iconCode,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                        )
                        Text(
                            text = "${weather.temperature.toInt()}°",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Light,
                        )
                    }
                    Text(
                        text = weather.description.replaceFirstChar { it.uppercase() },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Star + delete actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onToggleStar, modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = if (city.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(if (city.isFavorite) R.string.cd_remove_favorite else R.string.cd_add_favorite),
                            tint = if (city.isFavorite) AppColors.StarColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(42.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun SavedCity.toCity() = City(name = name, country = country, lat = lat, lon = lon)

@Suppress("MissingPermission")
private fun resolveCurrentLocation(
    context: android.content.Context,
    onCitySelected: (City) -> Unit,
) {
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager ?: return
    val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        ?: return
    onCitySelected(City(name = "", country = "", lat = location.latitude, lon = location.longitude))
}
