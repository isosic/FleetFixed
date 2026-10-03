package com.isosic.fleetfixer.feature.bikes.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.ui.theme.FleetFixerTheme
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

data class BikePendingWorkRow(
    val id: String,
    val workId: String,
    val type: ComponentType,
    val componentName: String,
    val description: String
)

data class BikeWorklogRow(
    val id: String,
    val type: ComponentType,
    val componentName: String,
    val description: String,
    val notes: String,
    val completedAtEpochMillis: Long
)

@Composable
fun BikePendingWorkScreen(
    onNavigateBack: () -> Unit,
    onPendingWorkClick: (componentType: ComponentType, workId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BikeDetailViewModel = koinViewModel()
) {
    val bike by viewModel.bike.collectAsStateWithLifecycle()
    val pendingItems = remember(bike) {
        bike?.components
            ?.flatMap { component ->
                component.pendingWork.map { item ->
                    BikePendingWorkRow(
                        id = "${component.type.name}-${item.id}",
                        workId = item.id,
                        type = component.type,
                        componentName = component.name,
                        description = item.description
                    )
                }
            }
            .orEmpty()
    }
    val worklogItems = remember(bike) {
        bike?.components
            ?.flatMap { component ->
                component.completedWork.map { item ->
                    BikeWorklogRow(
                        id = "${component.type.name}-${item.id}-${item.completedAtEpochMillis}",
                        type = component.type,
                        componentName = component.name,
                        description = item.description,
                        notes = item.notes,
                        completedAtEpochMillis = item.completedAtEpochMillis
                    )
                }
            }
            ?.sortedByDescending { it.completedAtEpochMillis }
            .orEmpty()
    }

    BikePendingWorkScreenContent(
        pendingItems = pendingItems,
        worklogItems = worklogItems,
        onNavigateBack = dropUnlessResumed { onNavigateBack() },
        onPendingWorkClick = onPendingWorkClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikePendingWorkScreenContent(
    pendingItems: List<BikePendingWorkRow>,
    worklogItems: List<BikeWorklogRow>,
    onNavigateBack: () -> Unit,
    onPendingWorkClick: (ComponentType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Pending work") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Pending work",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (pendingItems.isEmpty()) {
                item {
                    Text(
                        text = "No pending work",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(pendingItems, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPendingWorkClick(item.type, item.workId) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = item.type.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = item.componentName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    text = "Worklog",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            if (worklogItems.isEmpty()) {
                item {
                    Text(
                        text = "No completed work yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(worklogItems, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = item.type.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = item.componentName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (item.notes.isNotBlank() && item.notes != item.description) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatDate(item.completedAtEpochMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

@Preview(showBackground = true)
@Composable
private fun BikePendingWorkScreenContentPreview() {
    FleetFixerTheme {
        BikePendingWorkScreenContent(
            pendingItems = listOf(
                BikePendingWorkRow(
                    id = "1",
                    workId = "w1",
                    type = ComponentType.FORK,
                    componentName = "Fox 36",
                    description = "Replace seals"
                )
            ),
            worklogItems = listOf(
                BikeWorklogRow(
                    id = "2",
                    type = ComponentType.CHAIN,
                    componentName = "SRAM GX",
                    description = "Check chain wear and replace if needed",
                    notes = "Replaced with XX1 Eagle",
                    completedAtEpochMillis = System.currentTimeMillis()
                )
            ),
            onNavigateBack = {},
            onPendingWorkClick = { _, _ -> }
        )
    }
}
