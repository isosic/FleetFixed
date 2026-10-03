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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.ui.theme.FleetFixerTheme
import org.koin.androidx.compose.koinViewModel

data class BikePendingWorkRow(
    val id: String,
    val type: ComponentType,
    val componentName: String,
    val description: String
)

@Composable
fun BikePendingWorkScreen(
    onNavigateBack: () -> Unit,
    onComponentClick: (ComponentType) -> Unit,
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
                        type = component.type,
                        componentName = component.name,
                        description = item.description
                    )
                }
            }
            .orEmpty()
    }

    BikePendingWorkScreenContent(
        bikeName = bike?.name,
        pendingItems = pendingItems,
        onNavigateBack = dropUnlessResumed { onNavigateBack() },
        onComponentClick = onComponentClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikePendingWorkScreenContent(
    bikeName: String?,
    pendingItems: List<BikePendingWorkRow>,
    onNavigateBack: () -> Unit,
    onComponentClick: (ComponentType) -> Unit,
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
        if (pendingItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = bikeName?.let { "No pending work for $it" } ?: "No pending work",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Add pending work on a component to see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingItems, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onComponentClick(item.type) }
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BikePendingWorkScreenContentPreview() {
    FleetFixerTheme {
        BikePendingWorkScreenContent(
            bikeName = "Trail Rider",
            pendingItems = listOf(
                BikePendingWorkRow(
                    id = "1",
                    type = ComponentType.FORK,
                    componentName = "Fox 36",
                    description = "Replace seals"
                ),
                BikePendingWorkRow(
                    id = "2",
                    type = ComponentType.CHAIN,
                    componentName = "SRAM GX",
                    description = "Clean and lube"
                )
            ),
            onNavigateBack = {},
            onComponentClick = {}
        )
    }
}
