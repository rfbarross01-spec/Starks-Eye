package com.lume.app.ui.result

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lume.app.LumeApplication
import com.lume.app.ai.AnalysisEvent
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult
import com.lume.app.data.database.CaptureEntity
import com.lume.app.ui.theme.LumeTheme
import com.lume.app.ui.theme.VerdictGo
import com.lume.app.ui.theme.VerdictNoGo
import com.lume.app.ui.theme.VerdictObsolete
import com.lume.app.ui.theme.VerdictWatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ResultOverlayActivity : ComponentActivity() {

    private val app get() = applicationContext as LumeApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = PendingAnalysisHolder.consume()
        if (ctx == null) {
            finish()
            return
        }

        setContent {
            LumeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ResultScreen(
                        application = app,
                        captureContext = ctx,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(
    application: LumeApplication,
    captureContext: com.lume.app.ai.models.CaptureContext,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val json = remember {
        Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    }

    var phase by remember { mutableStateOf<String>("Iniciando…") }
    var layer1 by remember { mutableStateOf<Layer1Result?>(null) }
    var layer2 by remember { mutableStateOf<Layer2Result?>(null) }
    var verdict by remember { mutableStateOf<VerdictResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var skipReason by remember { mutableStateOf<String?>(null) }
    var providerName by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var savedToObsidian by remember { mutableStateOf(false) }
    var dbId by remember { mutableStateOf<Long?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Salva imagem em cache pra Histórico ter referência
        try {
            val imgDir = File(context.cacheDir, "captures").apply { mkdirs() }
            val file = File(imgDir, "${System.currentTimeMillis()}.jpg")
            file.writeBytes(captureContext.imageBytes)
            imagePath = file.absolutePath
        } catch (_: Exception) {}

        val provider = application.settings.layer2Provider.first()
        application.orchestrator.analyze(captureContext, provider).collect { event ->
            when (event) {
                is AnalysisEvent.Started -> phase = "Iniciando análise…"
                is AnalysisEvent.Layer1Started -> phase = "Identificando (Gemini)…"
                is AnalysisEvent.Layer1Done -> {
                    layer1 = event.result
                    phase = if (event.result.valeAprofundar) "Aprofundando…" else "Concluindo…"
                }
                is AnalysisEvent.Layer2Started -> {
                    providerName = event.providerName
                    phase = "Análise profunda (${event.providerName})…"
                }
                is AnalysisEvent.Layer2Done -> layer2 = event.result
                is AnalysisEvent.VerdictStarted -> {
                    providerName = event.providerName
                    phase = "Modo Veredito (${event.providerName})…"
                }
                is AnalysisEvent.VerdictDone -> verdict = event.result
                is AnalysisEvent.SkippedDeepening -> skipReason = event.reason
                is AnalysisEvent.Error -> error = event.message
                is AnalysisEvent.Completed -> {
                    done = true
                    phase = "Pronto"

                    // Salvar no banco
                    layer1?.let { l1 ->
                        val entity = CaptureEntity(
                            timestampMs = captureContext.timestampMs,
                            tipoConteudo = l1.tipoConteudo,
                            tituloTipo = l1.tituloTipo,
                            tituloEvocativo = l1.tituloEvocativo,
                            observacaoAguda = l1.observacaoAguda,
                            valeAprofundar = l1.valeAprofundar,
                            razaoNaoAprofundar = l1.razaoNaoAprofundar,
                            ehTechHype = l1.ehTechHype,
                            confiancaLayer1 = l1.confianca,
                            layer2Json = layer2?.let { runCatching { json.encodeToString(it) }.getOrNull() },
                            verdictJson = verdict?.let { runCatching { json.encodeToString(it) }.getOrNull() },
                            veredito = verdict?.veredito,
                            vereditoUmaLinha = verdict?.vereditoUmaLinha,
                            imagePath = imagePath,
                            userQuestion = captureContext.userQuestion,
                            layer2Provider = providerName,
                            tagsJson = layer2?.tagsObsidian?.let { runCatching { json.encodeToString(it) }.getOrNull() }
                        )
                        withContext(Dispatchers.IO) {
                            dbId = application.database.captureDao().insert(entity)
                        }
                        saved = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lume") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Layer 1
            layer1?.let { l1 ->
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            l1.tituloTipo,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(l1.tituloEvocativo, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(l1.observacaoAguda, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Estado de progresso enquanto não terminou
            if (!done && error == null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(phase, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            skipReason?.let {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Não aprofundei", style = MaterialTheme.typography.labelMedium)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Layer 2
            layer2?.let { l2 -> Layer2Block(l2) }

            // Verdict
            verdict?.let { v -> VerdictBlock(v) }

            error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Erro", style = MaterialTheme.typography.titleMedium)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (done && saved) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        enabled = !savedToObsidian,
                        onClick = {
                            scope.launch {
                                val l1 = layer1 ?: return@launch
                                val img = imagePath?.let { File(it).readBytes() } ?: ByteArray(0)
                                val res = application.obsidianExporter.export(
                                    l1, layer2, verdict, img,
                                    captureContext.timestampMs, providerName, captureContext.userQuestion
                                )
                                res.onSuccess { savedToObsidian = true }
                            }
                        }
                    ) {
                        Text(if (savedToObsidian) "Salvo no Obsidian ✓" else "Salvar no Obsidian")
                    }
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun Layer2Block(l2: Layer2Result) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SubTitle("O que é")
            Text(l2.oQueE, style = MaterialTheme.typography.bodyLarge)
            SubTitle("Contexto")
            Text(l2.contexto, style = MaterialTheme.typography.bodyMedium)
            SubTitle("Camadas mais profundas")
            Text(l2.camadasMaisProfundas, style = MaterialTheme.typography.bodyMedium)
            SubTitle("Tensões")
            Text(l2.tensoes, style = MaterialTheme.typography.bodyMedium)
            SubTitle("Para refletir")
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Text(l2.paraRefletir, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
            }
            if (l2.conexoes.isNotEmpty()) {
                SubTitle("Conexões")
                l2.conexoes.forEach { c -> Text("• ${c.wikilink} — ${c.porQue}", style = MaterialTheme.typography.bodyMedium) }
            }
            if (l2.paraIrAlem.isNotEmpty()) {
                SubTitle("Para ir além")
                l2.paraIrAlem.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (l2.tagsObsidian.isNotEmpty()) {
                SubTitle("Tags")
                Text(l2.tagsObsidian.joinToString(" "), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VerdictBlock(v: VerdictResult) {
    val color = when (v.veredito) {
        "GO" -> VerdictGo
        "NO_GO" -> VerdictNoGo
        "WATCH" -> VerdictWatch
        "OBSOLETO" -> VerdictObsolete
        else -> Color.Gray
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = color, shape = MaterialTheme.shapes.medium) {
                Text(
                    v.veredito, color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Text(v.vereditoUmaLinha, style = MaterialTheme.typography.headlineMedium)

            SubTitle("Maturidade")
            Text("Hype: ${v.maturidade.hypeCycle}", style = MaterialTheme.typography.bodyMedium)
            Text("Wardley: ${v.maturidade.wardley}", style = MaterialTheme.typography.bodyMedium)
            Text("Chasm: ${v.maturidade.crossingChasm}", style = MaterialTheme.typography.bodyMedium)
            Text(v.maturidade.justificativa, style = MaterialTheme.typography.bodyMedium)

            SubTitle("Lindy")
            Text("Duração esperada: ${v.lindyAnalysis.expectativaDuracaoMeses}", style = MaterialTheme.typography.bodyMedium)
            Text(v.lindyAnalysis.justificativa, style = MaterialTheme.typography.bodyMedium)

            SubTitle("Infra ou wrapper?")
            Text("${v.ehInfraOuWrapper.classificacao} — ${v.ehInfraOuWrapper.justificativa}", style = MaterialTheme.typography.bodyMedium)

            if (v.sinaisDeSubstancia.isNotEmpty()) {
                SubTitle("Substância")
                v.sinaisDeSubstancia.forEach { Text("✓ $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (v.sinaisDeHype.isNotEmpty()) {
                SubTitle("Hype")
                v.sinaisDeHype.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodyMedium) }
            }
            if (v.alternativasMaisSerias.isNotEmpty()) {
                SubTitle("Alternativas")
                v.alternativasMaisSerias.forEach {
                    Text("• ${it.nome} — ${it.porQueMaisSeria}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            SubTitle("Recomendação")
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Text(v.recomendacaoConcreta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp))
            }

            SubTitle("Quando reavaliar")
            Text(v.quandoReavaliar, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SubTitle(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
}
