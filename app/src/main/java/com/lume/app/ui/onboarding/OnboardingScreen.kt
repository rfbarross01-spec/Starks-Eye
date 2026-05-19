package com.lume.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lume.app.data.KeyStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    keyStore: KeyStore,
    onDone: () -> Unit
) {
    var gemini by remember { mutableStateOf(keyStore.getGeminiKey() ?: "") }
    var anthropic by remember { mutableStateOf(keyStore.getAnthropicKey() ?: "") }
    var kimi by remember { mutableStateOf(keyStore.getKimiKey() ?: "") }
    var showKeys by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configurar Lume") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "O Lume usa três modelos de IA. Cole suas chaves aqui — ficam só no seu celular, criptografadas.",
                style = MaterialTheme.typography.bodyMedium
            )

            KeyField(
                label = "Google Gemini (obrigatório)",
                hint = "Camada 1 — identificação rápida",
                helpUrl = "https://aistudio.google.com/app/apikey",
                value = gemini,
                onValueChange = { gemini = it },
                visible = showKeys
            )

            KeyField(
                label = "Anthropic Claude (obrigatório)",
                hint = "Camada 2 default — análise editorial profunda",
                helpUrl = "https://console.anthropic.com/settings/keys",
                value = anthropic,
                onValueChange = { anthropic = it },
                visible = showKeys
            )

            KeyField(
                label = "Moonshot Kimi (opcional)",
                hint = "Camada 2 alternativa — pra testar K2.6 vs Claude",
                helpUrl = "https://platform.moonshot.ai/console/api-keys",
                value = kimi,
                onValueChange = { kimi = it },
                visible = showKeys
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showKeys, onCheckedChange = { showKeys = it })
                Spacer(Modifier.width(8.dp))
                Text("Mostrar chaves")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (gemini.isNotBlank()) keyStore.setGeminiKey(gemini)
                    if (anthropic.isNotBlank()) keyStore.setAnthropicKey(anthropic)
                    if (kimi.isNotBlank()) keyStore.setKimiKey(kimi)
                    onDone()
                },
                enabled = gemini.isNotBlank() && anthropic.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar e continuar")
            }

            Text(
                "Pode adicionar/editar depois nas Configurações.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyField(
    label: String,
    hint: String,
    helpUrl: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(hint) },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Obter chave: $helpUrl",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}
