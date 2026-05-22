package com.lume.app.ai.prompts

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerencia prompts do Lume.
 *
 * - Defaults vêm dos assets do APK (assets/prompts/_default.txt).
 * - Customizações do usuário ficam em arquivos .md dentro do vault Obsidian
 *   (pasta "lume-prompts/" criada automaticamente).
 * - Se vault não configurado: usa apenas defaults.
 * - Se vault configurado: na primeira leitura, copia defaults pra lume-prompts/ pra usuário editar.
 *
 * Vantagem dessa arquitetura: usuário edita os prompts ou pelo app OU diretamente
 * no Obsidian no celular. Sincroniza automaticamente.
 */
class PromptStore(
    private val context: Context,
    private val vaultUriProvider: suspend () -> Uri?
) {
    companion object {
        const val FILE_LAYER1 = "layer1.md"
        const val FILE_LAYER2 = "layer2.md"
        const val FILE_VERDICT = "verdict.md"
        const val FILE_META_SESSION = "meta_session.md"

        private const val ASSET_LAYER1 = "prompts/layer1_default.txt"
        private const val ASSET_LAYER2 = "prompts/layer2_default.txt"
        private const val ASSET_VERDICT = "prompts/verdict_default.txt"
        private const val ASSET_META_SESSION = "prompts/meta_session_default.md"

        const val PROMPTS_FOLDER = "lume-prompts"
    }

    suspend fun getLayer1Prompt(): String = readPrompt(FILE_LAYER1, ASSET_LAYER1)
    suspend fun getLayer2Prompt(): String = readPrompt(FILE_LAYER2, ASSET_LAYER2)
    suspend fun getVerdictPrompt(): String = readPrompt(FILE_VERDICT, ASSET_VERDICT)
    suspend fun getMetaSessionPrompt(): String = readPrompt(FILE_META_SESSION, ASSET_META_SESSION)

    suspend fun getDefaultLayer1(): String = readAsset(ASSET_LAYER1)
    suspend fun getDefaultLayer2(): String = readAsset(ASSET_LAYER2)
    suspend fun getDefaultVerdict(): String = readAsset(ASSET_VERDICT)
    suspend fun getDefaultMetaSession(): String = readAsset(ASSET_META_SESSION)

    suspend fun saveLayer1Prompt(content: String) = writePrompt(FILE_LAYER1, content)
    suspend fun saveLayer2Prompt(content: String) = writePrompt(FILE_LAYER2, content)
    suspend fun saveVerdictPrompt(content: String) = writePrompt(FILE_VERDICT, content)
    suspend fun saveMetaSessionPrompt(content: String) = writePrompt(FILE_META_SESSION, content)

    suspend fun resetLayer1() = deletePrompt(FILE_LAYER1)
    suspend fun resetLayer2() = deletePrompt(FILE_LAYER2)
    suspend fun resetVerdict() = deletePrompt(FILE_VERDICT)
    suspend fun resetMetaSession() = deletePrompt(FILE_META_SESSION)

    private suspend fun readPrompt(filename: String, assetPath: String): String =
        withContext(Dispatchers.IO) {
            val vaultUri = vaultUriProvider() ?: return@withContext readAsset(assetPath)

            try {
                val promptsDir = getOrCreatePromptsFolder(vaultUri) ?: return@withContext readAsset(assetPath)
                val file = promptsDir.findFile(filename)

                if (file != null && file.exists() && file.canRead()) {
                    context.contentResolver.openInputStream(file.uri)?.use { stream ->
                        return@withContext stream.bufferedReader().readText()
                    }
                }

                // Primeira vez: copia default pro vault pra usuário editar
                val default = readAsset(assetPath)
                val newFile = promptsDir.createFile("text/markdown", filename)
                newFile?.let { f ->
                    context.contentResolver.openOutputStream(f.uri)?.use { stream ->
                        stream.write(default.toByteArray())
                    }
                }
                default
            } catch (e: Exception) {
                readAsset(assetPath)
            }
        }

    private suspend fun writePrompt(filename: String, content: String) = withContext(Dispatchers.IO) {
        val vaultUri = vaultUriProvider() ?: return@withContext
        val promptsDir = getOrCreatePromptsFolder(vaultUri) ?: return@withContext

        val existing = promptsDir.findFile(filename)
        existing?.delete()

        val newFile = promptsDir.createFile("text/markdown", filename) ?: return@withContext
        context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->
            stream.write(content.toByteArray())
        }
    }

    private suspend fun deletePrompt(filename: String) = withContext(Dispatchers.IO) {
        val vaultUri = vaultUriProvider() ?: return@withContext
        val promptsDir = getOrCreatePromptsFolder(vaultUri) ?: return@withContext
        promptsDir.findFile(filename)?.delete()
    }

    private fun getOrCreatePromptsFolder(vaultUri: Uri): DocumentFile? {
        val vault = DocumentFile.fromTreeUri(context, vaultUri) ?: return null
        return vault.findFile(PROMPTS_FOLDER) ?: vault.createDirectory(PROMPTS_FOLDER)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
