package com.lume.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lume.app.data.KeyStore
import com.lume.app.ui.theme.*

@Composable
fun SettingsScreen(
    keyStore: KeyStore,
    onBack: () -> Unit,
    onResetKeys: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Ink
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // API Keys section
            SectionLabel(text = "CHAVES DE API")
            Spacer(modifier = Modifier.height(12.dp))

            KeyStatusCard(
                label = "Google Gemini",
                configured = keyStore.getGeminiKey() != null
            )

            Spacer(modifier = Modifier.height(8.dp))

            KeyStatusCard(
                label = "Anthropic Claude",
                configured = keyStore.getAnthropicKey() != null
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onResetKeys,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, Error)
            ) {
                Text(
                    text = "RECONFIGURAR CHAVES",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            SectionLabel(text = "SOBRE")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Lume v0.1.0 — protótipo. Companheiro de tela para absorver, não consumir.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .width(20.dp)
                .background(Accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Accent
        )
    }
}

@Composable
private fun KeyStatusCard(label: String, configured: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(PaperDeep)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink
            )
            Text(
                text = if (configured) "CONFIGURADA ✓" else "NÃO CONFIGURADA",
                style = MaterialTheme.typography.labelSmall,
                color = if (configured) Success else InkFaint
            )
        }
    }
}
