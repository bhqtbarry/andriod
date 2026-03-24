package cn.syphotos.android.ui.screen

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
import cn.syphotos.android.ui.state.AppUiState
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    state: AppUiState,
    onApplyMapSelection: (String) -> Unit,
) {
    val clusters = state.mapState.clusters
        .filter { it.latitude != null && it.longitude != null && it.locationCode.isNotBlank() }

    val payload = remember(clusters) {
        JSONArray().apply {
            clusters.forEach { cluster ->
                put(
                    JSONObject().apply {
                        put("code", cluster.locationCode)
                        put("name", cluster.name)
                        put("count", cluster.photoCount)
                        put("lat", cluster.latitude)
                        put("lng", cluster.longitude)
                    },
                )
            }
        }.toString()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun selectAirport(code: String) {
                                post { onApplyMapSelection(code) }
                            }
                        },
                        "AndroidMap",
                    )
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://www.syphotos.cn/",
                    buildLeafletHtml(payload),
                    "text/html",
                    "utf-8",
                    null,
                )
            },
        )

        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun buildLeafletHtml(payload: String): String = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <style>
    html, body, #map { height: 100%; margin: 0; padding: 0; background: #d9ecff; }
    .leaflet-container { font-family: sans-serif; }
    .airport-badge {
      background: rgba(13, 99, 201, 0.95);
      color: #fff;
      border: 0;
      border-radius: 999px;
      padding: 6px 10px;
      font-size: 12px;
      font-weight: 700;
      box-shadow: 0 8px 24px rgba(0,0,0,0.18);
      white-space: nowrap;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <script>
    const points = $payload;
    const map = L.map('map', { zoomControl: true }).setView([22, 110], 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    const bounds = [];
    points.forEach(point => {
      if (point.lat == null || point.lng == null) return;
      bounds.push([point.lat, point.lng]);
      const icon = L.divIcon({
        className: '',
        html: `<div class="airport-badge">${'$'}{point.code} · ${'$'}{point.count}</div>`,
        iconSize: [90, 28],
        iconAnchor: [45, 14]
      });
      const marker = L.marker([point.lat, point.lng], { icon }).addTo(map);
      marker.bindPopup(`<strong>${'$'}{point.code}</strong><br/>${'$'}{point.name}<br/>${'$'}{point.count} photos`);
      marker.on('click', () => {
        if (window.AndroidMap && window.AndroidMap.selectAirport) {
          window.AndroidMap.selectAirport(point.code);
        }
      });
    });

    if (bounds.length > 0) {
      map.fitBounds(bounds, { padding: [32, 32] });
    }
  </script>
</body>
</html>
""".trimIndent()
