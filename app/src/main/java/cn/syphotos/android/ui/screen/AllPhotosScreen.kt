@file:OptIn(ExperimentalFoundationApi::class)

package cn.syphotos.android.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

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
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(gridState, state.feedState, state.photos.size) {
        derivedStateOf { derivedLoadMore(gridState, state) }
    }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = if (showFilters) 220.dp else 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFDFeEFF)),
        ) {
            state.feedState.errorMessage?.let { message ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            items(state.photos, key = { it.id }) { photo ->
                PhotoCard(photo = photo, onOpenPhoto = onOpenPhoto, onToggleLike = onToggleLike)
            }

            if (state.feedState.isLoadingMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        if (showFilters) {
            SearchDrawer(
                filter = state.photoFilter,
                suggestionsByField = suggestionsByField,
                onFilterChange = onFilterChange,
                onRequestSuggestions = onRequestSuggestions,
                onClearSuggestions = onClearSuggestions,
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

        if (state.feedState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SearchDrawer(
    filter: PhotoFilter,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onFilterChange: (PhotoFilter) -> Unit,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    var draft by remember(filter) { mutableStateOf(filter) }
    var authorText by rememberSaveable(filter.author) { mutableStateOf(filter.author) }
    var airlineText by rememberSaveable(filter.airline) { mutableStateOf(filter.airline) }
    var modelText by rememberSaveable(filter.aircraftModel) { mutableStateOf(filter.aircraftModel) }
    var cameraText by rememberSaveable(filter.camera) { mutableStateOf(filter.camera) }
    var lensText by rememberSaveable(filter.lens) { mutableStateOf(filter.lens) }
    var registrationText by rememberSaveable(filter.registration) { mutableStateOf(filter.registration) }
    var locationText by rememberSaveable(filter.locationCode) { mutableStateOf(filter.locationCode) }

    LaunchedEffect(filter) {
        draft = filter
    }
    LaunchedEffect(draft) {
        delay(500)
        if (draft != filter) {
            onFilterChange(draft)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.keyword,
                onValueChange = { draft = draft.copy(keyword = it) },
                label = { Text(strings.searchHint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = authorText,
                    label = strings.author,
                    suggestions = suggestionsByField["userid"].orEmpty(),
                    onValueChange = {
                        authorText = it
                        draft = draft.copy(author = it)
                        if (it.isBlank()) onClearSuggestions("userid") else onRequestSuggestions("userid", it)
                    },
                    onSuggestionClick = {
                        authorText = it.label
                        draft = draft.copy(author = it.label)
                        onClearSuggestions("userid")
                    },
                )
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = locationText,
                    label = strings.location,
                    suggestions = suggestionsByField["iatacode"].orEmpty(),
                    onValueChange = {
                        locationText = it
                        draft = draft.copy(locationCode = it)
                        if (it.isBlank()) onClearSuggestions("iatacode") else onRequestSuggestions("iatacode", it)
                    },
                    onSuggestionClick = {
                        locationText = it.label
                        draft = draft.copy(locationCode = it.value)
                        onClearSuggestions("iatacode")
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = registrationText,
                    label = strings.registration,
                    suggestions = suggestionsByField["registration_number"].orEmpty(),
                    onValueChange = {
                        registrationText = it
                        draft = draft.copy(registration = it)
                        if (it.isBlank()) onClearSuggestions("registration_number") else onRequestSuggestions("registration_number", it)
                    },
                    onSuggestionClick = {
                        registrationText = it.label
                        draft = draft.copy(registration = it.value)
                        onClearSuggestions("registration_number")
                    },
                )
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = airlineText,
                    label = strings.airline,
                    suggestions = suggestionsByField["airline"].orEmpty(),
                    onValueChange = {
                        airlineText = it
                        draft = draft.copy(airline = it)
                        if (it.isBlank()) onClearSuggestions("airline") else onRequestSuggestions("airline", it)
                    },
                    onSuggestionClick = {
                        airlineText = it.label
                        draft = draft.copy(airline = it.value)
                        onClearSuggestions("airline")
                    },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = modelText,
                    label = strings.aircraftModel,
                    suggestions = suggestionsByField["aircraft_model"].orEmpty(),
                    onValueChange = {
                        modelText = it
                        draft = draft.copy(aircraftModel = it)
                        if (it.isBlank()) onClearSuggestions("aircraft_model") else onRequestSuggestions("aircraft_model", it)
                    },
                    onSuggestionClick = {
                        modelText = it.label
                        draft = draft.copy(aircraftModel = it.value)
                        onClearSuggestions("aircraft_model")
                    },
                )
                SuggestionField(
                    modifier = Modifier.weight(1f),
                    value = cameraText,
                    label = strings.camera,
                    suggestions = suggestionsByField["cam"].orEmpty(),
                    onValueChange = {
                        cameraText = it
                        draft = draft.copy(camera = it)
                        if (it.isBlank()) onClearSuggestions("cam") else onRequestSuggestions("cam", it)
                    },
                    onSuggestionClick = {
                        cameraText = it.label
                        draft = draft.copy(camera = it.value)
                        onClearSuggestions("cam")
                    },
                )
            }
            SuggestionField(
                value = lensText,
                label = strings.lens,
                suggestions = suggestionsByField["lens"].orEmpty(),
                onValueChange = {
                    lensText = it
                    draft = draft.copy(lens = it)
                    if (it.isBlank()) onClearSuggestions("lens") else onRequestSuggestions("lens", it)
                },
                onSuggestionClick = {
                    lensText = it.label
                    draft = draft.copy(lens = it.value)
                    onClearSuggestions("lens")
                },
            )
            TextButton(
                onClick = {
                    draft = PhotoFilter()
                    authorText = ""
                    airlineText = ""
                    modelText = ""
                    cameraText = ""
                    lensText = ""
                    registrationText = ""
                    locationText = ""
                    onClearSuggestions("userid")
                    onClearSuggestions("iatacode")
                    onClearSuggestions("registration_number")
                    onClearSuggestions("airline")
                    onClearSuggestions("aircraft_model")
                    onClearSuggestions("cam")
                    onClearSuggestions("lens")
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("清除筛选")
            }
        }
    }
}

private fun derivedLoadMore(
    gridState: LazyGridState,
    state: AppUiState,
): Boolean {
    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    val totalItems = gridState.layoutInfo.totalItemsCount
    if (totalItems == 0) return false
    return state.feedState.hasMore &&
        !state.feedState.isLoading &&
        !state.feedState.isLoadingMore &&
        lastVisible >= totalItems - 7
}

@Composable
private fun SuggestionField(
    value: String,
    label: String,
    suggestions: List<SearchSuggestion>,
    onValueChange: (String) -> Unit,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        suggestions.take(3).forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(item) },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF3F6FB),
            ) {
                Text(
                    text = "${item.label} (${item.count})",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    }
}
