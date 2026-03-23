package cn.syphotos.android.ui.screen

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.state.AppUiState
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MapScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    val mapHtml = remember(state.mapState.clusters) {
        buildMapHtml(clusters = state.mapState.clusters)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapWebView(
            html = mapHtml,
            onApplyMapSelection = onApplyMapSelection,
            modifier = Modifier.fillMaxSize(),
        )
        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MapWebView(
    html: String,
    onApplyMapSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
    )
}

private fun buildMapHtml(
    clusters: List<MapCluster>,
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
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body {
                margin: 0;
                padding: 0;
                height: 100%;
                width: 100%;
                overflow: hidden;
                background: #dfe7ef;
            }
            #map {
                width: 100%;
                height: 100%;
            }
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
                text-align: center;
                white-space: nowrap;
            }
            .map-pin-count--empty {
                background: #eef3f8;
                color: #5f6b7a;
                border-color: #c9d4df;
                min-width: 16.8px;
                height: 16.8px;
                padding: 0 5.4px;
                font-size: 7.8px;
            }
            .map-count-marker {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                transform: translate(-50%, -50%);
            }
            .leaflet-container {
                background: #dfe7ef;
                font-family: sans-serif;
            }
            .leaflet-control-attribution {
                font-size: 10px;
            }
            @media (max-width: 768px) {
                .leaflet-control-zoom a {
                    font-size: 20px;
                    width: 36px;
                    height: 36px;
                    line-height: 36px;
                }
            }
        </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                const airportData = $itemsJson;
                const map = L.map('map', {
                    zoomControl: true,
                    tap: false
                }).setView([20, 0], 2);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 18
                }).addTo(map);

                const bounds = [];

                airportData.forEach(item => {
                    const lng = parseFloat(item.longitude_deg);
                    const lat = parseFloat(item.latitude_deg);
                    const photoCount = parseInt(item.photoCount, 10) || 0;
                    const hasPhotos = photoCount > 0;
                    if (isNaN(lat) || isNaN(lng)) return;

                    bounds.push([lat, lng]);

                    const icon = L.divIcon({
                        className: 'map-count-marker',
                        html: `<div class="map-pin-count${hasPhotos ? '' : ' map-pin-count--empty'}">${photoCount}</div>`,
                        iconSize: hasPhotos ? [36, 28] : [21.6, 16.8],
                        iconAnchor: hasPhotos ? [18, 14] : [10.8, 8.4]
                    });

                    const marker = L.marker([lat, lng], { icon }).addTo(map);
                    marker.on('click', () => {
                        window.location.href = `syphotos://photolist?iatacode=${encodeURIComponent(item.iata_code)}`;
                    });
                });

                if (bounds.length > 1) {
                    map.fitBounds(bounds, { padding: [24, 24] });
                } else if (bounds.length === 1) {
                    map.setView(bounds[0], 6);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
