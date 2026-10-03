package com.isosic.fleetfixer.feature.bikes.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.model.PendingWorkItem
import com.isosic.fleetfixer.core.ui.formatDistanceKm
import com.isosic.fleetfixer.core.ui.theme.FleetFixerTheme
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun ComponentDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComponentDetailViewModel = koinViewModel()
) {
    val component by viewModel.component.collectAsStateWithLifecycle()

    ComponentDetailScreenContent(
        title = viewModel.componentTypeLabel,
        component = component,
        onNavigateBack = dropUnlessResumed { onNavigateBack() },
        onPurchaseDateChange = viewModel::updatePurchaseDate,
        onAddPendingWork = viewModel::addPendingWork,
        onRemovePendingWork = viewModel::removePendingWork,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentDetailScreenContent(
    title: String,
    component: BikeComponent?,
    onNavigateBack: () -> Unit,
    onPurchaseDateChange: (Long?) -> Unit,
    onAddPendingWork: (String) -> Unit,
    onRemovePendingWork: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddPendingWork by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPendingWork = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add pending work"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            component == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            component.dateAddedEpochMillis?.let {
                                "Purchase date: ${formatDate(it)}"
                            } ?: "Purchase date: not set"
                        )
                    }
                    TextButton(
                        onClick = { onPurchaseDateChange(null) },
                        enabled = component.dateAddedEpochMillis != null
                    ) {
                        Text("Clear purchase date")
                    }
                    Text(
                        text = "Total mileage: ${formatDistanceKm(component.totalDistanceMeters)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Since last service: ${formatDistanceKm(component.distanceSinceServiceMeters)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Pending work",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (component.pendingWork.isEmpty()) {
                        Text(
                            text = "No pending work",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Add pending work on a component to see it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        component.pendingWork.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onRemovePendingWork(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove pending work"
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDatePicker && component != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = component.dateAddedEpochMillis
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onPurchaseDateChange)
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddPendingWork) {
        AddPendingWorkDialog(
            onDismiss = { showAddPendingWork = false },
            onSave = { description ->
                onAddPendingWork(description)
                showAddPendingWork = false
            }
        )
    }
}

@Composable
private fun AddPendingWorkDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add pending work") },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(description) },
                enabled = description.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

@Preview(showBackground = true)
@Composable
private fun ComponentDetailScreenContentPreview() {
    FleetFixerTheme {
        ComponentDetailScreenContent(
            title = "Fork",
            component = BikeComponent(
                type = ComponentType.FORK,
                name = "Fox 36",
                dateAddedEpochMillis = System.currentTimeMillis(),
                totalDistanceMeters = 12_400.0,
                distanceSinceServiceMeters = 1_200.0,
                pendingWork = listOf(
                    PendingWorkItem(description = "Replace seals"),
                    PendingWorkItem(description = "Check air pressure")
                )
            ),
            onNavigateBack = {},
            onPurchaseDateChange = {},
            onAddPendingWork = {},
            onRemovePendingWork = {}
        )
    }
}
