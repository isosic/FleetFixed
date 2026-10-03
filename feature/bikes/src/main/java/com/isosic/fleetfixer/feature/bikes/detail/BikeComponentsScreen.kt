package com.isosic.fleetfixer.feature.bikes.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import com.isosic.fleetfixer.core.model.Bike
import com.isosic.fleetfixer.core.model.BikeComponent
import com.isosic.fleetfixer.core.model.ComponentType
import com.isosic.fleetfixer.core.ui.theme.FleetFixerTheme
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun BikeComponentsScreen(
    onNavigateBack: () -> Unit,
    onComponentClick: (ComponentType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BikeDetailViewModel = koinViewModel()
) {
    val bike by viewModel.bike.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.onComponentsVisible()
        }
    }

    BikeComponentsScreenContent(
        bike = bike,
        onNavigateBack = dropUnlessResumed { onNavigateBack() },
        onComponentClick = onComponentClick,
        onAddComponent = { type, name, notes, dateAdded, onAdded ->
            viewModel.addComponent(type, name, notes, dateAdded, onAdded)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BikeComponentsScreenContent(
    bike: Bike?,
    onNavigateBack: () -> Unit,
    onComponentClick: (ComponentType) -> Unit,
    onAddComponent: (ComponentType, String, String, Long?, onAdded: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var componentToAdd by remember { mutableStateOf<ComponentType?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Components") },
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
        if (bike == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bike not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ComponentType.allSlots, key = { it.name }) { type ->
                    val component = bike.componentFor(type)
                    ComponentSlotRow(
                        type = type,
                        component = component,
                        onAddClick = { componentToAdd = type },
                        onClick = { onComponentClick(type) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    componentToAdd?.let { type ->
        AddComponentDialog(
            type = type,
            onDismiss = { componentToAdd = null },
            onSave = { name, notes, dateAdded ->
                onAddComponent(type, name, notes, dateAdded) {
                    componentToAdd = null
                    onComponentClick(type)
                }
            }
        )
    }
}

@Composable
private fun ComponentSlotRow(
    type: ComponentType,
    component: BikeComponent?,
    onAddClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (component != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            if (component == null) {
                Text(
                    text = "Not added",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        if (component == null) {
            TextButton(onClick = onAddClick) {
                Text("Add")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddComponentDialog(
    type: ComponentType,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String, dateAddedEpochMillis: Long?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var dateAddedMillis by rememberSaveable {
        mutableStateOf<Long?>(System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${type.displayName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        dateAddedMillis?.let { "Purchase date: ${formatDate(it)}" }
                            ?: "Purchase date: not set"
                    )
                }
                TextButton(onClick = { dateAddedMillis = null }) {
                    Text("Clear purchase date")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, notes, dateAddedMillis) },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateAddedMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateAddedMillis = it }
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
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

@Preview(showBackground = true)
@Composable
private fun BikeComponentsScreenContentPreview() {
    FleetFixerTheme {
        BikeComponentsScreenContent(
            bike = Bike(
                name = "Trail Rider",
                components = listOf(
                    BikeComponent(
                        type = ComponentType.FORK,
                        name = "Fox 36"
                    )
                )
            ),
            onNavigateBack = {},
            onComponentClick = {},
            onAddComponent = { _, _, _, _, _ -> }
        )
    }
}
