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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.PhotoItem
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState
import coil3.compose.AsyncImage

@Composable
fun AllPhotosScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onToggleLike: (Long) -> Unit,
) {
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
                    photoCount = state.photos.size,
                    suggestionsByField = suggestionsByField,
                    onFilterChange = onFilterChange,
                    onRequestSuggestions = onRequestSuggestions,
                    onClearSuggestions = onClearSuggestions,
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
    photoCount: Int,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onFilterChange: (PhotoFilter) -> Unit,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var authorText by rememberSaveable(filter.author) { mutableStateOf(filter.author) }
    var airlineText by rememberSaveable(filter.airline) { mutableStateOf(filter.airline) }
    var modelText by rememberSaveable(filter.aircraftModel) { mutableStateOf(filter.aircraftModel) }
    var cameraText by rememberSaveable(filter.camera) { mutableStateOf(filter.camera) }
    var lensText by rememberSaveable(filter.lens) { mutableStateOf(filter.lens) }
    var registrationText by rememberSaveable(filter.registration) { mutableStateOf(filter.registration) }
    var locationText by rememberSaveable(filter.locationCode) { mutableStateOf(filter.locationCode) }

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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.allPhotosTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(strings.photosCount(photoCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                    )
                }
                OutlinedTextField(
                    value = filter.keyword,
                    onValueChange = { onFilterChange(filter.copy(keyword = it)) },
                    label = { Text(strings.searchHint) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (expanded) {
                    SuggestionField(
                        value = authorText,
                        label = strings.author,
                        suggestions = suggestionsByField["userid"].orEmpty(),
                        onValueChange = {
                            authorText = it
                            onFilterChange(filter.copy(author = it))
                            if (it.isBlank()) onClearSuggestions("userid") else onRequestSuggestions("userid", it)
                        },
                        onSuggestionClick = { item ->
                            authorText = item.label
                            onFilterChange(filter.copy(author = item.label))
                            onClearSuggestions("userid")
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SuggestionField(
                            modifier = Modifier.weight(1f),
                            value = locationText,
                            label = strings.location,
                            suggestions = suggestionsByField["iatacode"].orEmpty(),
                            onValueChange = {
                                locationText = it
                                onFilterChange(filter.copy(locationCode = it))
                                if (it.isBlank()) onClearSuggestions("iatacode") else onRequestSuggestions("iatacode", it)
                            },
                            onSuggestionClick = { item ->
                                locationText = item.label
                                onFilterChange(filter.copy(locationCode = item.value))
                                onClearSuggestions("iatacode")
                            },
                        )
                        SuggestionField(
                            modifier = Modifier.weight(1f),
                            value = registrationText,
                            label = strings.registration,
                            suggestions = suggestionsByField["registration_number"].orEmpty(),
                            onValueChange = {
                                registrationText = it
                                onFilterChange(filter.copy(registration = it))
                                if (it.isBlank()) onClearSuggestions("registration_number") else onRequestSuggestions("registration_number", it)
                            },
                            onSuggestionClick = { item ->
                                registrationText = item.label
                                onFilterChange(filter.copy(registration = item.value))
                                onClearSuggestions("registration_number")
                            },
                        )
                    }
                    SuggestionField(
                        value = airlineText,
                        label = strings.airline,
                        suggestions = suggestionsByField["airline"].orEmpty(),
                        onValueChange = {
                            airlineText = it
                            onFilterChange(filter.copy(airline = it))
                            if (it.isBlank()) onClearSuggestions("airline") else onRequestSuggestions("airline", it)
                        },
                        onSuggestionClick = { item ->
                            airlineText = item.label
                            onFilterChange(filter.copy(airline = item.value))
                            onClearSuggestions("airline")
                        },
                    )
                    SuggestionField(
                        value = modelText,
                        label = strings.aircraftModel,
                        suggestions = suggestionsByField["aircraft_model"].orEmpty(),
                        onValueChange = {
                            modelText = it
                            onFilterChange(filter.copy(aircraftModel = it))
                            if (it.isBlank()) onClearSuggestions("aircraft_model") else onRequestSuggestions("aircraft_model", it)
                        },
                        onSuggestionClick = { item ->
                            modelText = item.label
                            onFilterChange(filter.copy(aircraftModel = item.value))
                            onClearSuggestions("aircraft_model")
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SuggestionField(
                            modifier = Modifier.weight(1f),
                            value = cameraText,
                            label = strings.camera,
                            suggestions = suggestionsByField["cam"].orEmpty(),
                            onValueChange = {
                                cameraText = it
                                onFilterChange(filter.copy(camera = it))
                                if (it.isBlank()) onClearSuggestions("cam") else onRequestSuggestions("cam", it)
                            },
                            onSuggestionClick = { item ->
                                cameraText = item.label
                                onFilterChange(filter.copy(camera = item.value))
                                onClearSuggestions("cam")
                            },
                        )
                        SuggestionField(
                            modifier = Modifier.weight(1f),
                            value = lensText,
                            label = strings.lens,
                            suggestions = suggestionsByField["lens"].orEmpty(),
                            onValueChange = {
                                lensText = it
                                onFilterChange(filter.copy(lens = it))
                                if (it.isBlank()) onClearSuggestions("lens") else onRequestSuggestions("lens", it)
                            },
                            onSuggestionClick = { item ->
                                lensText = item.label
                                onFilterChange(filter.copy(lens = item.value))
                                onClearSuggestions("lens")
                            },
                        )
                    }
                }
            }
        }
    }
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
        )
        suggestions.take(5).forEach { item ->
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
