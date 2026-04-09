package cn.syphotos.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.model.asGalleryPhotoSource
import cn.syphotos.android.ui.gallery.PhotoGridRecyclerView
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun AllPhotosScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onLoadMore: () -> Unit,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val galleryPhotos = remember(state.photos, state.viewerState.photosById) {
        state.photos.map { photo ->
            photo.asGalleryPhotoSource(state.viewerState.photosById[photo.id])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B)),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> PhotoGridRecyclerView(context) },
            update = { view ->
                view.bind(
                    photos = galleryPhotos,
                    hasMore = state.feedState.hasMore,
                    isLoading = state.feedState.isLoading,
                    isLoadingMore = state.feedState.isLoadingMore,
                    onLoadMore = onLoadMore,
                    onPhotoClick = { photo -> onOpenPhoto(photo.id) },
                )
            },
        )

        state.feedState.errorMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        if (showFilters) {
            PhotoFilterPanel(
                filter = state.photoFilter,
                suggestionsByField = suggestionsByField,
                onFilterChange = onFilterChange,
                onRequestSuggestions = onRequestSuggestions,
                onClearSuggestions = onClearSuggestions,
                onApply = { showFilters = false },
                onClearAll = {
                    onFilterChange(PhotoFilter())
                    showFilters = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
            )
        }

        FloatingActionButton(
            onClick = { showFilters = !showFilters },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
        ) {
            Icon(Icons.Outlined.FilterAlt, contentDescription = "Filters")
        }

        if (state.feedState.isLoading && state.photos.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
