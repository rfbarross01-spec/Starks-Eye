package com.lume.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lume.app.ui.theme.*

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onActivateBubble: () -> Unit,
    bubbleActive: Boolean
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "lume.",
                        style = MaterialTheme.typography.displayLarge,
                        color = Ink
                    )
                    Text(
                        text = "V. PROTÓTIPO",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkFaint
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Hero
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .width(20.dp)
                        .background(Accent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "COMPANHEIRO DE TELA",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pausa,\nantes de rolar.",
                style = MaterialTheme.typography.displayMedium,
                color = Ink
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Toque pra ativar a bolha flutuante. Ela aparece sobre qualquer app. Quando algo te chamar atenção, toque na bolha — eu identifico o que é e devolvo contexto e profundidade que você não pegaria correndo.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = InkSoft
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Botão principal
            Button(
                onClick = onActivateBubble,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bubbleActive) Accent else Ink,
                    contentColor = Paper
                )
            ) {
                Text(
                    text = if (bubbleActive) "BOLHA ATIVA · ENCERRAR" else "ATIVAR BOLHA",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Status / próximos passos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(PaperDeep)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "EM CONSTRUÇÃO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Esta é a versão inicial. As funcionalidades a seguir serão entregues em iterações:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusLine(label = "Configuração de chaves", done = true)
                    StatusLine(label = "Tema editorial Lume", done = true)
                    StatusLine(label = "Bolha flutuante (overlay)", done = false)
                    StatusLine(label = "Captura de tela (MediaProjection)", done = false)
                    StatusLine(label = "Camada 1 (Gemini)", done = false)
                    StatusLine(label = "Camada 2 (Claude + web search)", done = false)
                    StatusLine(label = "Modo Veredito", done = false)
                    StatusLine(label = "Exportação Obsidian", done = false)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, done: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (done) "✓" else "○",
            color = if (done) Success else InkFaint,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) Ink else InkFaint
        )
    }
}
