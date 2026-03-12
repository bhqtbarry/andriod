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
import cn.syphotos.android.ui.state.AppUiState

@Composable
fun MapScreen(
    state: AppUiState,
    onFilterChange: (PhotoFilter) -> Unit,
    onApplyMapSelection: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Map clusters placeholder", style = MaterialTheme.typography.headlineSmall)
        Text("Expected hierarchy: country -> province/state -> city. Marker tap should jump into All Photos with shared filters.")
        OutlinedTextField(
            value = state.photoFilter.locationCode,
            onValueChange = { onFilterChange(state.photoFilter.copy(locationCode = it)) },
            label = { Text("Location / IATA") },
            modifier = Modifier.fillMaxWidth(),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Initial location strategy")
                Text("1. Request precise location permission")
                Text("2. If denied, infer country from app language")
                Button(onClick = { onApplyMapSelection("CGK") }) {
                    Text("Simulate marker tap for CGK")
                }
            }
        }
    }
}

