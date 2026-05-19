package com.lume.app.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lume.app.LumeApplication
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult
import com.lume.app.data.database.CaptureEntity
import com.lume.app.ui.theme.VerdictGo
import com.lume.app.ui.theme.VerdictNoGo
import com.lume.app.ui.theme.VerdictObsolete
import com.lume.app.ui.theme.VerdictWatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    application: LumeApplication,
    captureId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = remember {
        Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }
    var cap by remember { mutableStateOf<CaptureEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(captureId) {
        cap = withContext(Dispatchers.IO) {
            application.database.captureDao().getById(captureId)
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cap?.tituloEvocativo ?: "Captura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    cap?.let { c ->
                        IconButton(onClick = {
                            scope.launch {
                                application.database.captureDao().setStarred(c.id, !c.starred)
                                cap = application.database.captureDao().getById(c.id)
                            }
                        }) {
                            if (c.starred) Icon(Icons.Filled.Star, contentDescription = "Star", tint = Color(0xFFB88930))
                            else Icon(Icons.Outlined.StarBorder, contentDescription = "Star")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Apagar")
                        }
                    }
                }
            )
        }
    ) { inner ->
        if (loading) {
            Box(Modifier.padding(inner).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            }
            return@Scaffold
        }
        val c = cap ?: return@Scaffold

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(c.tituloTipo, style = MaterialTheme.typography.labelMedium)
                    Text(c.tituloEvocativo, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(c.observacaoAguda, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Layer 2
            c.layer2Json?.let { l2j ->
                val l2 = remember(l2j) {
                    runCatching { json.decodeFromString<Layer2Result>(l2j) }.getOrNull()
                }
                if (l2 != null) Layer2Section(l2)
            }

            // Verdict
            c.verdictJson?.let { vj ->
                val v = remember(vj) {
                    runCatching { json.decodeFromString<VerdictResult>(vj) }.getOrNull()
                }
                if (v != null) VerdictSection(v)
            }

            // Ações
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            val layer2 = c.layer2Json?.let { runCatching { json.decodeFromString<Layer2Result>(it) }.getOrNull() }
                            val verdict = c.verdictJson?.let { runCatching { json.decodeFromString<VerdictResult>(it) }.getOrNull() }
                            val img = c.imagePath?.let { File(it).readBytes() } ?: ByteArray(0)
                            val l1 = Layer1Result(
                                tipoConteudo = c.tipoConteudo,
                                tituloTipo = c.tituloTipo,
                                tituloEvocativo = c.tituloEvocativo,
                                observacaoAguda = c.observacaoAguda,
                                valeAprofundar = c.valeAprofundar,
                                razaoNaoAprofundar = c.razaoNaoAprofundar,
                                ehTechHype = c.ehTechHype,
                                confianca = c.confiancaLayer1
                            )
                            val res = application.obsidianExporter.export(
                                l1, layer2, verdict, img, c.timestampMs, c.layer2Provider, c.userQuestion
                            )
                            res.onSuccess { status = "Exportado para Obsidian" }
                            res.onFailure { status = "Erro: ${it.message}" }
                        }
                    }
                ) {
                    Text("Salvar no Obsidian")
                }
            }

            status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            Spacer(Modifier.height(60.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar captura?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        cap?.let { application.database.captureDao().deleteById(it.id) }
                        onBack()
                    }
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Layer2Section(l2: Layer2Result) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("O que é")
            Text(l2.oQueE, style = MaterialTheme.typography.bodyLarge)

            SectionTitle("Contexto")
            Text(l2.contexto, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Camadas mais profundas")
            Text(l2.camadasMaisProfundas, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Tensões em jogo")
            Text(l2.tensoes, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Para refletir")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    l2.paraRefletir,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (l2.conexoes.isNotEmpty()) {
                SectionTitle("Conexões")
                l2.conexoes.forEach { conn ->
                    Text("• ${conn.wikilink} — ${conn.porQue}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (l2.paraIrAlem.isNotEmpty()) {
                SectionTitle("Para ir além")
                l2.paraIrAlem.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (l2.flashcards.isNotEmpty()) {
                SectionTitle("Flashcards")
                l2.flashcards.forEach { fc ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(fc.frente, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(fc.verso, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (l2.tagsObsidian.isNotEmpty()) {
                SectionTitle("Tags")
                Text(l2.tagsObsidian.joinToString(" "), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VerdictSection(v: VerdictResult) {
    val color = when (v.veredito) {
        "GO" -> VerdictGo
        "NO_GO" -> VerdictNoGo
        "WATCH" -> VerdictWatch
        "OBSOLETO" -> VerdictObsolete
        else -> Color.Gray
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        v.veredito,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            Text(v.vereditoUmaLinha, style = MaterialTheme.typography.headlineMedium)

            SectionTitle("Maturidade")
            Text("Hype Cycle: ${v.maturidade.hypeCycle}", style = MaterialTheme.typography.bodyMedium)
            Text("Wardley: ${v.maturidade.wardley}", style = MaterialTheme.typography.bodyMedium)
            Text("Crossing the Chasm: ${v.maturidade.crossingChasm}", style = MaterialTheme.typography.bodyMedium)
            Text(v.maturidade.justificativa, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Lindy")
            Text("Duração esperada: ${v.lindyAnalysis.expectativaDuracaoMeses}", style = MaterialTheme.typography.bodyMedium)
            Text(v.lindyAnalysis.justificativa, style = MaterialTheme.typography.bodyMedium)

            v.ehRebrand?.let { rb ->
                SectionTitle("É rebrand de")
                Text("${rb.nomeOriginal} ${rb.anoOriginal?.let { "($it)" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
                Text("O que mudou: ${rb.oQueMudou}", style = MaterialTheme.typography.bodyMedium)
            }

            SectionTitle("Infra ou wrapper?")
            Text("${v.ehInfraOuWrapper.classificacao} — ${v.ehInfraOuWrapper.justificativa}", style = MaterialTheme.typography.bodyMedium)

            if (v.sinaisDeSubstancia.isNotEmpty()) {
                SectionTitle("Sinais de substância")
                v.sinaisDeSubstancia.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (v.sinaisDeHype.isNotEmpty()) {
                SectionTitle("Sinais de hype")
                v.sinaisDeHype.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (v.alternativasMaisSerias.isNotEmpty()) {
                SectionTitle("Alternativas mais sérias")
                v.alternativasMaisSerias.forEach {
                    Text("• ${it.nome} — ${it.porQueMaisSeria}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionTitle("Custo de oportunidade")
            Text(v.custoOportunidade, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Recomendação")
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Text(v.recomendacaoConcreta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
            }

            SectionTitle("Quando reavaliar")
            Text(v.quandoReavaliar, style = MaterialTheme.typography.bodyMedium)

            SectionTitle("Confiança: ${v.confianca}")
            Text(v.porQueConfianca, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary
    )
}
