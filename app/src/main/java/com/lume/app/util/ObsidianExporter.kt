package com.lume.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult
import com.lume.app.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta análise individual como .md + imagem pro vault Obsidian.
 * Estrutura:
 *   <vault>/Lume/capturas/<timestamp>_<slug>.md
 *   <vault>/Lume/attachments/<timestamp>_<slug>.jpg
 */
class ObsidianExporter(
    private val context: Context,
    private val settings: AppSettings
) {

    suspend fun export(
        layer1: Layer1Result,
        layer2: Layer2Result?,
        verdict: VerdictResult?,
        imageBytes: ByteArray,
        timestampMs: Long,
        layer2ProviderName: String?,
        userQuestion: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val vaultUri = settings.getVaultUriOnce() ?: error("Vault não configurado")
            val vault = DocumentFile.fromTreeUri(context, vaultUri)
                ?: error("Não consegui acessar vault")

            val lumeDir = vault.findFile("Lume") ?: vault.createDirectory("Lume")
                ?: error("Não consegui criar pasta Lume")
            val capturasDir = lumeDir.findFile("capturas") ?: lumeDir.createDirectory("capturas")
                ?: error("Não consegui criar capturas/")
            val attachmentsDir = lumeDir.findFile("attachments") ?: lumeDir.createDirectory("attachments")
                ?: error("Não consegui criar attachments/")

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date(timestampMs))
            val slug = SlugGenerator.slug(layer1.tituloEvocativo)
            val baseName = "${timestamp}_$slug"
            val imageName = "$baseName.jpg"
            val mdName = "$baseName.md"

            // 1. Salva imagem
            val imageFile = attachmentsDir.createFile("image/jpeg", imageName)
                ?: error("Não consegui criar arquivo de imagem")
            context.contentResolver.openOutputStream(imageFile.uri)?.use { it.write(imageBytes) }

            // 2. Gera markdown
            val markdown = MarkdownFormatter.format(
                layer1, layer2, verdict, imageName,
                timestampMs, layer2ProviderName, userQuestion
            )

            // 3. Salva .md
            val mdFile = capturasDir.createFile("text/markdown", mdName)
                ?: error("Não consegui criar arquivo .md")
            context.contentResolver.openOutputStream(mdFile.uri)?.use { it.write(markdown.toByteArray()) }

            mdFile.uri
        }
    }
}
