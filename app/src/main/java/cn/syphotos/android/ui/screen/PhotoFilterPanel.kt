package cn.syphotos.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.model.SearchSuggestion
import cn.syphotos.android.ui.i18n.LocalAppStrings
import kotlinx.coroutines.delay

@Composable
fun PhotoFilterPanel(
    filter: PhotoFilter,
    suggestionsByField: Map<String, List<SearchSuggestion>>,
    onFilterChange: (PhotoFilter) -> Unit,
    onRequestSuggestions: (String, String) -> Unit,
    onClearSuggestions: (String) -> Unit,
    onClearAll: () -> Unit,
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
        if (draft != filter) onFilterChange(draft)
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
                SharedSuggestionField(
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
                SharedSuggestionField(
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
                SharedSuggestionField(
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
                SharedSuggestionField(
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
                SharedSuggestionField(
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
                SharedSuggestionField(
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
            SharedSuggestionField(
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
                    onClearAll()
                },
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            ) {
                Text("清除筛选")
            }
        }
    }
}

@Composable
private fun SharedSuggestionField(
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
