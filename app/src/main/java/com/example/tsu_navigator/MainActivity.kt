package com.example.tsu_navigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import ui.screens.Grid
import com.example.tsu_navigator.ui.screens.WhereEatScreen
import com.example.tsu_navigator.ui.theme.TSUNavigatorTheme
import com.example.tsu_navigator.ui.screens.CoworkingScreen
import com.example.tsu_navigator.ui.screens.CoworkingSpace

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TSUNavigatorTheme {
                TSUNavigatorApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun TSUNavigatorApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.MAP) }
    var selectedPlace by rememberSaveable { mutableStateOf<EatPlace?>(null) }
    var selectedCoworking by rememberSaveable { mutableStateOf<CoworkingSpace?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.MAP -> {
                    Grid(
                        selectedPlace = selectedPlace,
                        selectedCoworking = selectedCoworking
                    )
                }
                AppDestinations.EAT -> {
                    WhereEatScreen(
                        onPlaceSelected = { place ->
                            selectedPlace = place
                            selectedCoworking = null
                            currentDestination = AppDestinations.MAP
                        },
                        onBack = { currentDestination = AppDestinations.MAP }
                    )
                }
                AppDestinations.COWORKING -> {
                    CoworkingScreen(
                        onSpaceSelected = { space ->
                            selectedCoworking = space
                            selectedPlace = null
                            currentDestination = AppDestinations.MAP
                        },
                        onBack = { currentDestination = AppDestinations.MAP }
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    MAP("Карта", R.drawable.ic_home),
    EAT("Где поесть", R.drawable.ic_favorite),
    COWORKING("Коворкинг", R.drawable.ic_account_box),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TSUNavigatorTheme {
        Greeting("Android")
    }
}