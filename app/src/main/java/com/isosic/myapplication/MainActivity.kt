package com.isosic.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.isosic.myapplication.models.Bike
import com.isosic.myapplication.screens.addbike.AddBikeScreen
import com.isosic.myapplication.screens.homescreen.HomeScreen
import com.isosic.myapplication.ui.theme.FleetFixerTheme

private sealed interface Screen {
    data object Home : Screen
    data object AddBike : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FleetFixerTheme {
                var bikes by remember { mutableStateOf(listOf<Bike>()) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        bikes = bikes,
                        onAddBikeClick = { currentScreen = Screen.AddBike },
                        modifier = Modifier.fillMaxSize()
                    )

                    Screen.AddBike -> AddBikeScreen(
                        onBikeCreated = { bike ->
                            bikes = bikes + bike
                            currentScreen = Screen.Home
                        },
                        onNavigateBack = { currentScreen = Screen.Home },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
