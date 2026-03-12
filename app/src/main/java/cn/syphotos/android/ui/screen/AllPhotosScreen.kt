package cn.syphotos.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.ui.state.AppUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllPhotosScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PhotoFilterPanel(
            filter = state.photoFilter,
            onFilterChange = onFilterChange,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.photos, key = { it.id }) { photo ->
                PhotoCard(photo = photo, onOpenPhoto = onOpenPhoto, onToggleLike = onToggleLike)
            }
        }
    }
}

@Composable
private fun PhotoFilterPanel(
    filter: PhotoFilter,
    onFilterChange: (PhotoFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = filter.keyword,
            onValueChange = { onFilterChange(filter.copy(keyword = it)) },
            label = { Text("Keyword / IATA / title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.author,
            onValueChange = { onFilterChange(filter.copy(author = it)) },
            label = { Text("Author") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.airline,
            onValueChange = { onFilterChange(filter.copy(airline = it)) },
            label = { Text("Airline") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.aircraftModel,
            onValueChange = { onFilterChange(filter.copy(aircraftModel = it)) },
            label = { Text("Aircraft model") },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Camera" to filter.camera,
                "Lens" to filter.lens,
                "Registration" to filter.registration,
                "Location" to filter.locationCode,
            ).forEach { (label, value) ->
                AssistChip(
                    onClick = { },
                    label = { Text(if (value.isBlank()) label else "$label: $value") },
                )
            }
        }
        OutlinedTextField(
            value = filter.camera,
            onValueChange = { onFilterChange(filter.copy(camera = it)) },
            label = { Text("Camera") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.lens,
            onValueChange = { onFilterChange(filter.copy(lens = it)) },
            label = { Text("Lens") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.registration,
            onValueChange = { onFilterChange(filter.copy(registration = it)) },
            label = { Text("Registration number") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.locationCode,
            onValueChange = { onFilterChange(filter.copy(locationCode = it)) },
            label = { Text("Location / IATA") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Sort: score DESC, created_at DESC",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PhotoCard(
    photo: PhotoItem,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPhoto(photo.id) },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = photo.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "${photo.airline} • ${photo.aircraftModel}")
            Text(text = "${photo.location} • ${photo.createdAt}")
            Text(
                text = if (photo.liked) "Liked, tap to unlike" else "Tap to like before viewer",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onToggleLike(photo.id) },
            )
        }
    }
}
