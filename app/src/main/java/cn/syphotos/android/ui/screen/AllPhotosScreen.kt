@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package cn.syphotos.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState
import coil3.compose.AsyncImage

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
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFDFeEFF)),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PhotoFilterPanel(
                    filter = state.photoFilter,
                    onFilterChange = onFilterChange,
                    photoCount = state.photos.size,
                )
            }
            state.feedState.errorMessage?.let { message ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.padding(vertical = 8.dp),
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
    photoCount: Int,
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(strings.allPhotosTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(strings.photosCount(photoCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = filter.keyword,
                    onValueChange = { onFilterChange(filter.copy(keyword = it)) },
                    label = { Text(strings.searchHint) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = filter.author,
                        onValueChange = { onFilterChange(filter.copy(author = it)) },
                        label = { Text(strings.author) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = filter.locationCode,
                        onValueChange = { onFilterChange(filter.copy(locationCode = it)) },
                        label = { Text(strings.location) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = filter.airline,
                        onValueChange = { onFilterChange(filter.copy(airline = it)) },
                        label = { Text(strings.airline) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = filter.aircraftModel,
                        onValueChange = { onFilterChange(filter.copy(aircraftModel = it)) },
                        label = { Text(strings.aircraftModel) },
                        modifier = Modifier.weight(1f),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        strings.camera to filter.camera,
                        strings.lens to filter.lens,
                        strings.registration to filter.registration,
                    ).forEach { (label, value) ->
                        AssistChip(
                            onClick = { },
                            label = { Text(if (value.isBlank()) label else "$label: $value") },
                        )
                    }
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFDFeEFF))
            .clickable { onOpenPhoto(photo.id) },
    ) {
        if (photo.thumbUrl.isNotBlank()) {
            AsyncImage(
                model = photo.thumbUrl,
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(photo.title, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = photo.registration.ifBlank { photo.title },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = "${photo.location} • ${photo.author}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
            )
            Text(
                text = if (photo.liked) strings.tapToUnlike else strings.tapToLike,
                color = Color.White,
                modifier = Modifier.clickable { onToggleLike(photo.id) },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
