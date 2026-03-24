package cn.syphotos.android.ui.screen

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
import com.baidu.mapapi.map.BitmapDescriptor
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
    val markerIcon: BitmapDescriptor = remember {
        BitmapDescriptorFactory.fromAsset("Icon_mark.png")
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
            val marker = baiduMap.addOverlay(
                MarkerOptions()
                    .position(point)
                    .title("${cluster.locationCode} ${cluster.photoCount}张")
                    .icon(markerIcon),
            ) as? Marker
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
