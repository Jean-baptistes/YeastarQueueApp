package com.yeastar.queuecaller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yeastar.queuecaller.data.CallState
import com.yeastar.queuecaller.data.RegistrationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    vm: MainViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val config by vm.pbxConfig.collectAsState()
    val extensions by vm.extensions.collectAsState()
    val selected by vm.selectedExtension.collectAsState()
    val regState by vm.registrationState.collectAsState()
    val callState by vm.callState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appel file ${config.queueNumber}") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Bandeaux d'avertissement ---
            if (!config.isValid()) {
                WarningCard(
                    "Configuration PBX incomplète. Ouvrez les paramètres pour saisir l'adresse du PBX.",
                    onOpenSettings
                )
            } else if (extensions.isEmpty()) {
                WarningCard(
                    "Aucune extension dans le pool. Ajoutez au moins un poste dans les paramètres.",
                    onOpenSettings
                )
            }
            if (!permissionsGranted) {
                Spacer(Modifier.height(8.dp))
                WarningCard(
                    "L'autorisation micro est nécessaire pour appeler.",
                    onRequestPermissions,
                    actionLabel = "Autoriser"
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Sélection d'extension (pool) ---
            if (callState == CallState.IDLE || callState == CallState.ENDED) {
                Text(
                    "Choisir une extension disponible",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))
                extensions.forEach { ext ->
                    val isSelected = selected?.number == ext.number
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = config.isValid()) { vm.selectAndRegister(ext) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { vm.selectAndRegister(ext) },
                                enabled = config.isValid()
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(ext.displayName, modifier = Modifier.weight(1f))
                            if (isSelected) RegistrationBadge(regState)
                        }
                    }
                }

                if (extensions.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { vm.autoSelectFirst() },
                        enabled = config.isValid()
                    ) {
                        Text("Sélection automatique")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Zone d'appel ---
            CallControls(vm = vm, callState = callState, regState = regState)
        }
    }
}

@Composable
private fun CallControls(
    vm: MainViewModel,
    callState: CallState,
    regState: RegistrationState
) {
    val config by vm.pbxConfig.collectAsState()
    val muted by vm.muted.collectAsState()
    val speakerOn by vm.speakerOn.collectAsState()

    val inCall = callState == CallState.OUTGOING_INIT ||
            callState == CallState.OUTGOING_RINGING ||
            callState == CallState.CONNECTED

    Text(
        text = statusLabel(callState, regState),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(24.dp))

    if (!inCall) {
        // Gros bouton d'appel vert
        val canCall = regState == RegistrationState.REGISTERED
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RoundActionButton(
                enabled = canCall,
                background = if (canCall) MaterialTheme.colorScheme.tertiary else Color.Gray,
                icon = Icons.Filled.Call,
                contentDescription = "Appeler la file",
                onClick = { vm.callQueue() }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Appeler la file ${config.queueNumber}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        // Contrôles en communication
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.toggleMute() }) {
                Icon(
                    if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Micro",
                    tint = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { vm.toggleSpeaker() }) {
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = "Haut-parleur",
                    tint = if (speakerOn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        RoundActionButton(
            enabled = true,
            background = MaterialTheme.colorScheme.error,
            icon = Icons.Filled.CallEnd,
            contentDescription = "Raccrocher",
            onClick = { vm.hangUp() }
        )
    }
}

@Composable
private fun RoundActionButton(
    enabled: Boolean,
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(background)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun RegistrationBadge(state: RegistrationState) {
    when (state) {
        RegistrationState.PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        RegistrationState.REGISTERED -> StatusDot(MaterialTheme.colorScheme.tertiary, "Prêt")
        RegistrationState.FAILED -> StatusDot(MaterialTheme.colorScheme.error, "Échec")
        else -> {}
    }
}

@Composable
private fun StatusDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 12.sp, color = color)
    }
}

@Composable
private fun WarningCard(
    message: String,
    onAction: () -> Unit,
    actionLabel: String = "Ouvrir"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun statusLabel(callState: CallState, regState: RegistrationState): String = when (callState) {
    CallState.OUTGOING_INIT -> "Appel en cours…"
    CallState.OUTGOING_RINGING -> "Sonnerie…"
    CallState.CONNECTED -> "En communication"
    CallState.ENDED -> "Appel terminé"
    CallState.ERROR -> "Erreur d'appel"
    CallState.IDLE -> when (regState) {
        RegistrationState.REGISTERED -> "Extension enregistrée — prête"
        RegistrationState.PROGRESS -> "Enregistrement…"
        RegistrationState.FAILED -> "Enregistrement échoué"
        else -> "Sélectionnez une extension"
    }
}
