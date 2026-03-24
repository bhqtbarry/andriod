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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.syphotos.android.model.AirlineDirectoryItem
import cn.syphotos.android.model.AirlineTreeItem
import cn.syphotos.android.ui.state.CategoryUiState

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onSelectAirline: (String) -> Unit,
    onSelectTypecode: (String, String) -> Unit,
    onSelectRegistration: (String) -> Unit,
    onExpandAirline: (String) -> Unit,
    onExpandTypecode: (String, String) -> Unit,
) {
    val airlineExpanded = remember { mutableStateMapOf<String, Boolean>() }
    val typeExpanded = remember { mutableStateMapOf<String, Boolean>() }
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
    }.sortedWith(compareByDescending<AirlineDirectoryItem> { it.photoCount }.thenBy { it.label.lowercase() })

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

        items(airlines, key = { it.label }) { airline ->
            val isOpen = airlineExpanded[airline.label] == true
            TreeCard(
                title = airline.label,
                subtitle = "${airline.aircraftCount} 机型",
                count = airline.photoCount,
                level = 0,
                expanded = isOpen,
                onClick = {
                    val next = !isOpen
                    airlineExpanded[airline.label] = next
                    if (next) {
                        onExpandAirline(airline.label)
                    }
                },
                onOpenPhotos = { onSelectAirline(airline.label) },
            )

            if (isOpen) {
                state.typecodesByAirline[airline.label].orEmpty().forEach { typecode ->
                    val typeKey = "${airline.label}|${typecode.typecode}"
                    val typeOpen = typeExpanded[typeKey] == true
                    TreeCard(
                        title = typecode.label,
                        subtitle = "${typecode.aircraftCount} 架飞机",
                        count = typecode.photoCount,
                        level = 1,
                        expanded = typeOpen,
                        onClick = {
                            val next = !typeOpen
                            typeExpanded[typeKey] = next
                            if (next) {
                                onExpandTypecode(airline.label, typecode.typecode)
                            }
                        },
                        onOpenPhotos = {
                            onSelectTypecode(airline.label, typecode.typecode)
                        },
                    )

                    if (typeOpen) {
                        state.registrationsByType[typeKey].orEmpty().forEach { registration ->
                            RegistrationRow(
                                item = registration,
                                onClick = { onSelectRegistration(registration.registration) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeCard(
    title: String,
    subtitle: String,
    count: Int,
    level: Int,
    expanded: Boolean,
    onClick: () -> Unit,
    onOpenPhotos: () -> Unit,
) {
    val countColor = if (count == 0) Color(0xFFD7A1A1) else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + level * 20).dp, end = 16.dp)
            .clickable { onClick() },
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
                Text(
                    text = (if (expanded) "− " else "+ ") + title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "查看该项照片",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenPhotos() },
                )
            }
            Text(
                "$count 张",
                style = MaterialTheme.typography.labelLarge,
                color = countColor,
            )
        }
    }
}

@Composable
private fun RegistrationRow(
    item: AirlineTreeItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.registration.ifBlank { item.label }, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${item.photoCount} 张",
                style = MaterialTheme.typography.labelLarge,
                color = if (item.photoCount == 0) Color(0xFFD7A1A1) else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
