package com.lume.app.ai.prompts

import com.lume.app.ai.models.Layer1Result

/**
 * Adapta prompts injetando contexto dinâmico baseado no que a Camada 1 identificou.
 *
 * Substitui o placeholder {contexto_adaptativo} nos prompts por instruções específicas
 * ao tipo de conteúdo detectado. Não reescreve o prompt — só adiciona contexto.
 *
 * Mantém os prompts SOTA intactos enquanto torna cada análise específica.
 */
object ContextAdapter {

    private const val PLACEHOLDER = "{contexto_adaptativo}"

    fun adaptLayer1Prompt(prompt: String, userQuestion: String? = null): String {
        val context = buildString {
            appendLine("CONTEXTO DESTA CAPTURA:")
            appendLine("- Hora: ${formatTime(System.currentTimeMillis())}")
            if (!userQuestion.isNullOrBlank()) {
                appendLine("- Usuário perguntou especificamente: \"$userQuestion\"")
                appendLine("  → priorize sinalizar se a pergunta dele indica que vale aprofundar")
            }
        }
        return prompt.replace(PLACEHOLDER, context)
    }

    fun adaptLayer2Prompt(prompt: String, layer1: Layer1Result, userQuestion: String? = null): String {
        val context = buildAdaptiveContext(layer1, userQuestion, forVerdict = false)
        return prompt.replace(PLACEHOLDER, context)
    }

    fun adaptVerdictPrompt(prompt: String, layer1: Layer1Result, userQuestion: String? = null): String {
        val context = buildAdaptiveContext(layer1, userQuestion, forVerdict = true)
        return prompt.replace(PLACEHOLDER, context)
    }

    private fun buildAdaptiveContext(layer1: Layer1Result, userQuestion: String?, forVerdict: Boolean): String =
        buildString {
            appendLine("CONTEXTO DA CAMADA 1:")
            appendLine("- Tipo detectado: ${layer1.tipoConteudo}")
            appendLine("- Rótulo legível: ${layer1.tituloTipo}")
            appendLine("- Título evocativo proposto: ${layer1.tituloEvocativo}")
            appendLine("- Primeira observação: ${layer1.observacaoAguda}")
            appendLine("- É tech/hype? ${if (layer1.ehTechHype) "sim" else "não"}")
            appendLine("- Confiança da Layer 1: ${layer1.confianca}")
            appendLine()

            if (!userQuestion.isNullOrBlank()) {
                appendLine("USUÁRIO PERGUNTOU ESPECIFICAMENTE:")
                appendLine("\"$userQuestion\"")
                appendLine("→ A análise deve responder isso explicitamente.")
                appendLine()
            }

            appendLine("INSTRUÇÕES ADAPTATIVAS POR TIPO:")
            appendLine(instructionsForType(layer1.tipoConteudo, forVerdict))

            if (forVerdict) {
                appendLine()
                appendLine("NESTA ANÁLISE EM ESPECÍFICO:")
                appendLine("- Não amenize por gentileza. Se é NO_GO, diga NO_GO.")
                appendLine("- Não condene por reflexo. Se é GO, diga GO.")
                appendLine("- A pessoa está prestes a investir tempo/dinheiro. Seu trabalho é proteger o tempo dela.")
            }
        }

    private fun instructionsForType(tipo: String, forVerdict: Boolean): String = when {
        tipo.startsWith("reels") || tipo == "story" ->
            "Vídeo curto vertical. Provavelmente tem narração + corte rápido. Atenção a: gancho inicial, promessa, call-to-action. Identifique se é educativo real ou farming de atenção."

        tipo == "thread_x_sobre_tech" || tipo == "post_linkedin_tendencia" ->
            "Thread/post de opinião tech. Cuidado com: hot take pra engagement, números chutados, certeza onde não há. ${if (forVerdict) "Aplique Hype Cycle especialmente." else "Use web_search pra confirmar fatos citados."}"

        tipo == "noticia" || tipo == "artigo" ->
            "Jornalismo. Pergunte: quem é a fonte, o que ela tem a ganhar, o que foi deixado de fora. Contexto histórico do tema é essencial."

        tipo == "livro_texto" ->
            "Trecho de livro. Identifique autor/obra se possível. Trate como interlocutor sério. Cite onde se encaixa na obra/tradição."

        tipo == "chat_privado" ->
            "Conversa pessoal capturada. SEJA DISCRETO. Analise o conteúdo SEM julgar pessoas. Não invente psicologias."

        tipo == "conversa_ia" ->
            "Conversa com outro modelo de IA. Analise: o que o usuário perguntou, o que o modelo entregou, o que poderia ser melhor. Detecte tiques de IA na resposta capturada."

        tipo == "ferramenta_tech_nova" || tipo == "product_hunt_launch" ->
            "Ferramenta nova. ${if (forVerdict) "ESSENCIAL: aplicar Infra vs Wrapper, Lindy, e buscar alternativas mais maduras. Web search pra confirmar funders, MRR, casos reais." else "Identifique categoria, alternativas existentes, qual problema real resolve."}"

        tipo == "curso_pago_tech" ->
            "Curso pago. ${if (forVerdict) "ALTA SUSPEITA. Verifique: instrutor tem track record real? O conteúdo está gratuito em outro lugar? O preço é coerente com valor entregue?" else "Identifique promessa central e se ela é realista."}"

        tipo == "anuncio_funding" ->
            "Anúncio de funding. Lembre: funding não é validação de produto. Verifique: valuation/MRR ratio, lista de investidores (smart money?), o que vão construir com o dinheiro."

        tipo == "influencer_promovendo_metodo" ->
            "Influencer com método. ${if (forVerdict) "Quase sempre NO_GO ou WATCH. Verifique: o método tem evidência fora do depoimento dele? Quem mais validou?" else "Identifique o que está sendo vendido (curso, livro, comunidade) por trás da 'lição'."}"

        tipo == "paper_arxiv_thread" ->
            "Discussão sobre paper. Importantíssimo distinguir: paper aceito em conferência peer-reviewed vs preprint. Resultados replicados vs single-shot. Sigma do achado."

        tipo == "comparacao_ferramentas" ->
            "Comparação. Cuidado com: critérios escolhidos pra favorecer ferramenta X, ausência de baseline, tempo de uso do testador, conflitos de interesse."

        tipo == "grafico_dados" ->
            "Visualização. Verifique: eixo Y começa em zero? Período cherry-picked? Fonte dos dados é citada? Correlação vs causalidade?"

        tipo == "meme" ->
            "Meme. Identifique o template, a referência cultural, o que está sendo dito por trás da forma cômica. Memes condensam ideias inteiras."

        tipo == "foto_pessoal" || tipo == "tela_sistema" ->
            "Conteúdo privado/trivial. Análise mais leve, sem forçar profundidade. Se não há tese, diga isso."

        tipo == "propaganda" || tipo == "busca_appstore" ->
            "Anúncio. Identifique a promessa, o público-alvo, o que está sendo escondido na letra miúda."

        else ->
            "Tipo: $tipo. Aplique a análise SOTA padrão sem adaptação específica."
    }

    private fun formatTime(ms: Long): String {
        val date = java.util.Date(ms)
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return fmt.format(date)
    }
}
