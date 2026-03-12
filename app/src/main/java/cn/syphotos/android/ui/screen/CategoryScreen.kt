package cn.syphotos.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.CategoryCount
import cn.syphotos.android.ui.components.GradientHero
import cn.syphotos.android.ui.i18n.LocalAppStrings
import cn.syphotos.android.ui.state.CategoryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(state: CategoryUiState) {
    val strings = LocalAppStrings.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val titles = listOf(strings.airlineTab, strings.aircraftTab)
    val currentItems = if (selectedTab == 0) state.airlines else state.models

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHero(
            eyebrow = strings.categoryTitle,
            title = strings.categoryTitle,
            subtitle = strings.categorySubtitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(currentItems) { item ->
                CategoryRow(item)
            }
        }
    }
}

@Composable
private fun CategoryRow(item: CategoryCount) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text(strings.photosCount(item.count), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
