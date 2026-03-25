package cn.syphotos.android.ui.screen

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NearMe
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.ui.state.AppUiState
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng

@Composable
fun MapScreen(
    state: AppUiState,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onFilterChange: (PhotoFilter) -> Unit,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val updatedApplyMapSelection by rememberUpdatedState(onApplyMapSelection)
    val mapView = remember {
        MapView(context).apply {
            map.setMapType(BaiduMap.MAP_TYPE_NORMAL)
            map.isTrafficEnabled = false
            map.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(CHINA_CENTER, CHINA_DEFAULT_ZOOM))
        }
    }
    var selectedCluster by remember { mutableStateOf<cn.syphotos.android.model.MapCluster?>(null) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
        }
    }

    DisposableEffect(mapView, hasLocationPermission) {
        val baiduMap = mapView.map
        baiduMap.isMyLocationEnabled = hasLocationPermission
        if (!hasLocationPermission) {
            currentLocation = null
            return@DisposableEffect onDispose { baiduMap.isMyLocationEnabled = false }
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        var listener: LocationListener? = null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { provider -> runCatching { locationManager?.isProviderEnabled(provider) == true }.getOrDefault(false) }

        providers.asSequence()
            .mapNotNull { provider -> runCatching { locationManager?.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { location ->
                currentLocation = LatLng(location.latitude, location.longitude)
            }

        val chosenProvider = providers.firstOrNull()
        if (locationManager != null && chosenProvider != null) {
            listener = LocationListener { location ->
                currentLocation = LatLng(location.latitude, location.longitude)
            }
            @Suppress("MissingPermission")
            locationManager.requestLocationUpdates(chosenProvider, 3_000L, 10f, listener!!)
        }

        onDispose {
            if (locationManager != null && listener != null) {
                locationManager.removeUpdates(listener!!)
            }
            baiduMap.isMyLocationEnabled = false
        }
    }

    LaunchedEffect(currentLocation, hasLocationPermission) {
        if (hasLocationPermission && currentLocation != null) {
            mapView.map.setMyLocationData(
                MyLocationData.Builder()
                    .latitude(currentLocation!!.latitude)
                    .longitude(currentLocation!!.longitude)
                    .build(),
            )
        }
    }

    LaunchedEffect(state.mapState.clusters) {
        val baiduMap = mapView.map
        baiduMap.clear()
        val clusters = state.mapState.clusters
            .filter { it.latitude != null && it.longitude != null && it.locationCode.isNotBlank() }
        selectedCluster = selectedCluster?.let { selected -> clusters.firstOrNull { it.locationCode == selected.locationCode } }
        clusters
            .sortedWith(
                compareBy<cn.syphotos.android.model.MapCluster> { it.photoCount > 0 }
                    .thenBy { it.photoCount },
            )
            .forEach { cluster ->
            val point = LatLng(cluster.latitude ?: return@forEach, cluster.longitude ?: return@forEach)
            val markerIcon = createMarkerIcon(cluster.photoCount)
            val markerOptions = MarkerOptions()
                .position(point)
                .title(cluster.airportName.ifBlank { cluster.name })
                .icon(markerIcon)
                .anchor(0.5f, 0.5f)
            val marker = baiduMap.addOverlay(markerOptions) as? Marker
            marker?.extraInfo = android.os.Bundle().apply {
                putString("locationCode", cluster.locationCode)
            }
        }
        baiduMap.setOnMarkerClickListener { marker ->
            val locationCode = marker.extraInfo?.getString("locationCode").orEmpty()
            selectedCluster = clusters.firstOrNull { it.locationCode == locationCode }
            selectedCluster?.latitude?.let { lat ->
                selectedCluster?.longitude?.let { lng ->
                    baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(LatLng(lat, lng)))
                }
            }
            true
        }
        baiduMap.setOnMapClickListener(
            object : BaiduMap.OnMapClickListener {
                override fun onMapClick(point: LatLng?) {
                    selectedCluster = null
                }

                override fun onMapPoiClick(poi: com.baidu.mapapi.map.MapPoi?) {
                    selectedCluster = null
                }
            },
        )
        if (clusters.isEmpty()) {
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(CHINA_CENTER, CHINA_DEFAULT_ZOOM))
        } else if (clusters.size == 1) {
            val first = clusters.first()
            baiduMap.animateMapStatus(
                MapStatusUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude ?: 39.9139, first.longitude ?: 116.3917),
                    6f,
                ),
            )
        } else if (state.photoFilter.locationCode.isBlank()) {
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(CHINA_CENTER, CHINA_DEFAULT_ZOOM))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
        if (showFilters) {
            PhotoFilterPanel(
                filter = state.photoFilter,
                suggestionsByField = suggestionsByField,
                onFilterChange = onFilterChange,
                onRequestSuggestions = onRequestSuggestions,
                onClearSuggestions = onClearSuggestions,
                onClearAll = {
                    onFilterChange(PhotoFilter())
                    showFilters = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 80.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingActionButton(onClick = { showFilters = !showFilters }) {
                Icon(Icons.Outlined.FilterAlt, contentDescription = "Filters")
            }
            FloatingActionButton(
                onClick = {
                    val point = currentLocation
                    if (point != null) {
                        mapView.map.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(point, 12f))
                    } else if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
            ) {
                Icon(Icons.AutoMirrored.Outlined.NearMe, contentDescription = "定位")
            }
        }
        state.mapState.errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (showFilters) 250.dp else 16.dp, start = 16.dp, end = 88.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        selectedCluster?.let { cluster ->
            MapInfoCard(
                cluster = cluster,
                onViewPhotos = { updatedApplyMapSelection(cluster.locationCode) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
            )
        }
        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MapInfoCard(
    cluster: cn.syphotos.android.model.MapCluster,
    onViewPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = cluster.airportName.ifBlank { cluster.name.ifBlank { cluster.locationCode } },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = listOfNotNull(
                    cluster.locationCode.takeIf { it.isNotBlank() }?.let { "IATA: $it" },
                    cluster.icaoCode.takeIf { it.isNotBlank() }?.let { "ICAO: $it" },
                ).joinToString("   "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(cluster.province, cluster.city)
                .filter { it.isNotBlank() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" ")
                ?.let { locationText ->
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            Text(
                text = if (cluster.photoCount > 0) "照片数：${cluster.photoCount}" else "这个机场还没有照片",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onViewPhotos,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (cluster.photoCount > 0) "查看照片" else "去拍第一张")
            }
        }
    }
}

private val CHINA_CENTER = LatLng(35.8617, 104.1954)
private const val CHINA_DEFAULT_ZOOM = 4.5f

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun createMarkerIcon(photoCount: Int) = BitmapDescriptorFactory.fromBitmap(
    buildCountBitmap(photoCount = photoCount.coerceAtLeast(0)),
)

private fun buildCountBitmap(photoCount: Int): Bitmap {
    val hasPhotos = photoCount > 0
    val text = photoCount.toString()
    val density = 3f
    val horizontalPadding = if (hasPhotos) 10f * density else 6f * density
    val verticalPadding = if (hasPhotos) 4f * density else 2.4f * density
    val minWidth = if (hasPhotos) 28f * density else 16.8f * density
    val height = if (hasPhotos) 28f * density else 16.8f * density
    val textSize = if (hasPhotos) 13f * density else 7.8f * density
    val cornerRadius = height / 2f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(if (hasPhotos) "#d93025" else "#5f6b7a")
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(if (hasPhotos) "#ffffff" else "#eef3f8")
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(if (hasPhotos) "#f2b4b0" else "#c9d4df")
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 0, 0)
        style = Paint.Style.FILL
    }

    val textWidth = textPaint.measureText(text)
    val width = maxOf(minWidth, textWidth + horizontalPadding * 2)
    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val shadowRect = RectF(
        density,
        density * 2,
        width - density,
        height,
    )
    val rect = RectF(
        density * 0.5f,
        density * 0.5f,
        width - density * 0.5f,
        height - density * 1.5f,
    )

    canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, rect.centerX(), baseline, textPaint)
    return bitmap
}
