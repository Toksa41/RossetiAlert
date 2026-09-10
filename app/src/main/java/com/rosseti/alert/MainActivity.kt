package com.rosseti.alert

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rosseti.alert.data.SettingsStore
import com.rosseti.alert.ui.MainScreen
import com.rosseti.alert.ui.MainViewModel
import com.rosseti.alert.ui.SettingsScreen
import com.rosseti.alert.ui.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включаем edge-to-edge — контент под системные панели,
        // но в Compose добавляем отступ statusBars вручную
        enableEdgeToEdge()

        setContent {
            RossetiTheme {
                val store = remember { SettingsStore(applicationContext) }
                AppNavigation(settingsStore = store)
            }
        }
    }

    @Composable
    private fun RossetiTheme(content: @Composable () -> Unit) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = androidx.compose.material3.lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF325491),
                error = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                errorContainer = androidx.compose.ui.graphics.Color(0xFFFFEBEE)
            ),
            content = content
        )
    }

    @Composable
    private fun AppNavigation(settingsStore: SettingsStore) {
        val navController = rememberNavController()

        val mainViewModel: MainViewModel = viewModel()
        LaunchedEffect(settingsStore) {
            mainViewModel.initSettings(settingsStore)
        }

        // Добавляем padding под status bar (чтобы не наезжало на шторку)
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

        NavHost(
            navController,
            startDestination = "main",
            modifier = Modifier.padding(statusBarPadding)
        ) {
            composable("main") {
                MainScreen(
                    viewModel = mainViewModel,
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenSite = {
                        val url = "https://www.rosseti-sib.ru/otkluchenie-energii/"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }
                )
            }

            composable("settings") {
                val settingsViewModel: SettingsViewModel = viewModel()

                LaunchedEffect(settingsStore) {
                    settingsViewModel.init(settingsStore)
                }

                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = {
                        navController.popBackStack()
                        // Перечитываем настройки в главном ViewModel
                        mainViewModel.refreshSettings(settingsStore)
                    }
                )
            }
        }
    }
}