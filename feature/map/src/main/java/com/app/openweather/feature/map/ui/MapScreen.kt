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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.openweather.core.domain.model.City
import com.app.openweather.core.ui.AppColors
import com.app.openweather.feature.map.model.LocationPreviewUiState
import com.app.openweather.feature.map.model.MapMarkerUiModel
import com.app.openweather.feature.map.viewmodel.MapViewModel
import kotlinx.coroutines.Dispatchers
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
                                infoWindow = null // 禁用預設彈窗
                                
                                // Step 1: 立即顯示「純文字氣泡」，避免看到綠色大頭針
                                val initialBitmap = drawMarkerBitmap(context, null, markerData.tempLabel, markerData.isRainy)
                                icon = BitmapDrawable(context.resources, initialBitmap)
                                
                                setOnMarkerClickListener { _, _ ->
                                    viewModel.onMapClick(markerData.lat, markerData.lon)
                                    true
                                }
                            }
                            
                            // Step 2: 非同步加載圖示並更新
                            createMarkerIcon(context, markerData) { weatherIcon ->
                                Log.d("MapScreen", "Icon loaded for ${markerData.name}")
                                val finalBitmap = drawMarkerBitmap(context, weatherIcon, markerData.tempLabel, markerData.isRainy)
                                marker.icon = BitmapDrawable(context.resources, finalBitmap)
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
                    onViewDetails = onViewDetailsClick
                )
            }
        }
    }
}

/**
 * Creates a custom marker icon containing weather icon and temperature.
 * This is a simplified traditional Android drawing implementation.
 */
private fun createMarkerIcon(
    context: Context,
    data: MapMarkerUiModel,
    onReady: (Drawable) -> Unit
) {
    val imageLoader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(data.iconUrl)
        .allowHardware(false) // 重要：在繪製到 Canvas 時，禁用硬體加速 Bitmap 以避免一些渲染問題
        .target(
            onSuccess = { result ->
                Log.d("MapScreen", "Coil success: ${data.iconUrl}")
                onReady(result)
            },
            onError = {
                // 不再打印 result，而是看失敗原因
                Log.e("MapScreen", "Coil error for ${data.iconUrl}")
            }
        )
        .listener(
            onError = { _, result ->
                Log.e("MapScreen", "Coil detailed error: ${result.throwable.message}", result.throwable)
            }
        )
        .build()
    imageLoader.enqueue(request)
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
