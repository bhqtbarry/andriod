package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.ui.i18n.LocalAppStrings

@Composable
fun PhotoViewerScreen(
    photo: PhotoItem,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var zoomStep by rememberSaveable { mutableIntStateOf(0) }
    var showUi by rememberSaveable { mutableIntStateOf(1) }
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
                if (showUi == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.viewerTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                        Text(photo.title, color = MaterialTheme.colorScheme.onPrimary)
                        Text("${photo.author} • ${photo.airline}", color = MaterialTheme.colorScheme.onPrimary)
                        Text(strings.zoomState(listOf("Fit", "1x", "2x")[zoomStep]), color = MaterialTheme.colorScheme.onPrimary)
                        Text(strings.viewerHint, color = MaterialTheme.colorScheme.onPrimary)
                        Text(strings.preloadHint, color = MaterialTheme.colorScheme.onPrimary)
                        Text(strings.shareHint, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = onBack) { Text(strings.back) }
                        Button(onClick = onToggleLike) { Text(if (photo.liked) strings.unlike else strings.like) }
                        Button(onClick = { }) { Text(strings.authorAction) }
                        Button(onClick = { }) { Text(strings.share) }
                    }
                }
            }
        }
    }
}
