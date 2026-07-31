package com.isosic.fleetfixer

import android.os.Bundle
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isosic.fleetfixer.auth.AuthTokenStore
import com.isosic.fleetfixer.navigation.Routes
import com.isosic.fleetfixer.screens.addbike.AddBikeScreen
import com.isosic.fleetfixer.screens.homescreen.HomeScreen
import com.isosic.fleetfixer.screens.login.LoginScreen
import com.isosic.fleetfixer.ui.theme.FleetFixerTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FleetFixerTheme {
                val authTokenStore: AuthTokenStore = koinInject()
                var startDestination by remember { mutableStateOf<String?>(null) }
                val navController = rememberNavController()

                LaunchedEffect(authTokenStore) {
                    startDestination = if (authTokenStore.hasToken()) {
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
                    }
                }
            }
        }
    }
}
