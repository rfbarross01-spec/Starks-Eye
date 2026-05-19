package com.lume.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult
import com.lume.app.ai.prompts.PromptStore
import com.lume.app.data.database.CaptureEntity
import com.lume.app.data.database.LumeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SessionExporter(
    private val context: Context,
    private val db: LumeDatabase,
    private val promptStore: PromptStore
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportSession(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val captures = db.captureDao().getAllOnce()
            if (captures.isEmpty()) error("Nenhuma análise pra exportar")

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "lume-sessao-$timestamp.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                zip.putNextEntry(ZipEntry("README.md"))
                zip.write(buildReadme(captures, timestamp).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("meta-prompt-opus.md"))
                zip.write(promptStore.getMetaSessionPrompt().toByteArray())
                zip.closeEntry()

                captures.forEachIndexed { idx, cap ->
                    val num = String.format(Locale.ROOT, "%03d", idx + 1)
                    val slug = SlugGenerator.slug(cap.tituloEvocativo)
                    val mdName = "${num}_${slug}.md"
                    val imgName = "${num}_${slug}.jpg"

                    zip.putNextEntry(ZipEntry("analises/$mdName"))
                    zip.write(renderCaptureAsMarkdown(cap, imgName).toByteArray())
                    zip.closeEntry()

                    cap.imagePath?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            zip.putNextEntry(ZipEntry("imagens/$imgName"))
                            imgFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }

                zip.putNextEntry(ZipEntry("dados.json"))
                zip.write(renderDataJson(captures).toByteArray())
                zip.closeEntry()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )
        }
    }

    private fun buildReadme(captures: List<CaptureEntity>, timestamp: String): String = buildString {
        appendLine("# Sessão Lume — $timestamp")
        appendLine()
        appendLine("Pacote de **${captures.size} análises** capturadas no app Lume.")
        appendLine()
        appendLine("## Como usar")
        appendLine()
        appendLine("1. Abra **claude.ai** (versão web/desktop)")
        appendLine("2. Crie nova conversa com modelo **Claude Opus 4.7**")
        appendLine("3. Faça upload deste pacote inteiro")
        appendLine("4. Cole o conteúdo de `meta-prompt-opus.md` como primeira mensagem")
        appendLine("5. Opus fará meta-análise das suas capturas")
        appendLine()
        appendLine("## Estrutura")
        appendLine()
        appendLine("- `meta-prompt-opus.md` — prompt pronto pra colar")
        appendLine("- `analises/` — análises em markdown (uma por captura)")
        appendLine("- `imagens/` — screenshots originais")
        appendLine("- `dados.json` — versão estruturada")
        appendLine()
        appendLine("## Estatísticas")
        val techCount = captures.count { it.ehTechHype }
        val deepCount = captures.count { it.layer2Json != null }
        val verdictCount = captures.count { it.verdictJson != null }
        appendLine("- Total: ${captures.size}")
        appendLine("- Com análise profunda: $deepCount")
        appendLine("- Tech/hype detectados: $techCount")
        appendLine("- Vereditos emitidos: $verdictCount")
        appendLine()
        appendLine("Distribuição de vereditos:")
        val verdicts = captures.mapNotNull { it.veredito }.groupingBy { it }.eachCount()
        if (verdicts.isEmpty()) appendLine("- (nenhum)")
        else verdicts.forEach { (v, c) -> appendLine("- $v: $c") }
        appendLine()
        appendLine("---")
        appendLine("*Gerado pelo Lume.*")
    }

    private fun renderCaptureAsMarkdown(cap: CaptureEntity, imgName: String): String {
        val layer2 = cap.layer2Json?.let {
            runCatching { json.decodeFromString<Layer2Result>(it) }.getOrNull()
        }
        val verdict = cap.verdictJson?.let {
            runCatching { json.decodeFromString<VerdictResult>(it) }.getOrNull()
        }

        val layer1 = Layer1Result(
            tipoConteudo = cap.tipoConteudo,
            tituloTipo = cap.tituloTipo,
            tituloEvocativo = cap.tituloEvocativo,
            observacaoAguda = cap.observacaoAguda,
            valeAprofundar = cap.valeAprofundar,
            razaoNaoAprofundar = cap.razaoNaoAprofundar,
            ehTechHype = cap.ehTechHype,
            confianca = cap.confiancaLayer1
        )

        return MarkdownFormatter.format(
            layer1 = layer1,
            layer2 = layer2,
            verdict = verdict,
            imageFileName = if (cap.imagePath != null) "../imagens/$imgName" else null,
            timestampMs = cap.timestampMs,
            layer2ProviderName = cap.layer2Provider,
            userQuestion = cap.userQuestion
        )
    }

    private fun renderDataJson(captures: List<CaptureEntity>): String {
        val arr = buildJsonArray {
            captures.forEach { cap ->
                add(buildJsonObject {
                    put("id", cap.id)
                    put("timestamp", cap.timestampMs)
                    put("tipo", cap.tipoConteudo)
                    put("titulo_legivel", cap.tituloTipo)
                    put("titulo_evocativo", cap.tituloEvocativo)
                    put("observacao", cap.observacaoAguda)
                    put("vale_aprofundar", cap.valeAprofundar)
                    put("razao_nao_aprofundar", cap.razaoNaoAprofundar ?: "")
                    put("tech_hype", cap.ehTechHype)
                    put("confianca_layer1", cap.confiancaLayer1)
                    put("user_question", cap.userQuestion ?: "")
                    put("layer2_provider", cap.layer2Provider ?: "")
                    put("veredito", cap.veredito ?: "")
                    put("veredito_uma_linha", cap.vereditoUmaLinha ?: "")
                    put("starred", cap.starred)
                    cap.layer2Json?.let { put("layer2_raw", it) }
                    cap.verdictJson?.let { put("verdict_raw", it) }
                })
            }
        }
        return arr.toString()
    }
}
