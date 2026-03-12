package cn.syphotos.android.ui.screen

import android.annotation.SuppressLint
import android.net.Uri
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
import cn.syphotos.android.BuildConfig
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun MapScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val mapUrl = remember(state.photoFilter.locationCode) {
        buildMapUrl(state.photoFilter.locationCode)
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
                    url = mapUrl,
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
    url: String,
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
                        if (target.lastPathSegment == "photolist.php") {
                            val iata = target.getQueryParameter("iatacode").orEmpty()
                            if (iata.isNotBlank()) {
                                onApplyMapSelection(iata)
                                return true
                            }
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun buildMapUrl(locationCode: String): String {
    val apiBase = BuildConfig.SY_PHOTOS_BASE_URL.trimEnd('/')
    val siteBase = apiBase.substringBefore("/api/")
    return Uri.parse("$siteBase/map.php").buildUpon().apply {
        if (locationCode.isNotBlank()) {
            appendQueryParameter("iatacode", locationCode.uppercase())
        }
    }.build().toString()
}
