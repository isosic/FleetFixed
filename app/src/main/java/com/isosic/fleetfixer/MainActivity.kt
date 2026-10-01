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
import com.google.firebase.auth.FirebaseAuth
import com.isosic.fleetfixer.navigation.Routes
import com.isosic.fleetfixer.screens.addbike.AddBikeScreen
import com.isosic.fleetfixer.screens.bikedetail.BikeComponentsScreen
import com.isosic.fleetfixer.screens.bikedetail.BikeDetailScreen
import com.isosic.fleetfixer.screens.bikedetail.BikePendingWorkScreen
import com.isosic.fleetfixer.screens.homescreen.HomeScreen
import com.isosic.fleetfixer.screens.login.LoginScreen
import com.isosic.fleetfixer.strava.StravaAuthClient
import com.isosic.fleetfixer.ui.theme.FleetFixerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val stravaAuthClient: StravaAuthClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStravaIntent(intent)
        enableEdgeToEdge()
        setContent {
            FleetFixerTheme {
                val firebaseAuth: FirebaseAuth = koinInject()
                var startDestination by remember { mutableStateOf<String?>(null) }
                val navController = rememberNavController()

                LaunchedEffect(firebaseAuth) {
                    startDestination = if (firebaseAuth.currentUser != null) {
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
                                onBikeSaved = { navController.popBackStack() },
                                onNavigateBack = { navController.popBackStack() },
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
                            BikeDetailScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onComponentsClick = {
                                    navController.navigate(Routes.bikeComponents(bikeId))
                                },
                                onPendingWorkClick = {
                                    navController.navigate(Routes.bikePendingWork(bikeId))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        composable(
                            route = Routes.BikeComponents,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType }
                            )
                        ) {
                            BikeComponentsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        composable(
                            route = Routes.BikePendingWork,
                            arguments = listOf(
                                navArgument("bikeId") { type = NavType.StringType }
                            )
                        ) {
                            BikePendingWorkScreen(
                                onNavigateBack = { navController.popBackStack() },
                                modifier = Modifier.fillMaxSize()
                            )
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
        // onNewIntent runs before onResume for deep links; give it a moment to clear the flag.
        lifecycleScope.launch {
            delay(400)
            stravaAuthClient.onHostResumedWithoutFreshCallback()
        }
    }

    private fun handleStravaIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (!stravaAuthClient.isCallbackUri(uri)) return
        Log.i(TAG, "Handling Strava intent data=$uri")
        setIntent(Intent(intent).setData(null))
        lifecycleScope.launch {
            stravaAuthClient.handleCallbackIntent(uri)
        }
    }

    private companion object {
        const val TAG = "StravaAuth"
    }
}
