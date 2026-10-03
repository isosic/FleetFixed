package com.isosic.fleetfixer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isosic.fleetfixer.core.domain.AppAuth
import com.isosic.fleetfixer.core.domain.StravaAuthRepository
import com.isosic.fleetfixer.core.ui.theme.FleetFixerTheme
import com.isosic.fleetfixer.feature.auth.ui.LoginScreen
import com.isosic.fleetfixer.feature.bikes.addbike.AddBikeScreen
import com.isosic.fleetfixer.feature.bikes.detail.BikeComponentsScreen
import com.isosic.fleetfixer.feature.bikes.detail.BikeDetailScreen
import com.isosic.fleetfixer.feature.bikes.detail.BikePendingWorkScreen
import com.isosic.fleetfixer.feature.bikes.detail.ComponentDetailScreen
import com.isosic.fleetfixer.feature.bikes.home.HomeScreen
import com.isosic.fleetfixer.navigation.Routes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val stravaAuthRepository: StravaAuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStravaIntent(intent)
        enableEdgeToEdge()
        setContent {
            FleetFixerTheme {
                val appAuth: AppAuth = koinInject()
                var startDestination by remember { mutableStateOf<String?>(null) }
                val navController = rememberNavController()

                LaunchedEffect(appAuth) {
                    startDestination = if (appAuth.hasCurrentUser()) {
                        Routes.Home
                    } else {
                        Routes.Login
                    }
                }

                val destination = startDestination
                if (destination == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = destination,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Routes.Login) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Routes.Home) {
                                        popUpTo(Routes.Login) { inclusive = true }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        composable(Routes.Home) {
                            HomeScreen(
                                onAddBikeClick = { navController.navigate(Routes.AddBike) },
                                onBikeClick = { bike ->
                                    navController.navigate(Routes.bikeDetail(bike.id))
                                },
                                onLogoutClick = {
                                    navController.navigate(Routes.Login) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        composable(Routes.AddBike) {
                            AddBikeScreen(
                                onBikeSaved = { navController.navigateUp() },
                                onNavigateBack = { navController.navigateUp() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        composable(
                            route = Routes.BikeDetail,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val bikeId = checkNotNull(entry.arguments?.getString("bikeId"))
                            key(bikeId) {
                                BikeDetailScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onComponentsClick = {
                                        navController.navigate(Routes.bikeComponents(bikeId))
                                    },
                                    onPendingWorkClick = {
                                        navController.navigate(Routes.bikePendingWork(bikeId))
                                    },
                                    viewModel = koinViewModel(
                                        viewModelStoreOwner = entry
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        composable(
                            route = Routes.ComponentDetail,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType },
                                navArgument("componentType") { type = NavType.StringType }
                            )
                        ) { entry ->
                            key(
                                entry.arguments?.getString("bikeId"),
                                entry.arguments?.getString("componentType")
                            ) {
                                ComponentDetailScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    viewModel = koinViewModel(
                                        viewModelStoreOwner = entry
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        composable(
                            route = Routes.BikeComponents,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val bikeId = checkNotNull(entry.arguments?.getString("bikeId"))
                            key(bikeId) {
                                BikeComponentsScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onComponentClick = { type ->
                                        navController.navigate(
                                            Routes.componentDetail(bikeId, type.name)
                                        )
                                    },
                                    viewModel = koinViewModel(
                                        viewModelStoreOwner = entry
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        composable(
                            route = Routes.BikePendingWork,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val bikeId = checkNotNull(entry.arguments?.getString("bikeId"))
                            key(bikeId) {
                                BikePendingWorkScreen(
                                    onNavigateBack = { navController.navigateUp() },
                                    onComponentClick = { type ->
                                        navController.navigate(
                                            Routes.componentDetail(bikeId, type.name)
                                        )
                                    },
                                    viewModel = koinViewModel(
                                        viewModelStoreOwner = entry
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStravaIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(400)
            stravaAuthRepository.onHostResumedWithoutFreshCallback()
        }
    }

    private fun handleStravaIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!stravaAuthRepository.isCallbackUri(uri)) return
        Log.i(TAG, "Handling Strava intent data=$uri")
        setIntent(Intent(intent).setData(null))
        lifecycleScope.launch {
            stravaAuthRepository.handleCallbackIntent(uri)
        }
    }

    private companion object {
        const val TAG = "StravaAuth"
    }
}
