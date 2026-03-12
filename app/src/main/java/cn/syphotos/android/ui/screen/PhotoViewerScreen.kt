package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.ViewerUiState
import coil3.compose.AsyncImage

@Composable
fun PhotoViewerScreen(
    state: ViewerUiState,
    fallbackPhotoTitle: String,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var zoomStep by rememberSaveable { mutableIntStateOf(0) }
    var showUi by rememberSaveable { mutableIntStateOf(1) }
    val photo = state.detail?.photo
    val imageUrl = state.detail?.originalUrl?.ifBlank { photo?.originalUrl }.orEmpty()
    val scale = when (zoomStep) {
        1 -> 1.5f
        2 -> 2f
        else -> 1f
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showUi = if (showUi == 1) 0 else 1
                    },
                    onDoubleTap = {
                        zoomStep = (zoomStep + 1) % 3
                    },
                )
            },
        color = MaterialTheme.colorScheme.scrim,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showUi == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.viewerTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(photo?.title ?: fallbackPhotoTitle, color = Color.White)
                            Text("${photo?.author.orEmpty()} • ${photo?.airline.orEmpty()}", color = Color.White.copy(alpha = 0.9f))
                            Text(strings.zoomState(listOf("Fit", "1.5x", "2x")[zoomStep]), color = Color.White.copy(alpha = 0.9f))
                            Text(strings.viewerHint, color = Color.White.copy(alpha = 0.85f))
                            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
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
                    if (showUi == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(state.detail?.description ?: strings.preloadHint, color = Color.White.copy(alpha = 0.9f))
                            Text(state.detail?.shareUrl ?: strings.shareHint, color = Color.White.copy(alpha = 0.72f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (showUi == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = onBack) { Text(strings.back) }
                        Button(onClick = onToggleLike) { Text(if (photo?.liked == true) strings.unlike else strings.like) }
                    }
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
