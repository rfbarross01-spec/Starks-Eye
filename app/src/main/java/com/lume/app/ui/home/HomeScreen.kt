package com.lume.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onActivateBubble: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPrompts: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Lume") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Companheiro de tela",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toque na bolha sobre qualquer app pra capturar e receber uma leitura editorial — sem doomscroll.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onActivateBubble,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = CircleShape
            ) {
                Text("Ativar bolha", style = MaterialTheme.typography.titleLarge)
            }

            Text(
                "Após ativar, a bolha fica sobre todos os apps. Toque pra analisar. Mantenha pressionada pra forçar Modo Veredito.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                onClick = onOpenJournal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Article, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Diário de capturas")
            }

            FilledTonalButton(
                onClick = onOpenPrompts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Editar prompts")
            }

            FilledTonalButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Configurações")
            }
        }
    }
}
