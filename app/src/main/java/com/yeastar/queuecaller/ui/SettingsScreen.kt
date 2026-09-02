package com.yeastar.queuecaller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yeastar.queuecaller.data.Extension
import com.yeastar.queuecaller.data.PbxConfig
import com.yeastar.queuecaller.data.SipTransport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val config by vm.pbxConfig.collectAsState()
    val extensions by vm.extensions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
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
                .verticalScroll(rememberScrollState())
        ) {
            PbxConfigSection(config = config, onSave = vm::updatePbxConfig)

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            ExtensionPoolSection(
                extensions = extensions,
                onAdd = vm::addExtension,
                onRemove = vm::removeExtension
            )
        }
    }
}

@Composable
private fun PbxConfigSection(
    config: PbxConfig,
    onSave: (PbxConfig) -> Unit
) {
    var domain by remember(config) { mutableStateOf(config.domain) }
    var port by remember(config) { mutableStateOf(config.port.toString()) }
    var queue by remember(config) { mutableStateOf(config.queueNumber) }
    var transport by remember(config) { mutableStateOf(config.transport) }

    Text("Serveur PBX Yeastar", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = domain,
        onValueChange = { domain = it },
        label = { Text("Adresse du PBX (IP ou domaine)") },
        placeholder = { Text("ex. 192.168.1.10 ou pbx.exemple.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = port,
        onValueChange = { port = it.filter { c -> c.isDigit() } },
        label = { Text("Port SIP") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))

    Text("Transport", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SipTransport.values().forEach { t ->
            FilterChip(
                selected = transport == t,
                onClick = {
                    transport = t
                    // Ajuster le port par défaut selon le transport.
                    port = if (t == SipTransport.TLS) "5061" else "5060"
                },
                label = { Text(t.name) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = queue,
        onValueChange = { queue = it },
        label = { Text("Numéro de la file d'attente") },
        placeholder = { Text("6400") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    Button(
        onClick = {
            onSave(
                PbxConfig(
                    domain = domain.trim(),
                    port = port.toIntOrNull() ?: 5060,
                    transport = transport,
                    queueNumber = queue.trim().ifBlank { "6400" }
                )
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Enregistrer la configuration")
    }
}

@Composable
private fun ExtensionPoolSection(
    extensions: List<Extension>,
    onAdd: (Extension) -> Unit,
    onRemove: (Extension) -> Unit
) {
    Text("Pool d'extensions", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        "Ajoutez les postes que l'application pourra utiliser pour appeler la file.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))

    // Liste existante
    extensions.forEach { ext ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ext.displayName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Mot de passe : ${"•".repeat(ext.password.length.coerceAtMost(8))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onRemove(ext) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    AddExtensionForm(onAdd = onAdd)
}

@Composable
private fun AddExtensionForm(onAdd: (Extension) -> Unit) {
    var number by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Ajouter une extension", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() } },
                label = { Text("Numéro d'extension") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe SIP (registration)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Libellé (facultatif)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (number.isNotBlank() && password.isNotBlank()) {
                        onAdd(Extension(number.trim(), password, label.trim()))
                        number = ""; password = ""; label = ""
                    }
                },
                enabled = number.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ajouter au pool")
            }
        }
    }
}
