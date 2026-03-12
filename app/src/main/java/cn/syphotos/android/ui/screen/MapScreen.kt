package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GradientHero(
            eyebrow = strings.navMap,
            title = strings.mapTitle,
            subtitle = strings.mapSubtitle,
        )
        Text(strings.mapClusters, style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = state.photoFilter.locationCode,
            onValueChange = { onFilterChange(state.photoFilter.copy(locationCode = it)) },
            label = { Text(strings.location) },
            modifier = Modifier.fillMaxWidth(),
        )
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
}
