@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package cn.syphotos.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.ui.components.GradientHero
import cn.syphotos.android.ui.components.MetricPill
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun AllPhotosScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
    val strings = LocalAppStrings.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                GradientHero(
                    eyebrow = strings.trendingNow,
                    title = strings.allPhotosTitle,
                    subtitle = strings.allPhotosSubtitle,
                )
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricPill(Icons.Outlined.AutoAwesome, strings.trendingNow, strings.photosCount(state.photos.count { it.liked }), Modifier.weight(1f))
                    MetricPill(Icons.Outlined.Schedule, strings.latestUploads, strings.photosCount(state.photos.size), Modifier.weight(1f))
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                PhotoFilterPanel(
                    filter = state.photoFilter,
                    onFilterChange = onFilterChange,
                )
            }
            state.feedState.errorMessage?.let { message ->
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = if (state.feedState.usingFallbackData) "$message\nShowing local fallback data." else message,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            items(state.photos, key = { it.id }) { photo ->
                PhotoCard(photo = photo, onOpenPhoto = onOpenPhoto, onToggleLike = onToggleLike)
            }
        }
        if (state.feedState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun PhotoFilterPanel(
    filter: PhotoFilter,
    onFilterChange: (PhotoFilter) -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = filter.keyword,
            onValueChange = { onFilterChange(filter.copy(keyword = it)) },
            label = { Text(strings.searchHint) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.author,
            onValueChange = { onFilterChange(filter.copy(author = it)) },
            label = { Text(strings.author) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.airline,
            onValueChange = { onFilterChange(filter.copy(airline = it)) },
            label = { Text(strings.airline) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.aircraftModel,
            onValueChange = { onFilterChange(filter.copy(aircraftModel = it)) },
            label = { Text(strings.aircraftModel) },
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                strings.camera to filter.camera,
                strings.lens to filter.lens,
                strings.registration to filter.registration,
                strings.location to filter.locationCode,
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
            label = { Text(strings.camera) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.lens,
            onValueChange = { onFilterChange(filter.copy(lens = it)) },
            label = { Text(strings.lens) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.registration,
            onValueChange = { onFilterChange(filter.copy(registration = it)) },
            label = { Text(strings.registration) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = filter.locationCode,
            onValueChange = { onFilterChange(filter.copy(locationCode = it)) },
            label = { Text(strings.location) },
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = strings.sortInfo,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun PhotoCard(
    photo: PhotoItem,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPhoto(photo.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = photo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "${photo.airline} • ${photo.aircraftModel}")
            Text(text = "${photo.location} • ${photo.createdAt}")
            Text(
                text = if (photo.liked) strings.tapToUnlike else strings.tapToLike,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onToggleLike(photo.id) },
            )
            Text(strings.openViewer, style = MaterialTheme.typography.labelLarge)
        }
    }
}
