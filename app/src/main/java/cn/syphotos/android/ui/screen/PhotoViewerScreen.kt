package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoItem

@Composable
fun PhotoViewerScreen(
    photo: PhotoItem,
    onBack: () -> Unit,
    onToggleLike: () -> Unit,
) {
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
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showUi == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Original viewer placeholder", style = MaterialTheme.typography.headlineSmall)
                    Text(photo.title)
                    Text("${photo.author} • ${photo.airline}")
                    Text("Zoom state: ${listOf("Fit screen", "Zoom 1", "Zoom 2")[zoomStep]}")
                    Text("Planned: preload next 5 originals, 200 MB original cache, 100 MB thumbnail cache.")
                    Text("Share output should use HTTPS web detail link.")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onBack) { Text("Back") }
                    Button(onClick = onToggleLike) { Text(if (photo.liked) "Unlike" else "Like") }
                    Button(onClick = { }) { Text("Author") }
                    Button(onClick = { }) { Text("Share") }
                }
            }
        }
    }
}
