package com.isosic.fleetfixer.screens.homescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isosic.fleetfixer.models.Bike
import com.isosic.fleetfixer.ui.theme.FleetFixerTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onAddBikeClick: () -> Unit,
    onBikeClick: (Bike) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = koinViewModel()
) {
    val bikes by viewModel.bikes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.stravaMessage) {
        val message = uiState.stravaMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearStravaMessage()
    }

    uiState.stravaLinkPrompt?.let { prompt ->
        StravaBikeLinkDialog(
            prompt = prompt,
            onLink = viewModel::linkSelectedLocalBike,
            onImportAsNew = viewModel::importStravaBikeAsNew,
            onSkip = viewModel::skipStravaBike
        )
    }

    HomeScreenContent(
        bikes = bikes,
        isStravaConnected = uiState.isStravaConnected,
        isStravaBusy = uiState.isStravaBusy,
        snackbarHostState = snackbarHostState,
        onAddBikeClick = onAddBikeClick,
        onBikeClick = onBikeClick,
        onStravaClick = { viewModel.onStravaClick(context) },
        onImportStravaBikesClick = viewModel::importStravaBikes,
        onLogoutClick = {
            viewModel.logout(onLoggedOut = onLogoutClick)
        },
        modifier = modifier
    )
}

@Composable
private fun StravaBikeLinkDialog(
    prompt: StravaLinkPrompt,
    onLink: (localBikeId: String) -> Unit,
    onImportAsNew: () -> Unit,
    onSkip: () -> Unit
) {
    var selectedLocalBikeId by remember(prompt.stravaBike.id) {
        mutableStateOf(prompt.candidates.firstOrNull()?.id)
    }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Link Strava bike") },
        text = {
            Column {
                Text(
                    text = "Does \"${prompt.stravaBike.name}\" match one of your bikes?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                prompt.candidates.forEach { bike ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedLocalBikeId == bike.id,
                                onClick = { selectedLocalBikeId = bike.id },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLocalBikeId == bike.id,
                            onClick = { selectedLocalBikeId = bike.id }
                        )
                        Text(
                            text = bike.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = selectedLocalBikeId ?: return@TextButton
                    onLink(id)
                },
                enabled = selectedLocalBikeId != null
            ) {
                Text("Link")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onImportAsNew) {
                    Text("Add as new")
                }
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    bikes: List<Bike>,
    isStravaConnected: Boolean,
    isStravaBusy: Boolean,
    snackbarHostState: SnackbarHostState,
    onAddBikeClick: () -> Unit,
    onBikeClick: (Bike) -> Unit,
    onStravaClick: () -> Unit,
    onImportStravaBikesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Bikes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()
                    if (bikes.isEmpty()) {
                        Text(
                            text = "No bikes yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(bikes, key = { it.id }) { bike ->
                                NavigationDrawerItem(
                                    label = { Text(bike.name) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        onBikeClick(bike)
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    if (isStravaConnected) {
                        NavigationDrawerItem(
                            label = { Text("Import Strava bikes") },
                            selected = false,
                            onClick = {
                                if (isStravaBusy) return@NavigationDrawerItem
                                scope.launch { drawerState.close() }
                                onImportStravaBikesClick()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    NavigationDrawerItem(
                        label = {
                            Text(
                                when {
                                    isStravaBusy && !isStravaConnected -> "Updating Strava…"
                                    isStravaConnected -> "Disconnect Strava"
                                    else -> "Connect Strava"
                                }
                            )
                        },
                        selected = false,
                        onClick = {
                            if (isStravaBusy) return@NavigationDrawerItem
                            scope.launch { drawerState.close() }
                            onStravaClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FleetFixer") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open bike list"
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onLogoutClick) {
                            Text("Logout")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddBikeClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add bike"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (bikes.isEmpty()) {
                            "No bikes in your fleet"
                        } else {
                            "${bikes.size} bike${if (bikes.size == 1) "" else "s"} in your fleet"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Open the drawer to see your bikes, or tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isStravaConnected) {
                            "Strava connected"
                        } else {
                            "Strava not connected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenContentPreview() {
    FleetFixerTheme {
        HomeScreenContent(
            bikes = listOf(
                Bike(name = "Trail Rider"),
                Bike(name = "City Commuter")
            ),
            isStravaConnected = true,
            isStravaBusy = false,
            snackbarHostState = SnackbarHostState(),
            onAddBikeClick = {},
            onBikeClick = {},
            onStravaClick = {},
            onImportStravaBikesClick = {},
            onLogoutClick = {}
        )
    }
}
