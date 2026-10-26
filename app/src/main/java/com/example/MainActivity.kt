package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.BmkExpressTheme
import com.example.ui.theme.BmkNavy
import com.example.ui.theme.BmkOrange
import com.example.ui.viewmodel.BmkViewModel

sealed class BmkTabScreen(
    val route: String,
    val titleBurmese: String,
    val titleEnglish: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
) {
    object Mandalay : BmkTabScreen("mandalay", "မန္တလေးဂိတ်", "Mandalay", Icons.Filled.Store, Icons.Outlined.Store)
    object Car : BmkTabScreen("car", "ကားစရိတ်", "Car", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping)
    object Kalay : BmkTabScreen("kalay", "ကလေးဂိတ်", "Kalay", Icons.Filled.LocationOn, Icons.Outlined.LocationOn)
    object Workers : BmkTabScreen("workers", "အလုပ်သမား", "Workers", Icons.Filled.Engineering, Icons.Outlined.Engineering)
    object Settings : BmkTabScreen("settings", "ဆက်တင်", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.util.configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            BmkExpressTheme {
                BmkAppMain()
            }
        }
    }
}

@Composable
fun BmkAppMain(viewModel: BmkViewModel = viewModel()) {
    var currentRoute by remember { mutableStateOf(BmkTabScreen.Mandalay.route) }
    val settingsState by viewModel.settingsState.collectAsState()

    // Handle system back button
    if (currentRoute != BmkTabScreen.Mandalay.route) {
        BackHandler {
            currentRoute = BmkTabScreen.Mandalay.route
        }
    }

    val tabs = listOf(
        BmkTabScreen.Mandalay,
        BmkTabScreen.Car,
        BmkTabScreen.Kalay,
        BmkTabScreen.Workers,
        BmkTabScreen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = BmkNavy,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    val labelText = if (settingsState.language == "my") tab.titleBurmese else tab.titleEnglish

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentRoute = tab.route },
                        label = {
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.iconSelected else tab.iconUnselected,
                                contentDescription = labelText
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BmkNavy,
                            selectedTextColor = BmkNavy,
                            indicatorColor = BmkOrange.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                BmkTabScreen.Mandalay.route -> MandalayTabScreen(viewModel = viewModel)
                BmkTabScreen.Car.route -> CarTabScreen(viewModel = viewModel)
                BmkTabScreen.Kalay.route -> KalaymyoTabScreen(viewModel = viewModel)
                BmkTabScreen.Workers.route -> WorkersTabScreen(viewModel = viewModel)
                BmkTabScreen.Settings.route -> SettingsTabScreen(viewModel = viewModel)
            }
        }
    }
}
