package cn.syphotos.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.MapCluster
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState
import kotlin.math.ln

@Composable
fun MapScreen(
    state: AppUiState,
    onApplyMapSelection: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val clusters = state.mapState.clusters
        .filter { it.latitude != null && it.longitude != null && it.locationCode.isNotBlank() }
        .sortedByDescending { it.photoCount }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(strings.mapTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        strings.mapSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AirportScatterMap(
                    clusters = clusters.take(80),
                    onApplyMapSelection = onApplyMapSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(260.dp),
                )
            }

            items(clusters.take(120), key = { it.id }) { cluster ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onApplyMapSelection(cluster.locationCode) },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${cluster.locationCode} · ${cluster.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            strings.photosCount(cluster.photoCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun AirportScatterMap(
    clusters: List<MapCluster>,
    onApplyMapSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF082C53), Color(0xFF0D63C9), Color(0xFF7BC4FF)),
                ),
            ),
    ) {
        val width = maxWidth
        val height = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 6f
            val stepY = size.height / 5f
            for (index in 1 until 6) {
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(stepX * index, 0f),
                    end = Offset(stepX * index, size.height),
                    strokeWidth = 1f,
                )
            }
            for (index in 1 until 5) {
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(0f, stepY * index),
                    end = Offset(size.width, stepY * index),
                    strokeWidth = 1f,
                )
            }
        }

        clusters.forEach { cluster ->
            val latitude = cluster.latitude ?: return@forEach
            val longitude = cluster.longitude ?: return@forEach
            val markerSize = markerSize(cluster.photoCount)
            val x = width * (((longitude + 180.0) / 360.0).toFloat())
            val y = height * (((90.0 - latitude) / 180.0).toFloat())
            val offsetX = x - (markerSize / 2f)
            val offsetY = y - (markerSize / 2f)

            Surface(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .clip(CircleShape)
                    .clickable { onApplyMapSelection(cluster.locationCode) },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Box(
                    modifier = Modifier
                        .background(if (cluster.photoCount > 0) Color(0xFFD93025) else Color(0xFF708090))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cluster.locationCode,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun markerSize(photoCount: Int): Dp {
    val scaled = 34f + (ln((photoCount + 1).toFloat()) * 6f)
    return scaled.dp
}
