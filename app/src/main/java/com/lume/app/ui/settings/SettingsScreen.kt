package com.lume.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lume.app.LumeApplication
import com.lume.app.ai.models.Layer2Provider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    application: LumeApplication,
    onPickVault: () -> Unit,
    onReconfigureKeys: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val provider by application.settings.layer2Provider.collectAsState(initial = Layer2Provider.CLAUDE_SONNET_4_5)
    val vault by application.settings.vaultUri.collectAsState(initial = null)
    val experimental by application.settings.experimentalPromptRewrite.collectAsState(initial = false)

    var status by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    val hasKimi = application.keyStore.getKimiKey() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ===== Modelo Camada 2 =====
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modelo da Camada 2", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Quem faz a análise editorial profunda e o veredito.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = provider == Layer2Provider.CLAUDE_SONNET_4_5,
                            onClick = {
                                scope.launch {
                                    application.settings.setLayer2Provider(Layer2Provider.CLAUDE_SONNET_4_5)
                                }
                            }
                        )
                        Text("Claude Sonnet 4.5 (recomendado)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = provider == Layer2Provider.KIMI_K2_6,
                            onClick = {
                                if (hasKimi) {
                                    scope.launch {
                                        application.settings.setLayer2Provider(Layer2Provider.KIMI_K2_6)
                                    }
                                } else status = "Configure a chave Kimi primeiro"
                            },
                            enabled = hasKimi
                        )
                        Text("Kimi K2.6 ${if (!hasKimi) "(sem chave)" else ""}")
                    }
                }
            }

            // ===== Vault Obsidian =====
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vault Obsidian", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (vault != null) "Configurado: ${vault?.lastPathSegment}"
                        else "Não configurado",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onPickVault) {
                        Text(if (vault != null) "Trocar vault" else "Escolher vault")
                    }
                    if (vault != null) {
                        Text(
                            "Capturas salvas vão pra <vault>/Lume/. Prompts customizados ficam em <vault>/lume-prompts/.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ===== Exportar sessão =====
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exportar sessão", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Empacota todas as análises num zip pronto pra subir no claude.ai e pedir meta-análise no Opus 4.7.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        enabled = !exporting,
                        onClick = {
                            exporting = true
                            scope.launch {
                                val result = application.sessionExporter.exportSession()
                                exporting = false
                                result.onSuccess { uri ->
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartilhar sessão"))
                                    status = "Sessão exportada"
                                }
                                result.onFailure {
                                    status = "Erro: ${it.message}"
                                }
                            }
                        }
                    ) {
                        if (exporting) CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        else Text("Exportar sessão")
                    }
                }
            }

            // ===== Reset chaves =====
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chaves de API", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onReconfigureKeys) {
                        Text("Reconfigurar chaves")
                    }
                }
            }

            // ===== Experimental =====
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Experimental", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = experimental,
                            onCheckedChange = { v ->
                                scope.launch { application.settings.setExperimentalRewrite(v) }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Reescrita IA do prompt")
                            Text(
                                "Antes de cada análise, deixa uma IA adaptar o prompt à captura. Pode degradar resultados em prompts SOTA.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            status?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(it, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
