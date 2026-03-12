package cn.syphotos.android.ui.screen

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MapScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val mapHtml = remember(state.mapState.clusters) {
        buildMapHtml(
            clusters = state.mapState.clusters,
            mapIataLabel = "IATA",
            mapPhotoCountLabel = "Photos",
            mapViewPhotosLabel = "View photos",
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(strings.mapTitle, style = MaterialTheme.typography.headlineMedium)
            Text(strings.mapSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = state.photoFilter.locationCode,
                onValueChange = { onFilterChange(state.photoFilter.copy(locationCode = it)) },
                label = { Text(strings.location) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
            ) {
                MapWebView(
                    html = mapHtml,
                    onApplyMapSelection = onApplyMapSelection,
                )
            }
        }
        state.mapState.errorMessage?.let { message ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        if (state.mapState.isLoading) {
            item {
                CircularProgressIndicator()
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(strings.mapClusters, style = MaterialTheme.typography.titleMedium)
                state.mapState.clusters.take(6).forEach { cluster ->
                    Text(
                        text = "${cluster.locationCode} • ${strings.photosCount(cluster.photoCount)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MapWebView(
    html: String,
    onApplyMapSelection: (String) -> Unit,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url ?: return false
                        if (target.scheme == "syphotos" && target.host == "photolist") {
                            val iata = target.getQueryParameter("iatacode").orEmpty()
                            if (iata.isNotBlank()) {
                                onApplyMapSelection(iata)
                                return true
                            }
                        }
                        return false
                    }
                }
                loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "utf-8", null)
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun buildMapHtml(
    clusters: List<MapCluster>,
    mapIataLabel: String,
    mapPhotoCountLabel: String,
    mapViewPhotosLabel: String,
): String {
    val itemsJson = JSONArray().apply {
        clusters.forEach { cluster ->
            if (cluster.latitude != null && cluster.longitude != null) {
                put(
                    JSONObject().apply {
                        put("iata_code", cluster.locationCode)
                        put("name", cluster.name)
                        put("photoCount", cluster.photoCount)
                        put("latitude_deg", cluster.latitude)
                        put("longitude_deg", cluster.longitude)
                    },
                )
            }
        }
    }
    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body, #map { margin: 0; padding: 0; height: 100%; width: 100%; }
            body { background: #dfeeff; font-family: sans-serif; }
            .map-pin-count {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                min-width: 28px;
                height: 28px;
                padding: 0 9px;
                border-radius: 999px;
                background: #ffffff;
                color: #d93025;
                border: 1px solid #f2b4b0;
                box-shadow: 0 3px 10px rgba(0, 0, 0, 0.16);
                font-size: 13px;
                font-weight: 700;
                line-height: 1;
                white-space: nowrap;
            }
            .map-count-marker {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                transform: translate(-50%, -50%);
            }
            .leaflet-popup-content { font-size: 15px; line-height: 1.6; }
        </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                const airportData = $itemsJson;
                const map = L.map('map', { zoomControl: true, tap: true }).setView([20, 0], 2);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18 }).addTo(map);
                airportData.forEach(item => {
                    const lng = parseFloat(item.longitude_deg);
                    const lat = parseFloat(item.latitude_deg);
                    if (isNaN(lat) || isNaN(lng)) return;
                    const icon = L.divIcon({
                        className: 'map-count-marker',
                        html: `<div class="map-pin-count">${'$'}{item.photoCount}</div>`,
                        iconSize: [36, 28],
                        iconAnchor: [18, 14],
                        popupAnchor: [0, -12]
                    });
                    const popupHtml = `
                        <div>
                            <strong>${'$'}{item.iata_code} - ${'$'}{item.name}</strong><br>
                            $mapIataLabel: ${'$'}{item.iata_code}<br>
                            $mapPhotoCountLabel: ${'$'}{item.photoCount}<br>
                            <a href="syphotos://photolist?iatacode=${'$'}{item.iata_code}" style="display:inline-block;margin-top:6px;color:#0066cc">
                                $mapViewPhotosLabel →
                            </a>
                        </div>
                    `;
                    L.marker([lat, lng], { icon }).addTo(map).bindPopup(popupHtml);
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
