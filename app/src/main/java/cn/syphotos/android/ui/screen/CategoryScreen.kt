package cn.syphotos.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.AirlineDirectoryItem
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.CategoryUiState

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onSelectAirline: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val airlines = if (state.airlineDirectory.isNotEmpty()) {
        state.airlineDirectory
    } else {
        state.airlines.map {
            AirlineDirectoryItem(
                label = it.name,
                aircraftCount = 0,
                photoCount = it.count,
                href = "",
                photoStatus = if (it.count > 0) "has-photos" else "no-photos",
            )
        }
    }.sortedWith(
        compareByDescending<AirlineDirectoryItem> { it.photoCount }.thenBy { it.label.lowercase() },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (airlines.isEmpty()) {
            item {
                Text(
                    text = "暂无航司数据",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(airlines, key = { it.label }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { onSelectAirline(item.label) },
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Aircraft ${item.aircraftCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        strings.photosCount(item.photoCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
