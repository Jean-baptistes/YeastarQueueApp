package com.yeastar.queuecaller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yeastar.queuecaller.ui.AppRoot
import com.yeastar.queuecaller.ui.MainViewModel
import com.yeastar.queuecaller.ui.YeastarQueueTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            YeastarQueueTheme {
                val vm: MainViewModel = viewModel()

                var permissionsGranted by remember { mutableStateOf(false) }

                val requiredPermissions = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()

                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    // On exige au minimum le micro.
                    permissionsGranted = result[Manifest.permission.RECORD_AUDIO] == true
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (micGranted) {
                        permissionsGranted = true
                    } else {
                        launcher.launch(requiredPermissions)
                    }
                    vm.restoreLastSelection()
                }

                AppRoot(
                    vm = vm,
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = { launcher.launch(requiredPermissions) }
                )
            }
        }
    }
}
