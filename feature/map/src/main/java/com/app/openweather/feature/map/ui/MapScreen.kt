package com.app.openweather.feature.map.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.app.openweather.core.domain.model.City
import com.app.openweather.core.ui.AppColors
import com.app.openweather.feature.map.model.LocationPreviewUiState
import com.app.openweather.feature.map.model.MapMarkerUiModel
import com.app.openweather.feature.map.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    initialLat: Double,
    initialLon: Double,
    onBackClick: () -> Unit,
    onViewDetailsClick: (City) -> Unit,
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Configure osmdroid user agent
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BgDark),
                title = { Text("天氣地圖 (OSM)", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.TextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 1.0   // 最小縮放：可看見整顆地球
                        maxZoomLevel = 12.0  // 最大縮放：城市層級，不放大到街道
                        controller.setZoom(8.5)
                        controller.setCenter(GeoPoint(initialLat, initialLon))
                        
                        // Handle Map Clicks
                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                viewModel.onMapClick(p.latitude, p.longitude)
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        })
                        overlays.add(eventsOverlay)
                    }
                },
                update = { mapView ->
                    val currentMarkers = mapView.overlays.filterIsInstance<Marker>()
                    
                    if (currentMarkers.size != uiState.markers.size) {
                        Log.d("MapScreen", "Updating markers: ${uiState.markers.size}")
                        mapView.overlays.removeAll { it is Marker }
                        
                        uiState.markers.forEach { markerData ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(markerData.lat, markerData.lon)
                                title = markerData.name
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                infoWindow = null

                                setOnMarkerClickListener { _, _ ->
                                    viewModel.onMapClick(markerData.lat, markerData.lon)
                                    true
                                }
                            }

                            // 全程在 Default dispatcher 上繪製 Bitmap，完成後回 Main 更新 icon
                            createMarkerIcon(context, scope, markerData) { drawable ->
                                marker.icon = drawable
                                mapView.invalidate()
                            }

                            mapView.overlays.add(marker)
                        }
                        mapView.invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.locationPreview !is LocationPreviewUiState.Idle) {
                WeatherPreviewBottomSheet(
                    state = uiState.locationPreview,
                    onDismiss = viewModel::dismissPreview,
                    onViewDetails = { city ->
                        viewModel.saveSelectedCity()
                        onViewDetailsClick(city)
                    }
                )
            }
        }
    }
}

/**
 * Process-level LruCache for marker Bitmaps.
 * Key: "<cityId>_<tempLabel>_<isRainy>" — avoids recreating identical bitmaps.
 * Size: 1 MB (typical marker bitmap ~4 KB → holds ~256 markers comfortably).
 */
private val markerBitmapCache = LruCache<String, Bitmap>(1 * 1024 * 1024) // 1 MB

/**
 * Loads the weather icon via Coil (singleton imageLoader) then draws the
 * marker bitmap entirely on Dispatchers.Default, finally calls [onReady]
 * back on the Main thread for safe OSMDroid update.
 */
private fun createMarkerIcon(
    context: Context,
    scope: CoroutineScope,
    data: MapMarkerUiModel,
    onReady: (Drawable) -> Unit,
) {
    val cacheKey = "${data.cityId}_${data.tempLabel}_${data.isRainy}"

    scope.launch(Dispatchers.Default) {
        // Check bitmap cache first (no Coil round-trip needed)
        val cachedBitmap = markerBitmapCache.get(cacheKey)
        if (cachedBitmap != null) {
            val drawable = withContext(Dispatchers.Main) {
                BitmapDrawable(context.resources, cachedBitmap)
            }
            onReady(drawable)
            return@launch
        }

        // Load weather icon via Coil singleton (no new ImageLoader allocation)
        val iconDrawable: Drawable? = try {
            val request = ImageRequest.Builder(context)
                .data(data.iconUrl)
                .allowHardware(false)
                .build()
            context.imageLoader.execute(request).drawable
        } catch (e: Exception) {
            Log.e("MapScreen", "Coil error for ${data.iconUrl}: ${e.message}")
            null
        }

        // Draw bitmap off Main thread
        val bitmap = drawMarkerBitmap(context, iconDrawable, data.tempLabel, data.isRainy)
        markerBitmapCache.put(cacheKey, bitmap)

        withContext(Dispatchers.Main) {
            onReady(BitmapDrawable(context.resources, bitmap))
        }
    }
}

private fun drawMarkerBitmap(
    context: Context,
    icon: Drawable?,
    temp: String,
    isRainy: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val padding = (6 * density).toInt()
    val iconSize = (28 * density).toInt()
    val fontSize = 14 * density
    val pointerHeight = (6 * density).toInt()
    
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = fontSize
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    
    val textBounds = Rect()
    textPaint.getTextBounds(temp, 0, temp.length, textBounds)
    
    // Calculate total width (reduce if no icon)
    val contentWidth = if (icon != null) (iconSize + textBounds.width() + (padding * 3)) 
                       else (textBounds.width() + (padding * 4))
    
    val bodyHeight = iconSize + (padding * 2)
    val totalHeight = bodyHeight + pointerHeight
    
    val bitmap = Bitmap.createBitmap(contentWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Paints
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isRainy) 0xFF4A6572.toInt() else 0xFF2196F3.toInt()
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
    }
    
    // 1. Draw the "Pin" pointer
    val path = android.graphics.Path().apply {
        moveTo(contentWidth / 2f - (8 * density), bodyHeight.toFloat() - 2)
        lineTo(contentWidth / 2f, totalHeight.toFloat())
        lineTo(contentWidth / 2f + (8 * density), bodyHeight.toFloat() - 2)
        close()
    }
    canvas.drawPath(path, bgPaint)
    canvas.drawPath(path, borderPaint)
    
    // 2. Draw main pill
    val rect = RectF(0f, 0f, contentWidth.toFloat(), bodyHeight.toFloat())
    canvas.drawRoundRect(rect, bodyHeight / 2f, bodyHeight / 2f, bgPaint)
    canvas.drawRoundRect(rect, bodyHeight / 2f, bodyHeight / 2f, borderPaint)
    
    // 3. Draw weather icon if present
    if (icon != null) {
        val iconTop = (bodyHeight - iconSize) / 2
        icon.setBounds(padding, iconTop, padding + iconSize, iconTop + iconSize)
        icon.draw(canvas)
    }
    
    // 4. Draw temperature text
    val textX = if (icon != null) (padding * 1.5f + iconSize).toFloat() else (padding * 2f).toFloat()
    val textY = (bodyHeight / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(temp, textX, textY, textPaint)
    
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherPreviewBottomSheet(
    state: LocationPreviewUiState,
    onDismiss: () -> Unit,
    onViewDetails: (City) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.BgCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AppColors.TextSecondary.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is LocationPreviewUiState.Loading -> {
                    CircularProgressIndicator(color = AppColors.AccentBlue)
                    Spacer(Modifier.height(16.dp))
                    Text("載入位置天氣中...", color = AppColors.TextSecondary)
                }
                is LocationPreviewUiState.Success -> {
                    Text(
                        text = state.city.name,
                        color = AppColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = listOfNotNull(state.city.state, state.city.country).joinToString(", "),
                        color = AppColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = state.iconUrl, contentDescription = null, modifier = Modifier.size(64.dp))
                        Column {
                            Text(text = state.tempLabel, color = AppColors.TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Light)
                            Text(text = state.description, color = AppColors.TextSecondary, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onViewDetails(City(state.city.name, state.city.country, state.city.lat, state.city.lon))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("查看完整預報", color = Color.White)
                    }
                }
                is LocationPreviewUiState.Error -> {
                    Text(state.message, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss) { Text("關閉") }
                }
                else -> {}
            }
        }
    }
}
