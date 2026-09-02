package com.yeastar.queuecaller.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState

/** Écrans de l'application. */
enum class Screen { CALL, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    vm: MainViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    var screen by remember { mutableStateOf(Screen.CALL) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val error by vm.lastError.collectAsState()

    // Afficher les erreurs SIP dans un snackbar.
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                Screen.CALL -> CallScreen(
                    vm = vm,
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = onRequestPermissions,
                    onOpenSettings = { screen = Screen.SETTINGS }
                )
                Screen.SETTINGS -> SettingsScreen(
                    vm = vm,
                    onBack = { screen = Screen.CALL }
                )
            }
        }
    }
}
