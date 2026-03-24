package cn.syphotos.android.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.syphotos.android.ui.state.AppUiState
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.model.LatLngBounds

@Composable
fun MapScreen(
    state: AppUiState,
    onApplyMapSelection: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            map.setMapType(BaiduMap.MAP_TYPE_NORMAL)
            map.isTrafficEnabled = false
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

    LaunchedEffect(state.mapState.clusters) {
        val baiduMap = mapView.map
        baiduMap.clear()
        val clusters = state.mapState.clusters
            .filter { it.latitude != null && it.longitude != null && it.locationCode.isNotBlank() }
        if (clusters.isEmpty()) return@LaunchedEffect

        val boundsBuilder = LatLngBounds.Builder()
        clusters.forEach { cluster ->
            val point = LatLng(cluster.latitude ?: return@forEach, cluster.longitude ?: return@forEach)
            boundsBuilder.include(point)
            val markerIcon = createMarkerIcon(cluster.photoCount)
            val markerOptions = MarkerOptions()
                .position(point)
                .title("${cluster.locationCode} ${cluster.photoCount}张")
                .icon(markerIcon)
                .anchor(0.5f, 0.5f)
            val marker = baiduMap.addOverlay(markerOptions) as? Marker
            marker?.extraInfo = android.os.Bundle().apply {
                putString("locationCode", cluster.locationCode)
            }
        }
        baiduMap.setOnMarkerClickListener { marker ->
            marker.extraInfo?.getString("locationCode")?.let(onApplyMapSelection)
            true
        }
        if (clusters.size == 1) {
            val first = clusters.first()
            baiduMap.animateMapStatus(
                MapStatusUpdateFactory.newLatLngZoom(
                    LatLng(first.latitude ?: 39.9139, first.longitude ?: 116.3917),
                    6f,
                ),
            )
        } else {
            baiduMap.animateMapStatus(
                MapStatusUpdateFactory.newLatLngBounds(boundsBuilder.build()),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )
        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
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
