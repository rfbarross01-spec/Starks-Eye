package com.lume.app.util

import com.lume.app.ai.models.Layer1Result
import com.lume.app.ai.models.Layer2Result
import com.lume.app.ai.models.VerdictResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formata análise como Markdown Obsidian com frontmatter YAML completo.
 */
object MarkdownFormatter {

    fun format(
        layer1: Layer1Result,
        layer2: Layer2Result?,
        verdict: VerdictResult?,
        imageFileName: String?,
        timestampMs: Long,
        layer2ProviderName: String?,
        userQuestion: String?
    ): String = buildString {
        val date = Date(timestampMs)
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(date)
        val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

        appendLine("---")
        appendLine("title: \"${escapeYaml(layer1.tituloEvocativo)}\"")
        appendLine("date: $isoDate")
        appendLine("created: $dateOnly")
        appendLine("type: ${layer1.tipoConteudo}")
        appendLine("tipo_legivel: \"${escapeYaml(layer1.tituloTipo)}\"")
        if (verdict != null) appendLine("veredito: ${verdict.veredito}")
        if (layer2ProviderName != null) appendLine("modelo_camada2: \"$layer2ProviderName\"")
        appendLine("confianca_layer1: ${layer1.confianca}")
        appendLine("tech_hype: ${layer1.ehTechHype}")

        val tags = layer2?.tagsObsidian?.map { it.removePrefix("#") } ?: emptyList()
        if (tags.isNotEmpty()) {
            appendLine("tags:")
            tags.forEach { appendLine("  - $it") }
        }

        appendLine("source: lume")
        appendLine("---")
        appendLine()

        appendLine("# ${layer1.tituloEvocativo}")
        appendLine()

        if (imageFileName != null) {
            appendLine("![[$imageFileName]]")
            appendLine()
        }

        appendLine("> [!observation] ${layer1.tituloTipo}")
        appendLine("> ${layer1.observacaoAguda.replace("\n", "\n> ")}")
        appendLine()

        if (!userQuestion.isNullOrBlank()) {
            appendLine("**Pergunta do usuário:** $userQuestion")
            appendLine()
        }

        if (layer2 != null) {
            appendLine("## O que é")
            appendLine(layer2.oQueE)
            appendLine()

            appendLine("## Contexto")
            appendLine(layer2.contexto)
            appendLine()

            appendLine("## Camadas mais profundas")
            appendLine(layer2.camadasMaisProfundas)
            appendLine()

            appendLine("## Tensões em jogo")
            appendLine(layer2.tensoes)
            appendLine()

            if (layer2.conexoes.isNotEmpty()) {
                appendLine("## Conexões")
                layer2.conexoes.forEach { conn ->
                    appendLine("- ${conn.wikilink} — ${conn.porQue}")
                }
                appendLine()
            }

            appendLine("## Para refletir")
            appendLine("> ${layer2.paraRefletir}")
            appendLine()

            if (layer2.paraIrAlem.isNotEmpty()) {
                appendLine("## Para ir além")
                layer2.paraIrAlem.forEach { appendLine("- $it") }
                appendLine()
            }

            if (layer2.flashcards.isNotEmpty()) {
                appendLine("## Flashcards")
                appendLine()
                layer2.flashcards.forEach { card ->
                    appendLine("**${card.frente}**")
                    appendLine("?")
                    appendLine(card.verso)
                    appendLine()
                }
            }

            if (layer2.fontesWeb.isNotEmpty()) {
                appendLine("## Fontes consultadas")
                layer2.fontesWeb.forEach { src ->
                    appendLine("- [${src.titulo}](${src.url})")
                    if (!src.trecho.isNullOrBlank()) {
                        appendLine("  > ${src.trecho}")
                    }
                }
                appendLine()
            }
        }

        if (verdict != null) {
            appendLine("---")
            appendLine()
            appendLine("## ⚖️ Veredito: **${verdict.veredito}**")
            appendLine()
            appendLine("> ${verdict.vereditoUmaLinha}")
            appendLine()

            appendLine("### Maturidade")
            appendLine("- **Hype Cycle:** ${verdict.maturidade.hypeCycle}")
            appendLine("- **Wardley:** ${verdict.maturidade.wardley}")
            appendLine("- **Crossing the Chasm:** ${verdict.maturidade.crossingChasm}")
            appendLine()
            appendLine(verdict.maturidade.justificativa)
            appendLine()

            appendLine("### Lindy")
            appendLine("Expectativa de duração: ${verdict.lindyAnalysis.expectativaDuracaoMeses}")
            appendLine()
            appendLine(verdict.lindyAnalysis.justificativa)
            appendLine()

            verdict.ehRebrand?.let { rb ->
                appendLine("### É rebrand de:")
                appendLine("**${rb.nomeOriginal}** ${rb.anoOriginal?.let { "($it)" } ?: ""}")
                appendLine()
                appendLine("O que mudou: ${rb.oQueMudou}")
                appendLine()
            }

            appendLine("### Infra ou wrapper?")
            appendLine("**${verdict.ehInfraOuWrapper.classificacao}** — ${verdict.ehInfraOuWrapper.justificativa}")
            appendLine()

            if (verdict.sinaisDeSubstancia.isNotEmpty()) {
                appendLine("### Sinais de substância")
                verdict.sinaisDeSubstancia.forEach { appendLine("- ✓ $it") }
                appendLine()
            }

            if (verdict.sinaisDeHype.isNotEmpty()) {
                appendLine("### Sinais de hype")
                verdict.sinaisDeHype.forEach { appendLine("- ⚠ $it") }
                appendLine()
            }

            if (verdict.alternativasMaisSerias.isNotEmpty()) {
                appendLine("### Alternativas mais sérias")
                verdict.alternativasMaisSerias.forEach { alt ->
                    appendLine("- **${alt.nome}** — ${alt.porQueMaisSeria}")
                }
                appendLine()
            }

            appendLine("### Custo de oportunidade")
            appendLine(verdict.custoOportunidade)
            appendLine()

            appendLine("### Recomendação concreta")
            appendLine("> ${verdict.recomendacaoConcreta}")
            appendLine()

            appendLine("### Quando reavaliar")
            appendLine(verdict.quandoReavaliar)
            appendLine()

            appendLine("### Confiança no veredito: ${verdict.confianca}")
            appendLine(verdict.porQueConfianca)
            appendLine()

            if (verdict.fontesWeb.isNotEmpty()) {
                appendLine("### Fontes do veredito")
                verdict.fontesWeb.forEach { src ->
                    appendLine("- [${src.titulo}](${src.url})")
                }
                appendLine()
            }
        }

        appendLine("---")
        appendLine("*Capturado pelo Lume em $isoDate.*")
    }

    private fun escapeYaml(s: String): String =
        s.replace("\"", "\\\"").replace("\n", " ")
}
