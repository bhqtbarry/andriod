package cn.syphotos.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun MapScreen(
    state: AppUiState,
    onApplyMapSelection: (String) -> Unit,
) {
    val clusters = state.mapState.clusters
        .filter { it.latitude != null && it.longitude != null && it.locationCode.isNotBlank() }

    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFDCEEFF))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color(0xFFDCEEFF))
                val continentColor = Color(0xFF9ABF93)
                val outlineColor = Color(0xFF5E7F5D)

                fun drawBlob(points: List<Offset>) {
                    if (points.isEmpty()) return
                    val path = Path().apply {
                        moveTo(points.first().x * size.width, points.first().y * size.height)
                        points.drop(1).forEach { point ->
                            lineTo(point.x * size.width, point.y * size.height)
                        }
                        close()
                    }
                    drawPath(path, color = continentColor)
                    drawPath(path, color = outlineColor, style = Stroke(width = 2f))
                }

                drawBlob(
                    listOf(
                        Offset(0.05f, 0.20f), Offset(0.16f, 0.12f), Offset(0.22f, 0.15f), Offset(0.25f, 0.24f),
                        Offset(0.23f, 0.38f), Offset(0.18f, 0.47f), Offset(0.14f, 0.62f), Offset(0.10f, 0.72f),
                        Offset(0.06f, 0.62f), Offset(0.04f, 0.42f),
                    ),
                )
                drawBlob(
                    listOf(
                        Offset(0.28f, 0.18f), Offset(0.41f, 0.16f), Offset(0.48f, 0.21f), Offset(0.55f, 0.19f),
                        Offset(0.60f, 0.23f), Offset(0.57f, 0.32f), Offset(0.50f, 0.34f), Offset(0.44f, 0.30f),
                        Offset(0.36f, 0.33f), Offset(0.30f, 0.28f),
                    ),
                )
                drawBlob(
                    listOf(
                        Offset(0.43f, 0.36f), Offset(0.50f, 0.40f), Offset(0.52f, 0.49f), Offset(0.49f, 0.60f),
                        Offset(0.44f, 0.72f), Offset(0.39f, 0.63f), Offset(0.38f, 0.49f),
                    ),
                )
                drawBlob(
                    listOf(
                        Offset(0.58f, 0.38f), Offset(0.71f, 0.35f), Offset(0.83f, 0.39f), Offset(0.88f, 0.46f),
                        Offset(0.82f, 0.56f), Offset(0.72f, 0.58f), Offset(0.65f, 0.52f), Offset(0.60f, 0.45f),
                    ),
                )
                drawBlob(
                    listOf(
                        Offset(0.79f, 0.67f), Offset(0.86f, 0.71f), Offset(0.88f, 0.82f), Offset(0.83f, 0.90f),
                        Offset(0.77f, 0.84f), Offset(0.75f, 0.73f),
                    ),
                )
            }

            clusters.forEach { cluster ->
                val latitude = cluster.latitude ?: return@forEach
                val longitude = cluster.longitude ?: return@forEach
                val x = widthPx * (((longitude + 180.0) / 360.0).toFloat())
                val y = heightPx * (((90.0 - latitude) / 180.0).toFloat())

                Text(
                    text = "${cluster.locationCode} ${cluster.photoCount}张",
                    modifier = Modifier
                        .offset(
                            x = (x / density.density).dp,
                            y = (y / density.density).dp,
                        )
                        .background(Color(0xCC0D63C9), MaterialTheme.shapes.small)
                        .clickable { onApplyMapSelection(cluster.locationCode) },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (state.mapState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
