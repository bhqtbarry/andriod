package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.ViewerUiState
import coil3.compose.AsyncImage

@Composable
fun PhotoViewerScreen(
    state: ViewerUiState,
    fallbackPhotoTitle: String,
    onToggleLike: () -> Unit,
    onApplyFilter: (PhotoFilter) -> Unit,
) {
    val strings = LocalAppStrings.current
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val photo = state.detail?.photo
    val imageUrl = state.detail?.originalUrl?.ifBlank { photo?.originalUrl }.orEmpty()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim),
        color = MaterialTheme.colorScheme.scrim,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageUrl) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            if (newScale == 1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = newScale
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                    .pointerInput(imageUrl) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2f
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = photo?.title ?: fallbackPhotoTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Image unavailable",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Button(
                onClick = { showDetails = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Text("详细信息")
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    if (showDetails && photo != null) {
        ModalBottomSheet(onDismissRequest = { showDetails = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(photo.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DetailLinkRow("作者", photo.author, onClick = null)
                DetailLinkRow("航司", photo.airline, onClick = { onApplyFilter(PhotoFilter(airline = photo.airline)) })
                DetailLinkRow("型号", photo.aircraftModel, onClick = { onApplyFilter(PhotoFilter(aircraftModel = photo.aircraftModel)) })
                DetailLinkRow("注册号", photo.registration, onClick = { onApplyFilter(PhotoFilter(registration = photo.registration)) })
                DetailLinkRow("拍摄地点", photo.location, onClick = { onApplyFilter(PhotoFilter(locationCode = photo.location)) })
                DetailLinkRow("相机", photo.camera, onClick = { onApplyFilter(PhotoFilter(camera = photo.camera)) })
                DetailLinkRow("镜头", photo.lens, onClick = { onApplyFilter(PhotoFilter(lens = photo.lens)) })
                DetailLinkRow("拍摄时间", state.detail?.shootingTime.orEmpty(), onClick = null)
                DetailLinkRow("焦距", state.detail?.focalLength?.let { "$it mm" }.orEmpty(), onClick = null)
                DetailLinkRow("ISO", state.detail?.iso.orEmpty(), onClick = null)
                DetailLinkRow("光圈", state.detail?.aperture?.let { "f/$it" }.orEmpty(), onClick = null)
                DetailLinkRow("快门", state.detail?.shutter.orEmpty(), onClick = null)
                DetailLinkRow("评分", state.detail?.score.orEmpty(), onClick = null)
                Button(onClick = onToggleLike, modifier = Modifier.fillMaxWidth()) {
                    Text(if (photo.liked) strings.unlike else strings.like)
                }
                Button(onClick = { showDetails = false }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.back)
                }
            }
        }
    }
}

@Composable
private fun DetailLinkRow(
    label: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        )
    }
}
