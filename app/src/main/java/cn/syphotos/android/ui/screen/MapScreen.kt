package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.PhotoFilter
import cn.syphotos.android.ui.components.GradientHero
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun MapScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GradientHero(
                eyebrow = strings.navMap,
                title = strings.mapTitle,
                subtitle = strings.mapSubtitle,
            )
        }
        item {
            Text(strings.mapClusters, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            OutlinedTextField(
                value = state.photoFilter.locationCode,
                onValueChange = { onFilterChange(state.photoFilter.copy(locationCode = it)) },
                label = { Text(strings.location) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.mapStrategy, style = MaterialTheme.typography.titleMedium)
                    Text(strings.mapStepPermission)
                    Text(strings.mapStepLanguage)
                    Button(onClick = { onApplyMapSelection("CGK") }) {
                        Text(strings.simulateMarker)
                    }
                }
            }
        }
        state.mapState.errorMessage?.let { message ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        if (state.mapState.isLoading) {
            item {
                CircularProgressIndicator()
            }
        }
        items(state.mapState.clusters, key = { it.id }) { cluster ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cluster.name, style = MaterialTheme.typography.titleMedium)
                    Text("${cluster.level} • ${strings.photosCount(cluster.photoCount)}")
                    Button(onClick = { onApplyMapSelection(cluster.locationCode) }) {
                        Text(cluster.locationCode)
                    }
                }
            }
        }
    }
}
