package com.lume.app.ui.prompts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lume.app.LumeApplication
import com.lume.app.ai.prompts.PromptStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsEditorScreen(
    application: LumeApplication,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val store = application.promptStore

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Camada 1" to PromptKind.LAYER1,
        "Camada 2" to PromptKind.LAYER2,
        "Veredito" to PromptKind.VERDICT,
        "Meta-Sessão" to PromptKind.META_SESSION
    )

    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val vaultUri by application.settings.vaultUri.collectAsState(initial = null)

    // Carrega prompt atual
    LaunchedEffect(selectedTab, vaultUri) {
        loading = true
        text = readPrompt(store, tabs[selectedTab].second)
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar prompts") },
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
                .fillMaxSize()
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, (label, _) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }

            if (vaultUri == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Vault Obsidian não configurado.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Configure em Configurações → Vault. Sem vault, você só pode ver os prompts default (sem editar).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 400.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        label = { Text(tabs[selectedTab].first) },
                        enabled = vaultUri != null,
                        supportingText = {
                            Text(
                                "Use {contexto_adaptativo} pra inserir contexto dinâmico da Camada 1.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    enabled = vaultUri != null && !loading
                ) {
                    Text("Resetar")
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            savePrompt(store, tabs[selectedTab].second, text)
                            saving = false
                            status = "Salvo"
                        }
                    },
                    enabled = vaultUri != null && !loading && !saving
                ) {
                    if (saving) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    else Text("Salvar")
                }
            }

            status?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Resetar pro padrão?") },
            text = { Text("Sua versão customizada será apagada e o prompt voltará ao default do Lume.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        loading = true
                        resetPrompt(store, tabs[selectedTab].second)
                        text = readPrompt(store, tabs[selectedTab].second)
                        loading = false
                        status = "Resetado pro padrão"
                    }
                }) {
                    Text("Resetar")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar") }
            }
        )
    }
}

private enum class PromptKind { LAYER1, LAYER2, VERDICT, META_SESSION }

private suspend fun readPrompt(store: PromptStore, kind: PromptKind): String = when (kind) {
    PromptKind.LAYER1 -> store.getLayer1Prompt()
    PromptKind.LAYER2 -> store.getLayer2Prompt()
    PromptKind.VERDICT -> store.getVerdictPrompt()
    PromptKind.META_SESSION -> store.getMetaSessionPrompt()
}

private suspend fun savePrompt(store: PromptStore, kind: PromptKind, content: String) = when (kind) {
    PromptKind.LAYER1 -> store.saveLayer1Prompt(content)
    PromptKind.LAYER2 -> store.saveLayer2Prompt(content)
    PromptKind.VERDICT -> store.saveVerdictPrompt(content)
    PromptKind.META_SESSION -> store.saveMetaSessionPrompt(content)
}

private suspend fun resetPrompt(store: PromptStore, kind: PromptKind) = when (kind) {
    PromptKind.LAYER1 -> store.resetLayer1()
    PromptKind.LAYER2 -> store.resetLayer2()
    PromptKind.VERDICT -> store.resetVerdict()
    PromptKind.META_SESSION -> store.resetMetaSession()
}
