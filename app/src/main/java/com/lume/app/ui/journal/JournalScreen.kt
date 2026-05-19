package com.lume.app.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lume.app.LumeApplication
import com.lume.app.data.database.CaptureEntity
import com.lume.app.ui.theme.VerdictGo
import com.lume.app.ui.theme.VerdictNoGo
import com.lume.app.ui.theme.VerdictObsolete
import com.lume.app.ui.theme.VerdictWatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    application: LumeApplication,
    onOpenEntry: (Long) -> Unit,
    onBack: () -> Unit
) {
    val captures by application.database.captureDao().observeAll()
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diário de capturas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { inner ->
        if (captures.isEmpty()) {
            Box(
                Modifier.padding(inner).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sem capturas ainda. Ative a bolha e toque pra começar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(inner).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(captures, key = { it.id }) { cap ->
                CaptureCard(cap, onClick = { onOpenEntry(cap.id) })
            }
        }
    }
}

@Composable
private fun CaptureCard(cap: CaptureEntity, onClick: () -> Unit) {
    val date = remember(cap.timestampMs) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(cap.timestampMs))
    }
    val verdictColor = when (cap.veredito) {
        "GO" -> VerdictGo
        "NO_GO" -> VerdictNoGo
        "WATCH" -> VerdictWatch
        "OBSOLETO" -> VerdictObsolete
        else -> null
    }

    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(cap.tituloTipo, style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(Modifier.weight(1f))
                if (verdictColor != null) {
                    Surface(
                        color = verdictColor,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            cap.veredito ?: "",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(cap.tituloEvocativo, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                cap.observacaoAguda,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Text(
                date,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
