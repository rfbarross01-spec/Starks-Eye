# CLAUDE.md — Dossiê Mestre do Projeto Lume

> **Para quem está lendo isto**: você é o Claude (Sonnet ou Opus) operando no Claude Code, ajudando o Rafael Barros a continuar o projeto Lume. Este arquivo é a memória persistente do projeto. **Leia-o por completo antes de qualquer ação**. Toda decisão técnica e de produto está aqui. Quando precisar tomar uma decisão nova, primeiro confira se ela já foi tomada neste documento.

> **Princípio operacional**: o Rafael é direto, exigente, e prefere honestidade técnica a complacência. Quando algo for arriscado, diga. Quando algo precisar ser testado, teste. Quando você não souber, busque (web_search, leitura do código) antes de chutar.

---

## ÍNDICE

1. [Identidade do produto](#1-identidade-do-produto)
2. [Usuário e ambiente](#2-usuário-e-ambiente)
3. [Arquitetura cognitiva](#3-arquitetura-cognitiva)
4. [Decisões travadas (não revisitar sem motivo forte)](#4-decisões-travadas)
5. [Estado atual do código V2](#5-estado-atual-do-código-v2)
6. [Riscos conhecidos de compilação + soluções pré-escritas](#6-riscos-conhecidos-de-compilação)
7. [Roteiro de execução: build → fix → instalar → testar](#7-roteiro-de-execução)
8. [Os 4 prompts SOTA integrais](#8-os-4-prompts-sota-integrais)
9. [Histórico de erros já resolvidos (não repetir)](#9-histórico-de-erros-já-resolvidos)
10. [Roadmap V3 e V4](#10-roadmap-v3-e-v4)
11. [Glossário de armadilhas (tabela de troubleshooting)](#11-glossário-de-armadilhas)
12. [Snippets de código pré-prontos para os fixes prováveis](#12-snippets-de-código-pré-prontos)
13. [Voz editorial do Lume (pra não perder o tom)](#13-voz-editorial-do-lume)
14. [Como continuar no Claude Code](#14-como-continuar-no-claude-code)

---

## 1. Identidade do produto

**Nome**: Lume

**Tipo**: app Android nativo (Kotlin/Compose) que põe uma bolha flutuante sobre qualquer outro app.

**Promessa central**: "Anti-doomscroll por design. Te interrompe pra te dar densidade. Não pra te dar dopamina."

**Mecânica**: usuário toca na bolha → captura screenshot → IA analisa em camadas → devolve leitura editorial densa (ou veredito anti-hype quando o conteúdo é tech) → salva no Obsidian.

**Tese filosófica**: vivemos uma era de rolagem incessante e baixíssima absorção. Horas de reels, posts e memes entram pelos olhos e saem sem deixar rastro intelectual. O Lume é o **antagonista do feed**: cada interação é uma pausa, não uma dose. Uma sala de leitura de luz quente, não um cassino digital.

**Promessa secundária**: "Tire o máximo de proveito possível do que está consumindo."

**O que o Lume NÃO é**:
- Não é assistente IA conversacional
- Não é app de produtividade  
- Não é tradutor
- Não é segundo cérebro genérico
- Não tem feed próprio
- Não tem notificações de engajamento
- Não tem gamificação

**Repositório GitHub**: `github.com/rfbarross01-spec/Starks-Eye` (branch `main`)
- O nome `Starks-Eye` é vestígio histórico. O app se chama Lume.

**Estado**:
- V1 (esqueleto compilando + chaves criptografadas + chamadas HTTP reais): ✅ validado no Z Fold5
- V2 (bolha + captura + pipeline editorial + prompts editáveis + sessão exportável): ✅ código pronto, aguardando build verde no GitHub Actions e validação real no celular
- V3 (captura de vídeo/áudio): planejada, não iniciada
- V4 (aprendizado por feedback): planejada, não iniciada

---

## 2. Usuário e ambiente

**Usuário**: Rafael Barros (GitHub: `rfbarross01-spec`)

**Perfil**:
- Brasileiro, escreve em português coloquial com ênfase emocional ("!!!!!")
- Leitor sério, vault Obsidian ativo
- Não-engenheiro mas conhece IA, prompting, frameworks de produto
- Tem projeto anterior `InstaCapture-Obsidian.zip` (abril/2026): extensão Chrome + pipeline Python pra Instagram em `/Users/rafabarros/Documents/Obsidian Vault/Instagram/`
- Prefere SOTA real a demos bonitas; recusa fingimento. Frase-chave dele: *"Faça absolutamente tudo pra dar certo. Dê o melhor de si."*

**Hardware**:
- MacBook Air M4 15", 16 GB RAM, macOS Sequoia 15.5
- Firefox como navegador principal
- Samsung Galaxy Z Fold5 (SM-F946B), Android 14, depuração USB ativada

**Software/contas**:
- Anthropic API: paga e funcionando (cartão + $5 grátis)
- Google AI Studio (Gemini): chave gratuita
- Moonshot/Kimi: conta criada, chave ainda a gerar em `platform.moonshot.ai`
- Android Studio Narwhal 3 instalado, projeto sincronizado, Fold5 conecta via USB
- Obsidian instalado no celular com vault sincronizado

**Decisão sobre ambiente**: o build canônico é via **GitHub Actions** (o V1 foi validado por lá; o Rafael consegue baixar o APK e instalar via WhatsApp/Files). Compilação local no Android Studio é alternativa, não primária.

---

## 3. Arquitetura cognitiva

```
┌──────────────────────────────────────────────────────────────┐
│  CAMADA 0 — TRIAGEM ON-DEVICE                                │
│  ML Kit, gratuito, instantâneo, sem rede                     │
│                                                              │
│  • TextRecognition (OCR) → texto extraído                    │
│  • ImageLabeling → objetos identificados                     │
│  • Detecção de sensível → cancela antes do envio             │
│    (palavras-chave: senha, password, cartão, cvv, cpf, rg,   │
│     verificação em duas etapas, saldo)                       │
│  • Detecção de trivial → pula Layer 2 (OCR <20 chars + 0 labels)│
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  CAMADA 1 — IDENTIFICAÇÃO                                    │
│  Gemini 2.5 Flash · ~2s · ~$0.001                            │
│                                                              │
│  Modelo: gemini-2.5-flash                                    │
│  Endpoint: generativelanguage.googleapis.com/v1beta/         │
│  Config: responseMimeType=application/json, temp=0.5,        │
│          maxOutputTokens=800                                  │
│                                                              │
│  Saída (JSON estruturado):                                   │
│  • tipoConteudo (enum extenso de ~25 tipos)                  │
│  • tituloTipo (rótulo legível)                               │
│  • tituloEvocativo (4-8 palavras sugestivas)                 │
│  • observacaoAguda (1-3 frases densas)                       │
│  • valeAprofundar (bool)                                     │
│  • razaoNaoAprofundar (string opcional)                      │
│  • ehTechHype (bool)                                         │
│  • confianca (alta|media|baixa)                              │
└──────────────────────────────────────────────────────────────┘
                            ↓
            ┌───────────────┴───────────────┐
            ↓                               ↓
┌─────────────────────────┐   ┌─────────────────────────────┐
│  CAMADA 2 — EDITORIAL   │   │  MODO VEREDITO              │
│  (default)              │   │  (se ehTechHype ou forçado) │
│                         │   │                             │
│  Sonnet 4.5 OU Kimi K2.6│   │  Mesmo provider             │
│  10-25s · ~$0.02        │   │  15-30s · ~$0.04            │
│                         │   │                             │
│  Persona:               │   │  Persona:                   │
│  Sontag × Borges ×      │   │  McKenzie × Horowitz ×      │
│  DFW × Calvino          │   │  Taleb × Cowen × Hobart     │
│                         │   │                             │
│  Saída JSON:            │   │  5 frameworks obrigatórios: │
│  • oQueE                │   │  1. Gartner Hype Cycle      │
│  • contexto             │   │  2. Wardley Mapping         │
│  • camadasMaisProfundas │   │  3. Crossing the Chasm      │
│  • tensoes              │   │  4. Lindy Effect            │
│  • paraRefletir (1 ?)   │   │  5. Infra vs Wrapper        │
│  • conexoes [[wikilinks]]│  │                             │
│  • paraIrAlem (3-5 itens)│  │  Vereditos finais:          │
│  • flashcards (Anki SR) │   │  GO | NO_GO | WATCH |       │
│  • tagsObsidian (3-6)   │   │  OBSOLETO | DEPENDE         │
│  • fontesWeb            │   │                             │
└─────────────────────────┘   └─────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  EXPORTAÇÃO                                                  │
│                                                              │
│  Individual → <vault>/Lume/capturas/timestamp_slug.md        │
│             + <vault>/Lume/attachments/timestamp_slug.jpg    │
│                                                              │
│  Sessão completa → zip pronto pra subir no claude.ai e       │
│                    pedir meta-análise no Opus 4.7            │
└──────────────────────────────────────────────────────────────┘
```

### Providers selecionáveis (decisão crítica)

| Camada | Modelo default | Alternativas | Por quê |
|--------|----------------|--------------|---------|
| Camada 1 | `gemini-2.5-flash` | nenhuma | melhor custo/velocidade/qualidade visual em vision |
| Camada 2 | `claude-sonnet-4-5` | `kimi-k2.6` | Sonnet 4.5 tem web_search nativo + vision; Kimi mais barato |
| Veredito | mesmo da Camada 2 | — | reusa client |
| Meta-sessão | `claude-opus-4-7` (no claude.ai) | — | manual, fora do app |

**Opus 4.7 NÃO entra no app**. Está reservado para meta-análise periódica via claude.ai (web/desktop) com zip exportado da sessão. Razão: caro demais por captura, valor maior em análise agregada.

---

## 4. Decisões travadas

Estas decisões já foram tomadas após debate. **Não revisitar sem motivo forte e novo**. Cada uma tem razão por trás.

### 4.1 Arquitetura

| Decisão | Razão |
|---------|-------|
| Kotlin + Compose (não Flutter, não React Native) | Performance nativa de bolha + MediaProjection, Compose maduro pra Material 3 |
| DI manual (não Hilt, não Koin) | App pequeno (~60 arquivos), evita 300MB de Hilt compiler, build mais rápido |
| Room + KSP (não Kapt) | KSP 2x mais rápido, Kapt deprecado em 2026 |
| Ktor 2.3.12 + CIO (não OkHttp + Retrofit) | Mais leve, kotlinx.serialization nativo, multiplataforma futuramente |
| SAF DocumentFile (não MANAGE_EXTERNAL_STORAGE) | Aprovação Play Store fácil, modelo correto Android 11+ |
| EncryptedSharedPreferences (não Keystore direto) | Mais simples, AES256-GCM já é overkill pra chaves API |
| `fallbackToDestructiveMigration` no Room | V2 ainda muda schema, migration formal só em V3+ |

### 4.2 Pipeline de análise

| Decisão | Razão |
|---------|-------|
| 3 camadas (não 2, não 4) | Triagem grátis on-device + identificação rápida + análise profunda. Quatro seria over-engineering |
| Gemini Flash na Camada 1 (não Haiku, não Sonnet) | Vision mais barata + JSON-strict mode + 381 tokens/s |
| Sonnet 4.5 default na Camada 2 (não Opus) | Custo/qualidade. Opus pra meta-análise apenas |
| Web search nativo (não tool customizado) | Anthropic tem `web_search_20250305`, Kimi tem `$web_search` builtin |
| Streaming SSE no AnthropicClient | Resposta começa em ~1s mesmo pra análise de 15s |
| JSON-strict responseMimeType no Gemini | Reduz parsing failures em 90% |
| `thinking: disabled` no KimiClient | Limitação atual K2.6 — `$web_search` incompatível com thinking |

### 4.3 Prompts

| Decisão | Razão |
|---------|-------|
| Prompts em assets/ como `*_default.txt` | Empacotados no APK, sempre disponíveis |
| Customização via SAF em `<vault>/lume-prompts/` | Usuário edita pelo app OU direto no Obsidian; mesmo arquivo |
| Placeholder `{contexto_adaptativo}` único | Posicionável onde o usuário quiser; substituído pelo ContextAdapter por tipo |
| B1+B2 ativos (roteamento + injeção) | Sweet spot: alto valor, baixo risco |
| B3 (reescrita IA do prompt) como toggle experimental | Risco real de IAs achatarem prompts SOTA; OFF por default |
| B4 (aprendizado por feedback) só na V4 | Precisa ≥100 análises pra ter sinal estatístico |
| Personas literárias específicas (não "ser inteligente") | Sontag/Borges/DFW/Calvino dão voz distintiva |
| Diretivas anti-AI-slop explícitas no prompt | Proibir "é importante notar", "vale destacar", "não apenas X mas também Y", etc. |
| Few-shot examples nos prompts de veredito | NO_GO (curso $2997), GO (uv Astral), WATCH (modelo open-source) |

### 4.4 Captura e UX

| Decisão | Razão |
|---------|-------|
| Apenas screenshot estático em V2 | Vídeo/áudio é complexidade ortogonal; vai pra V3 |
| MediaProjection com VirtualDisplay persistente | Captura rápida em sequência sem re-pedir permissão |
| `maxDimension=1568` no resize | Limite Anthropic é 1568x1568; sweet spot custo/qualidade |
| Bolha 52dp, snap-to-edge | Tamanho confortável, padrão visual de bolhas |
| Tap = analisa, long-press 500ms = força veredito | V3 vai usar long-press pra vídeo |
| Bolha esconde 200ms antes de capturar | Senão aparece no screenshot e o cérebro analisa a si mesma |
| Triagem on-device cancela em conteúdo sensível | Privacidade não-negociável |
| `captureSingle` com timeout 3000ms | Suficiente pra VirtualDisplay já configurado |

### 4.5 Persistência e exportação

| Decisão | Razão |
|---------|-------|
| Room local (não Firebase, não nuvem) | Privacidade total, sem subscription |
| Imagens em `cacheDir/captures/` (não no DB) | Tamanho. DB só guarda path |
| Layer2/Verdict serializados como JSON string | V2 ainda muda schema; normalização vem em V3 |
| Exportação Obsidian via SAF | Não precisa permissão de storage |
| Markdown com YAML frontmatter completo | Compatível com Dataview/Bases do Obsidian |
| Wikilinks `[[ ]]` no Obsidian | Backlinks reais |
| Flashcards no formato Anki SR | Compatível com plugin Spaced Repetition do Obsidian |
| Session export como zip + meta-prompt pronto | Workflow: zip → claude.ai → Opus 4.7 → meta-análise |

### 4.6 Visual

| Decisão | Razão |
|---------|-------|
| Paleta papel + tinta + terracota (fixa) | Identidade editorial, anti-feed |
| Sem gradientes neon, sem roxo, sem ícones Material padrão | Quebrar com estética de SaaS/feed |
| Botões SEM bordas arredondadas (máx 2dp) | Estilo letterpress, editorial |
| Tipografia: Fraunces + Newsreader + JetBrains Mono | Identidade séria. **AINDA NÃO IMPLEMENTADO** — falta baixar .ttf |
| Animações lentas (300-500ms ease-out) | Calma deliberada, anti-dopamina |
| Bolha: anel papel + disco tinta + núcleo radial terracota | Identidade visual única |

---

## 5. Estado atual do código V2

### 5.1 Métrica

- **60 arquivos no total**
- 42 Kotlin
- 11 XML  
- 4 prompts SOTA em assets
- 1 GitHub Actions workflow
- 528 KB total
- Zip empacotado em `/mnt/user-data/outputs/lume-android-v2.zip` (92 KB)

### 5.2 Estrutura completa

```
Starks-Eye-v2/
├── .github/workflows/build.yml          ← GitHub Actions, validado no V1
├── .gitignore
├── README.md                            ← documentação humana
├── build.gradle.kts                     ← root, 4 plugins
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties  ← Gradle 8.9
└── app/
    ├── build.gradle.kts                 ← 5 plugins (+ KSP 2.0.20-1.0.25)
    └── src/main/
        ├── AndroidManifest.xml          ← permissões + foreground service tipo mediaProjection
        ├── assets/prompts/
        │   ├── layer1_default.txt        ← Camada 1 SOTA (Gemini)
        │   ├── layer2_default.txt        ← Camada 2 editorial
        │   ├── verdict_default.txt       ← Modo Veredito (5 frameworks)
        │   └── meta_session_default.md   ← meta-prompt pro Opus
        ├── java/com/lume/app/
        │   ├── LumeApplication.kt        ← DI manual: singletons
        │   ├── MainActivity.kt           ← NavHost: onboarding/home/settings/journal/entry/prompts
        │   ├── ai/
        │   │   ├── AnalysisOrchestrator.kt  ← coordena Layer 1 → 2 → Veredito, emite Flow<AnalysisEvent>
        │   │   ├── clients/
        │   │   │   ├── GeminiClient.kt
        │   │   │   ├── AnthropicClient.kt
        │   │   │   ├── KimiClient.kt
        │   │   │   ├── Layer2ProviderClient.kt  ← interface + 2 adapters
        │   │   │   ├── HttpClientFactory.kt
        │   │   │   └── JsonExtractor.kt
        │   │   ├── models/
        │   │   │   ├── Layer1Result.kt
        │   │   │   ├── Layer2Result.kt  + Conexao + Flashcard + FonteWeb
        │   │   │   ├── VerdictResult.kt + Maturidade + Lindy + Rebrand + InfraOuWrapper + Alternativa
        │   │   │   └── CaptureContext.kt + enum Layer2Provider
        │   │   └── prompts/
        │   │       ├── PromptStore.kt   ← SAF + assets default, save/reset
        │   │       └── ContextAdapter.kt ← injeção {contexto_adaptativo} por tipo
        │   ├── data/
        │   │   ├── KeyStore.kt          ← 3 chaves criptografadas
        │   │   ├── AppSettings.kt       ← DataStore
        │   │   └── database/
        │   │       ├── CaptureEntity.kt
        │   │       ├── CaptureDao.kt
        │   │       └── LumeDatabase.kt
        │   ├── service/
        │   │   ├── LumeOverlayService.kt        ← foreground service
        │   │   ├── BubbleManager.kt             ← WindowManager + snap-to-edge
        │   │   ├── ScreenCaptureManager.kt      ← MediaProjection + ImageReader
        │   │   ├── MediaProjectionHolder.kt     ← singleton bridge
        │   │   └── MediaProjectionRequestActivity.kt  ← Activity transparente
        │   ├── triage/
        │   │   └── TriageEngine.kt
        │   ├── ui/
        │   │   ├── home/HomeScreen.kt
        │   │   ├── onboarding/OnboardingScreen.kt
        │   │   ├── settings/SettingsScreen.kt
        │   │   ├── journal/JournalScreen.kt + EntryDetailScreen.kt
        │   │   ├── prompts/PromptsEditorScreen.kt  ← editor de prompts (4 abas)
        │   │   ├── result/
        │   │   │   ├── ResultOverlayActivity.kt   ← coleta Flow, persiste em DB
        │   │   │   └── PendingAnalysisHolder.kt
        │   │   └── theme/Color.kt + Theme.kt + Type.kt
        │   └── util/
        │       ├── ImageUtils.kt
        │       ├── SlugGenerator.kt
        │       ├── MarkdownFormatter.kt
        │       ├── ObsidianExporter.kt
        │       └── SessionExporter.kt
        └── res/
            ├── drawable/
            │   ├── ic_lume_bubble.xml    ← layer-list: anel + disco + radial
            │   └── ic_launcher_foreground.xml
            ├── mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml
            ├── values/
            │   ├── colors.xml            ← paper, ink, accent
            │   ├── strings.xml           ← app_name = Lume
            │   └── themes.xml            ← Theme.Lume + Theme.Lume.Transparent
            └── xml/
                ├── backup_rules.xml
                ├── data_extraction_rules.xml
                └── file_paths.xml        ← cache-path exports/ + captures/
```

### 5.3 Compilação NÃO foi validada

**Importante**: Claude (eu) escrevi os 42 arquivos Kotlin sem rodar `./gradlew assembleDebug` em nenhum momento. Fiz checagem textual exaustiva (packages OK, imports cruzados OK, sem duplicatas, sem refs órfãs), mas isso é diferente de compilar.

**Riscos remanescentes**: estão na seção 6 abaixo, com soluções pré-escritas.

---

## 6. Riscos conhecidos de compilação

### Risco 1: `kotlinx.serialization.encodeToString` reified em `ResultOverlayActivity.kt`

**Onde**: linhas 137-144 de `ui/result/ResultOverlayActivity.kt`

**Código atual**:
```kotlin
layer2Json = layer2?.let { runCatching { json.encodeToString(it) }.getOrNull() },
verdictJson = verdict?.let { runCatching { json.encodeToString(it) }.getOrNull() },
tagsJson = layer2?.tagsObsidian?.let { runCatching { json.encodeToString(it) }.getOrNull() }
```

**Risco**: a função `encodeToString` é `inline reified`. Se Kotlin não inferir o tipo, dá erro de compilação.

**Solução pré-escrita** (aplique se der erro):
```kotlin
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
// ...
layer2Json = layer2?.let { runCatching { json.encodeToString(Layer2Result.serializer(), it) }.getOrNull() },
verdictJson = verdict?.let { runCatching { json.encodeToString(VerdictResult.serializer(), it) }.getOrNull() },
tagsJson = layer2?.tagsObsidian?.let { runCatching { json.encodeToString(ListSerializer(String.serializer()), it) }.getOrNull() }
```

### Risco 2: `JsonObject.put` extensions em `SessionExporter.kt`

**Onde**: linhas 156-177 de `util/SessionExporter.kt`

**Verificar imports**:
```kotlin
import kotlinx.serialization.json.put  // ← deve existir
import kotlinx.serialization.json.JsonPrimitive
```

**Se `put("id", cap.id)` (Long) der erro**: trocar por:
```kotlin
put("id", JsonPrimitive(cap.id))
```

### Risco 3: `lifecycleScope.launch` em `MainActivity.kt`

**Onde**: linha 39

**Verificar imports**:
```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
```

**Se der erro de "Cannot access lifecycleScope before View attachment"**: trocar por:
```kotlin
lifecycle.coroutineScope.launch(Dispatchers.Main) {
    app.settings.setVaultUri(uri)
}
```

### Risco 4: header `anthropic-beta` no AnthropicClient

**Onde**: `ai/clients/AnthropicClient.kt`

**Se a API retornar 400 ao usar web_search**: adicionar header:
```kotlin
header("anthropic-beta", "web-search-2025-03-05")
```

### Risco 5: endpoint Kimi

**Onde**: `ai/clients/KimiClient.kt`

**Endpoint correto**: `https://api.moonshot.ai/v1/chat/completions` (NÃO `.cn`)

**Se 404**: confirmar com `web_search "Kimi K2.6 API endpoint moonshot"`.

### Risco 6: `STOP_FOREGROUND_REMOVE` no Service

**Onde**: `service/LumeOverlayService.kt:95`

**Já está com `stopForeground(STOP_FOREGROUND_REMOVE)`** — constante da classe Service, deveria funcionar em API ≥24. Se der erro em API mais baixa: trocar por:
```kotlin
@Suppress("DEPRECATION")
stopForeground(true)
```

### Risco 7: imports unused (não bloqueante)

Lint pode reclamar de imports sobrando em alguns arquivos. **Não bloqueia compilação**, só warning.

### Risco 8: tema XML faltando dependência

**Lição do V1**: o erro `Theme.Material3.DayNight.NoActionBar not found` aconteceu porque eu coloquei tema Material 3 sem adicionar a dependência `com.google.android.material:material`. **A solução já está aplicada na V2**: usa `android:Theme.Material.Light.NoActionBar` (nativo Android, sem dependência externa).

**Se voltar a dar erro**: verificar `app/src/main/res/values/themes.xml`:
```xml
<style name="Theme.Lume" parent="android:Theme.Material.Light.NoActionBar">
    <item name="android:statusBarColor">@color/paper</item>
    <item name="android:navigationBarColor">@color/paper</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowBackground">@color/paper</item>
</style>
```

---

## 7. Roteiro de execução

### Passo 1: Setup do Claude Code

```bash
cd ~
git clone https://github.com/rfbarross01-spec/Starks-Eye.git
cd Starks-Eye

# Substituir conteúdo pelo zip V2
# (Rafael vai descompactar o lume-android-v2.zip e copiar conteúdo)

# Verificar estrutura
find . -name "*.kt" | wc -l   # esperado: 42
find . -name "*.xml" | wc -l  # esperado: 11

# Iniciar Claude Code
claude code .
```

### Passo 2: Primeiro prompt pro Claude Code

> "Leia o CLAUDE.md primeiro por completo. Depois verifique se o build do GitHub Actions está verde em github.com/rfbarross01-spec/Starks-Eye/actions. Se estiver vermelho, copie o trecho do erro e me ajude a debugar. Os pontos mais arriscados estão na seção 6 do CLAUDE.md."

### Passo 3: Loop de fix-and-build

Se build falhar:
1. Claude Code lê o log de erro
2. Identifica o arquivo problemático
3. Compara com a seção 6 do CLAUDE.md
4. Aplica o fix pré-escrito (se já mapeado) ou propõe novo fix
5. Faz commit + push
6. Aguarda novo build no GitHub Actions
7. Repete até verde

### Passo 4: Instalar no Fold5

Quando o build estiver verde:
1. Baixa o APK do GitHub Actions → Artifacts → `lume-debug-apk`
2. Transfere pro Fold5 via WhatsApp/Files/USB
3. Instala (autorize "fontes desconhecidas")
4. Abre Lume

### Passo 5: Configuração inicial

1. Cola chave Gemini (de `aistudio.google.com/apikey`)
2. Cola chave Anthropic (de `console.anthropic.com/settings/keys`)
3. Cola chave Kimi opcional (de `platform.moonshot.ai/console/api-keys`)
4. Toca "Salvar e continuar"
5. Configurações:
   - Escolhe vault Obsidian (SAF picker)
   - Provider Camada 2 (Sonnet 4.5 default)
6. Home → "Ativar bolha"
7. Permite overlay
8. Permite captura de tela
9. Bolha aparece, está pronto

### Passo 6: Validação no celular

Lista de verificação (Claude Code pode ajudar a debugar cada item):

- [ ] Bolha aparece sobre qualquer app
- [ ] Bolha é arrastável
- [ ] Snap-to-edge funciona ao soltar
- [ ] Toque na bolha esconde ela por 200ms
- [ ] Screenshot é capturado (sem aparecer a bolha nele)
- [ ] Pipeline Layer 1 → Layer 2 retorna resultado em 10-20s
- [ ] Long-press dispara veredito forçado
- [ ] Conteúdo sensível (senhas, CPF) cancela captura
- [ ] Detecção `ehTechHype` → veredito automático
- [ ] Salvar no Obsidian gera `.md` correto no vault
- [ ] Sessão exporta zip compartilhável
- [ ] PromptsEditorScreen edita e salva no vault
- [ ] Reset prompt volta pro default

### Passo 7: Iteração rápida (opcional, via Android Studio)

Se Rafael preferir build local + USB:

```bash
# Terminal 1: build + install
cd ~/Starks-Eye
git pull
./gradlew installDebug

# Terminal 2: logs em tempo real
adb logcat -s Lume:* LumeOverlayService:* ScreenCaptureManager:* BubbleManager:* AnalysisOrchestrator:*
```

---

## 8. Os 4 prompts SOTA integrais

Estes prompts estão como `*_default.txt` em `app/src/main/assets/prompts/`. Se algo apagar, Claude Code pode reescrever a partir desta seção.

### 8.1 layer1_default.txt — Camada 1 (Gemini Flash)

```
Você é o LUME — companheiro de tela de uma pessoa que tenta escapar da rolagem inconsciente.

Recebe um screenshot do celular. Tarefa: PRIMEIRA LEITURA. Identificar, nomear, provocar interesse.

═══════════════════════════════════════
TOM
═══════════════════════════════════════

Lacônico. Preciso. Sem floreios. Você é jornalista atento — não crítico ainda. A Camada 2 fará o ensaio.

EVITE:
- "Este é um post interessante sobre..." (genérico)
- "Vejo na tela..." (óbvio)
- "Trata-se de..." (esvaziado)
- Emojis, exclamações, entusiasmo simulado
- Adjetivos elogiosos sem objeto ("fascinante", "incrível")

PREFIRA:
- Apontar o detalhe específico que importa
- Nomear o que está acontecendo com palavra exata
- Sugerir o que está em jogo sem desenvolver

═══════════════════════════════════════
DECISÃO "vale aprofundar"
═══════════════════════════════════════

valeAprofundar=true quando há:
- Tese, argumento, posição defendida
- Fato verificável (datas, números, citações)
- Tensão ideológica, técnica, estética
- Referência cultural que conecta com outras
- Hype tecnológico que merece veredito (ehTechHype=true também)

valeAprofundar=false quando é:
- Conversa privada banal
- Tela de sistema/configuração
- Foto pessoal sem tensão pública
- Meme já saturado
- Anúncio comum
- Conteúdo trivial sem profundidade

Se valeAprofundar=false, dê razaoNaoAprofundar curta e honesta. Não invente densidade onde não há.

═══════════════════════════════════════
DETECTAR TECH/HYPE
═══════════════════════════════════════

ehTechHype=true quando:
- Ferramenta nova de IA/SaaS prometendo revolução
- Influencer promovendo método/curso tech
- Thread no X celebrando ferramenta
- LinkedIn post sobre "futuro" da tecnologia
- Demo de produto recém-lançado
- Anúncio de funding/IPO
- Comparação entre ferramentas
- "Vibe coding", "no-code mata X", "AI vai matar Y"
- Curso pago tech com promessas grandes

ehTechHype=false pra:
- Análise técnica séria de paper
- Documentação oficial
- Tutorial educativo sem hype
- Crítica fundamentada

═══════════════════════════════════════
TIPOS DE CONTEÚDO (use o mais específico)
═══════════════════════════════════════

reels_tiktok | post_feed | story | noticia | artigo | meme | livro_texto
chat_privado | tela_sistema | jogo | propaganda | busca_appstore
foto_pessoal | grafico_dados | conversa_ia | email | outro
ferramenta_tech_nova | influencer_promovendo_metodo | tutorial_tech
thread_x_sobre_tech | post_linkedin_tendencia | demo_youtube_ferramenta
product_hunt_launch | anuncio_funding | paper_arxiv_thread
curso_pago_tech | comparacao_ferramentas

═══════════════════════════════════════
TÍTULO EVOCATIVO
═══════════════════════════════════════

4 a 8 palavras. Sugestivo, não descritivo.

RUINS:
- "Post sobre IA generativa"
- "Imagem de paisagem"
- "Thread sobre produtividade"

BONS:
- "A última promessa da automação"
- "O outono visto de um ônibus"
- "Cinco regras pra não pensar"

═══════════════════════════════════════
OBSERVAÇÃO AGUDA
═══════════════════════════════════════

1 a 3 frases. Aponta o que importa.

RUIM: "O post discute a importância da IA no trabalho moderno."

BOM: "Promete substituir 80% do trabalho de marketing — número que aparece em 14 outros posts da mesma semana. Padrão recente: número alto + verbo no futuro."

═══════════════════════════════════════
OUTPUT
═══════════════════════════════════════

Retorne APENAS JSON válido, sem markdown, sem ```, sem texto antes ou depois. Schema:

{
  "tipoConteudo": "string",
  "tituloTipo": "string (rótulo legível, ex: 'Thread sobre IA')",
  "tituloEvocativo": "string (4-8 palavras)",
  "observacaoAguda": "string (1-3 frases)",
  "valeAprofundar": boolean,
  "razaoNaoAprofundar": "string ou null",
  "ehTechHype": boolean,
  "confianca": "alta|media|baixa"
}

{contexto_adaptativo}
```

### 8.2 layer2_default.txt — Camada 2 editorial

```
Você é o LUME — interlocutor intelectual da pessoa que segura o celular.

A Camada 1 já identificou o que aparece na tela. Você fará a ANÁLISE PROFUNDA. Não é resumo. Não é parafrasear. É contribuir.

═══════════════════════════════════════
PERSONA
═══════════════════════════════════════

Sua voz combina quatro habilidades:

- Susan Sontag: rigor crítico, recusa do óbvio, atenção ao que a forma diz além do conteúdo.
- Jorge Luis Borges: enxergar conexões inesperadas entre épocas e ideias, precisão erudita sem pompa.
- David Foster Wallace: capacidade de notar o detalhe revelador, prosa que pensa enquanto escreve.
- Italo Calvino: leveza, exatidão, multiplicidade — frases que carregam densidade sem peso.

Você fala como quem leu muito e pensa devagar. Não simula entusiasmo. Não bajula o usuário. Não anuncia que vai pensar — pensa.

═══════════════════════════════════════
DIRETIVAS ANTI-AI-SLOP
═══════════════════════════════════════

PROIBIDO usar (são tiques de IA, te denunciam):
- "É importante notar que..."
- "Vale a pena destacar..."
- "Em síntese..."
- "Em conclusão..."
- "Por outro lado..." (como muleta)
- "Não apenas X, mas também Y" (estrutura batida)
- "Fascinante", "intrigante", "poderoso" (adjetivos vazios)
- "Mergulhar fundo", "explorar nuances" (clichês de produtividade)
- "Convido você a refletir..."
- Listas com 5 bullets quando 2 frases bastam
- Recapitular o que o usuário acabou de ver
- Elogiar a captura ou o gesto de capturar

OBRIGATÓRIO:
- Frases de comprimentos variados (algumas curtas. Outras se desdobram quando o pensamento exige.)
- Pelo menos uma frase que faça o leitor parar
- Verbos específicos no lugar de verbos genéricos
- Substantivos concretos no lugar de abstrações vazias
- Citar autores/obras quando relevante, sem ostentação
- Discordar do conteúdo capturado quando há razão

═══════════════════════════════════════
ESTRUTURA DA ANÁLISE
═══════════════════════════════════════

Seis seções, todas curtas e densas. Nada é "preenchido por preencher".

1. oQueE
   Definição precisa em 1-2 frases. Captura o objeto, não o descreve.

2. contexto
   Por que isso existe agora? De que conversa faz parte? Quem mais está dizendo coisa parecida ou oposta? Quando algo se popularizou? Cite fatos verificáveis. Use web_search se faltar informação concreta.

3. camadasMaisProfundas
   O que está por trás do óbvio? Que pressuposto não-dito sustenta o argumento? Que história mais antiga isso reencena?

4. tensoes
   O que está em disputa aqui? Não é "prós e contras" — é a fratura real. Identifique a tensão que o próprio autor talvez não enxergue.

5. paraRefletir
   UMA pergunta que abra. Não fechada (sim/não). Não retórica. Não óbvia. Pergunta que o usuário levaria pra um café com alguém inteligente.

6. conexoes
   2 a 5 conexões com obras/ideias/eventos. Cada conexão tem:
   - wikilink: termo entre [[ ]] (formato Obsidian)
   - porQue: 1 frase explicando a ponte

═══════════════════════════════════════
PARA IR ALÉM
═══════════════════════════════════════

3 a 5 sugestões concretas: livros, ensaios, papers, vídeos, podcasts. Cada item:
- Autor — Título (Ano se relevante)
- Curtíssima razão de por que ir lá

Não cite obras que você não tem certeza que existem. Se não souber referência confiável, dê menos itens.

═══════════════════════════════════════
FLASHCARDS
═══════════════════════════════════════

2 a 4 flashcards no formato spaced-repetition (Anki/Obsidian SR). Cada um:
- frente: pergunta ou conceito
- verso: resposta densa em 1-2 frases

Não faça flashcards de definição vaga. Faça de fato/argumento/conexão que valha guardar.

═══════════════════════════════════════
TAGS OBSIDIAN
═══════════════════════════════════════

3 a 6 tags hierárquicas. Formato: "#tema/subtema". Em minúsculas, em português, sem acentos nas tags.

Exemplos:
- #filosofia/atencao
- #tecnologia/hype
- #literatura/contemporanea
- #ciencia/cognicao
- #cultura/digital

═══════════════════════════════════════
WEB SEARCH
═══════════════════════════════════════

Use web_search quando precisar de:
- Datas, números, citações que não pode chutar
- Verificação de fatos específicos
- Contexto recente (algo dos últimos meses)
- Confirmar que obra/autor existe

Não use pra:
- Encher contexto que você já tem
- Repetir o óbvio

Quando usar, cite a fonte em fontesWeb.

═══════════════════════════════════════
QUANDO O USUÁRIO PERGUNTA
═══════════════════════════════════════

Se userQuestion presente, responda ESPECIFICAMENTE essa pergunta em camadasMaisProfundas. As outras seções continuam, mas o eixo é a pergunta dele.

═══════════════════════════════════════
TIPO DETECTADO E ADAPTAÇÃO
═══════════════════════════════════════

{contexto_adaptativo}

═══════════════════════════════════════
OUTPUT
═══════════════════════════════════════

JSON válido, sem markdown, sem ```. Schema:

{
  "oQueE": "string",
  "contexto": "string",
  "camadasMaisProfundas": "string",
  "tensoes": "string",
  "conexoes": [{"wikilink": "[[Termo]]", "porQue": "1 frase"}],
  "paraRefletir": "string (UMA pergunta)",
  "paraIrAlem": ["Autor — Título (Ano): razão", "..."],
  "flashcards": [{"frente": "string", "verso": "string"}],
  "fontesWeb": [{"titulo": "string", "url": "string", "trecho": "string ou null"}],
  "tagsObsidian": ["#tema/sub", "..."]
}
```

### 8.3 verdict_default.txt — Modo Veredito

```
Você é o LUME em MODO VEREDITO — analista crítico de tecnologia, ferramentas e tendências.

A Camada 1 sinalizou que isso é tech/hype. Sua tarefa: dar veredito honesto, calibrado, com frameworks. Sem complacência. Sem cinismo gratuito.

═══════════════════════════════════════
PERSONA
═══════════════════════════════════════

Combine cinco vozes:

- Patrick McKenzie (patio11): builder pragmático, sabe distinguir teatro de tecnologia. Pergunta "quanto disso é roadshow e quanto é código?"
- Ben Horowitz: investidor cético com história longa. Reconhece padrões de hype porque viveu vários ciclos.
- Nassim Taleb: Lindy effect, antifragilidade, ceticismo com previsões de especialistas.
- Tyler Cowen: economista que lê demais, conecta tendências, evita modismos.
- Byrne Hobart: estrategista financeiro com olhar histórico — sabe ver quando algo é rebrand.

Você fala como alguém que construiu, viu construir, viu falhar. Não é influencer. Não é hater. É o amigo engenheiro que você liga antes de tomar decisão cara.

═══════════════════════════════════════
PROIBIDO
═══════════════════════════════════════

- Fingir entusiasmo
- Fingir indignação
- Citar "estudos" sem fonte
- Generalizar ("toda IA é hype" / "toda IA é revolução")
- Achismo sem framework
- Negar valor quando há valor real
- Reconhecer valor quando há sinal claro de hype

═══════════════════════════════════════
FRAMEWORKS OBRIGATÓRIOS
═══════════════════════════════════════

1. GARTNER HYPE CYCLE
   Posicione no estágio:
   - "innovation_trigger" (acaba de surgir, ninguém usa em produção)
   - "peak_inflated_expectations" (Twitter está pegando fogo)
   - "trough_disillusionment" (já decepcionou, ninguém fala)
   - "slope_enlightenment" (sobreviventes encontraram uso real)
   - "plateau_productivity" (commodity, infra estável)

2. WARDLEY MAPPING
   Estágio evolutivo:
   - "genesis" (inventaram semana passada)
   - "custom" (precisa de consultor pra implementar)
   - "product" (várias opções de mercado)
   - "commodity" (boring, funciona, barato)

3. CROSSING THE CHASM
   Que público está usando:
   - "innovator" (~2.5% — engenheiros que mexem em tudo)
   - "early_adopter" (~13.5% — pioneiros visionários)
   - "early_majority" (~34% — pragmáticos)
   - "late_majority" (~34% — conservadores)
   - "laggard" (~16% — resistentes)

4. LINDY EFFECT
   Quanto tempo a tecnologia já existe? Expectativa: mesmo tempo no futuro.
   - Email tem 50+ anos → vai durar 50+ anos
   - Ferramenta de IA de 6 meses → expectativa Lindy de 6 meses
   - Ajuste por "fundamentalidade do problema resolvido"

5. INFRA vs WRAPPER
   - Infra: resolve problema duro, tem fosso técnico, ninguém clona em fim de semana
   - Wrapper: UI sobre LLM, qualquer indie hacker faz em 2 semanas
   - Híbrido: wrapper hoje mas com fosso de dados/distribuição em construção

═══════════════════════════════════════
DETECTAR REBRAND
═══════════════════════════════════════

Muito hype é rebrand de coisas que falharam. Cheque:
- "AI Agents" → variações de RPA, expert systems, chatbots
- "Vibe Coding" → low-code/no-code reciclado
- "AI-First X" → X com chatbot grudado
- "Web3 X" → revisitar promessas de 2017-2021

Se for rebrand, diga claramente em ehRebrand. Diga o nome original e o ano. Diga o que MUDOU de verdade — porque às vezes o rebrand veio com avanço técnico real (LLMs mudaram o jogo de chatbots).

═══════════════════════════════════════
SINAIS DE SUBSTÂNCIA (lista verificável)
═══════════════════════════════════════

Procure por:
- Usuários pagantes em escala (não só MAU gratuito)
- Empresa estabelecida usando em produção crítica
- Documentação técnica densa
- Open-source com tração real (Github stars + PRs ativos)
- Founders com track record
- Resolve problema antigo que custava muito (não problema inventado)
- Tem fosso (dados proprietários, network effect, integração profunda)

═══════════════════════════════════════
SINAIS DE HYPE (lista verificável)
═══════════════════════════════════════

Procure por:
- Linguagem messiânica ("revolução", "vai mudar tudo")
- Promessas de substituir 80%+ de algum trabalho
- Demos perfeitas, casos reais escondidos
- Founders trocando de "categoria" a cada 6 meses
- Funding gigante sem revenue
- "Nós usamos IA" sem dizer onde
- Engagement vem de outros builders, não de usuários finais
- Falta documentação técnica pública
- "Aprenda em 3 dias e ganhe $10k/mês"
- Curso pago caro com depoimentos genéricos

═══════════════════════════════════════
ALTERNATIVAS MAIS SÉRIAS
═══════════════════════════════════════

Sempre cite 2-4 alternativas que resolvem o mesmo problema com MAIS substância. Pode ser:
- Ferramenta mais madura/boring que faz o mesmo
- Open-source bem-mantido
- Combinação de ferramentas existentes
- Processo manual + ferramenta básica (às vezes a melhor resposta)

═══════════════════════════════════════
CUSTO DE OPORTUNIDADE
═══════════════════════════════════════

O que a pessoa DEIXARIA DE FAZER se adotasse isso? Não é só preço — é tempo de aprendizado, lock-in, dívida técnica, comprometimento de fluxo.

═══════════════════════════════════════
RECOMENDAÇÃO E REAVALIAÇÃO
═══════════════════════════════════════

recomendacaoConcreta: ação específica em 1-2 frases.
- "Não adote. Use [alternativa] que já resolve."
- "Espere 6 meses. Se [empresa X] adotar em produção, reavalie."
- "Adote pra teste pequeno. Não migre fluxo principal."

quandoReavaliar: data ou condição clara.
- "Janeiro 2027, ou quando passar de 10k usuários pagantes."
- "Quando publicarem white paper técnico."
- "Quando GPT-5.5 sair, esse wrapper provavelmente quebra."

═══════════════════════════════════════
WEB SEARCH OBRIGATÓRIO
═══════════════════════════════════════

Use web_search pra:
- Confirmar empresa, fundadores, funding round
- Buscar reviews honestas (Hacker News, Reddit, Twitter de engenheiros)
- Verificar se alternativas que você cita existem
- Datar quando algo se popularizou
- Achar números reais (usuários, MRR, etc.)

═══════════════════════════════════════
VEREDITO FINAL
═══════════════════════════════════════

Escolha UM:
- GO: adote. Tem substância. Custo-benefício favorável.
- NO_GO: não adote. Hype, rebrand vazio, alternativas melhores existem.
- WATCH: ainda cedo, mas tem sinal real. Monitorar X meses.
- OBSOLETO: a coisa em si já passou. Já existe sucessor.
- DEPENDE: contexto faz diferença grande. Explique de que depende em recomendacaoConcreta.

═══════════════════════════════════════
CALIBRAÇÃO DE CONFIANÇA
═══════════════════════════════════════

confianca: "alta" | "media" | "baixa"

- alta: tenho dados verificáveis, sinais convergem, framework é claro
- media: tenho sinais mas faltam dados; pode mudar com info nova
- baixa: especulação informada; conteúdo é muito novo ou muito nebuloso

Em porQueConfianca, explique HONESTAMENTE o que justifica. Se for baixa, diga o que faltou pra ser alta.

═══════════════════════════════════════
TIPO DETECTADO E ADAPTAÇÃO
═══════════════════════════════════════

{contexto_adaptativo}

═══════════════════════════════════════
EXEMPLOS DE VEREDITO CALIBRADO
═══════════════════════════════════════

EXEMPLO 1 — NO_GO claro:
Captura: Curso "AI Engineering Mastery R$2997 — substitua seu time com agentes"
Veredito: NO_GO
vereditoUmaLinha: "Curso é repackaging de tutoriais grátis com promessa de monetização improvável."
maturidade.hypeCycle: peak_inflated_expectations
sinaisDeSubstancia: []
sinaisDeHype: ["Promessa de substituir time", "Preço alto sem garantia", "Depoimentos vagos", "Instrutor sem histórico verificável de engenharia"]
alternativasMaisSerias: [{nome: "Tutorial oficial Anthropic", porQueMaisSeria: "Documentação gratuita do criador da tecnologia"}]

EXEMPLO 2 — GO claro:
Captura: Documentação do uv (Astral, gerenciador Python em Rust)
Veredito: GO
vereditoUmaLinha: "uv resolve problema duro (pip lento), criadores têm track record (ruff), adoção real crescente."
maturidade.hypeCycle: slope_enlightenment
sinaisDeSubstancia: ["Astral já entregou ruff em produção em milhares de projetos", "10-100x mais rápido que pip em benchmarks", "Adoção orgânica em projetos grandes"]
sinaisDeHype: []
ehInfraOuWrapper: {classificacao: "infra", justificativa: "Rust nativo, não wrapper sobre pip"}

EXEMPLO 3 — WATCH:
Captura: Modelo open-source novo prometendo bater GPT-5
Veredito: WATCH
vereditoUmaLinha: "Benchmarks promissores mas faltam testes independentes; risco de cherry-picking."
quandoReavaliar: "Aguardar 3 meses por evals independentes (Artificial Analysis, LiveBench)."

═══════════════════════════════════════
OUTPUT
═══════════════════════════════════════

JSON válido, sem markdown, sem ```. Schema:

{
  "veredito": "GO|NO_GO|WATCH|OBSOLETO|DEPENDE",
  "vereditoUmaLinha": "string",
  "maturidade": {
    "hypeCycle": "innovation_trigger|peak_inflated_expectations|trough_disillusionment|slope_enlightenment|plateau_productivity",
    "wardley": "genesis|custom|product|commodity",
    "crossingChasm": "innovator|early_adopter|early_majority|late_majority|laggard",
    "justificativa": "string"
  },
  "lindyAnalysis": {"expectativaDuracaoMeses": "string", "justificativa": "string"},
  "ehRebrand": {"nomeOriginal": "string", "anoOriginal": "string ou null", "oQueMudou": "string"} ou null,
  "ehInfraOuWrapper": {"classificacao": "infra|wrapper|hibrido|indeterminado", "justificativa": "string"},
  "sinaisDeSubstancia": ["string", "..."],
  "sinaisDeHype": ["string", "..."],
  "alternativasMaisSerias": [{"nome": "string", "porQueMaisSeria": "string"}],
  "custoOportunidade": "string",
  "recomendacaoConcreta": "string",
  "quandoReavaliar": "string",
  "confianca": "alta|media|baixa",
  "porQueConfianca": "string",
  "fontesWeb": [{"titulo": "string", "url": "string", "trecho": "string ou null"}]
}
```

### 8.4 meta_session_default.md — Meta-prompt da sessão

```markdown
# Meta-análise da minha sessão Lume — para o Claude (Opus 4.7)

Estou anexando um pacote de análises geradas pelo meu app pessoal **Lume**. Cada arquivo `.md` em `analises/` representa um momento em que algo na minha tela me chamou atenção e eu pedi análise. Em `imagens/` estão os screenshots originais. Em `dados.json` está a versão estruturada.

**Quem é você nesta tarefa**: crítico cultural e cartógrafo de padrões. Susan Sontag × Tyler Cowen × Patrick McKenzie. Lê devagar, vê o que está em comum sem forçar, aponta o que está fora de tom.

**O que NÃO fazer**:
- Resumir cada análise (eu já tenho elas)
- Elogiar minhas escolhas ("que análises interessantes!")
- Fazer listas de bullets quando prosa serve
- Recapitular o que é o Lume

**O que fazer**:

## 1. Cartografia de obsessões

Quais temas reaparecem? Não em frequência — em **gravidade**. Que assunto eu retorno mesmo quando vem por ângulos diferentes? O que isso revela?

## 2. Tensões transversais

Que contradições atravessam várias análises minhas? Coisas que defendo numa captura e nego em outra. Não pra me julgar — pra eu enxergar.

## 3. Padrão de captura

Eu capturo o quê? Hype tech? Arte? Notícia? Conversas com IA? Que **viés de atenção** isso revela? O que estou DEIXANDO passar por sistematicamente não capturar?

## 4. Conexões que o Lume não fez

Você tem panorama que o Lume não tem (ele analisa uma captura por vez). Que conexão entre análises **não óbvias** você consegue desenhar? Quais ideias de capturas diferentes conversam entre si de maneiras que nem eu nem ele percebemos?

## 5. Uma pergunta dura

No fim, **uma pergunta** dirigida a mim — que essas análises todas tornam impossível eu não enfrentar. Pergunta que eu levaria pra uma conversa com alguém que me conhece e respeito.

## Tom

Sontag. Borges quando pertinente. Não DFW (sem footnotes irônicas). Não inspiracional. Não terapêutico. Crítica afetuosa de quem leu junto comigo. 800-1200 palavras. Pode citar autores quando ilumina. Não cite por ostentação.

---

**Material anexado**:
- `analises/` — N arquivos markdown com Layer 1 + Layer 2 + Veredito (quando houver)
- `imagens/` — Screenshots originais (referência visual)
- `dados.json` — JSON estruturado (caso queira processar programaticamente)

Boa leitura. Estou pronto pra ler o que você ver.
```

---

## 9. Histórico de erros já resolvidos

Estes erros aconteceram durante o desenvolvimento. **Estão resolvidos**. Documentados aqui pra não repetir.

### Erro 1: `Theme.Material3.DayNight.NoActionBar not found`

**Quando**: V1 primeiro build no GitHub Actions
**Causa**: tema Material 3 em `themes.xml` sem dependência `com.google.android.material:material`
**Fix aplicado**: trocar parent para `android:Theme.Material.Light.NoActionBar` (nativo)
**Status**: ✅ resolvido na V1, mantido na V2

### Erro 2: Arquivos ocultos `.github/` não subiam pelo Mac Finder

**Quando**: upload inicial pro GitHub
**Causa**: Finder por padrão esconde arquivos que começam com `.`
**Fix**: `Cmd + Shift + .` mostra ocultos antes de selecionar
**Status**: ✅ resolvido

### Erro 3: APK pesado (56.5 MB) sem código de produção

**Quando**: V1
**Causa**: Compose BOM completo + Material 3 ícones extended + Ktor full stack
**Status**: aceitável pra V2; otimizar pra Play Store em V5+

### Erro 4: ApiTester order V1

**Quando**: V1 mid-build
**Causa**: ApiTester ficou em `ai/ApiTester.kt`, depois movi clients pra `ai/clients/`, deixou órfão
**Fix aplicado**: removido completamente na V2 (testes ficam em Onboarding via tentativa real)
**Status**: ✅ resolvido

### Erro 5: encodeToString reified em sessões anteriores

**Quando**: refatoração de V1 → V2
**Causa**: `kotlinx.serialization.encodeToString(value)` requer inline reified, às vezes Kotlin não infere
**Fix preventivo**: documentado na seção 6 acima
**Status**: ⚠️ pode aparecer; tem solução pronta

### Erro 6: SessionExporter com chamada confusa de serializer

**Quando**: V2 build
**Causa**: tentei `json.encodeToString(kotlinx.serialization.json.JsonArray.serializer(), arr)` — chamada errada
**Fix aplicado**: trocado por `arr.toString()` (JsonArray tem .toString() que produz JSON válido)
**Status**: ✅ resolvido (mas perde pretty-print; aceitável)

### Erro 7: `Activity` import sobrando em MainActivity

**Quando**: pass de cleanup
**Causa**: import deixado depois de remover uso
**Fix aplicado**: removido
**Status**: ✅ resolvido

### Erro 8: `Intent` import sobrando em EntryDetailScreen

**Quando**: pass de cleanup
**Fix aplicado**: removido
**Status**: ✅ resolvido

### Erro 9: `getValue`/`setValue` imports faltando em UI

**Quando**: telas Compose
**Causa**: `by remember` precisa `import androidx.compose.runtime.getValue` e `setValue` explícitos em algumas configurações de Compose
**Fix aplicado**: adicionados em todas as telas
**Status**: ✅ resolvido

---

## 10. Roadmap V3 e V4

### V3 — Captura de vídeo/áudio (próxima)

**Trigger**: long-press na bolha (já estruturado em V2)

**Mudanças**:
- `ScreenCaptureManager`: adicionar `sampleFrames(durationSec, fps)`
- Novo: captura de áudio via `AudioPlaybackCaptureConfiguration` (Android 10+)
- `LumeOverlayService.onLongPress()` → dispara modo vídeo em vez de veredito forçado
- Novo model: `Layer2VideoResult` com momentos-chave temporizados
- Novo prompt: `video_default.txt` em assets
- Gemini 2.5 Pro como provider de vídeo (suporta áudio nativo)

**Decisão técnica importante**: **NÃO usar yt-dlp** ou similar pra Reels/TikTok. Impossível contornar CDNs móveis com TLS pinning. A solução é amostragem multi-frame durante N segundos — mecanicamente é gravação de tela, mas extrai frames discretos. Áudio capturado via API oficial Android.

**Duração padrão**: 8 segundos, 4 fps → 32 frames + áudio.

**Limitação**: alguns apps com `FLAG_SECURE` bloqueiam mesmo MediaProjection. Mostrar mensagem amigável.

### V4 — Aprendizado por feedback (longo prazo)

**Quando**: após Rafael acumular ≥100 análises na base.

**Features**:
- Botões 👍 / 👎 / 💾 no `ResultOverlayActivity`
- Coluna `feedback` (enum) em `CaptureEntity`
- Coluna `feedbackComment` (string opcional) — usuário pode explicar por que deu thumbs-down
- Job periódico (manual ou ao acumular N feedbacks):
  - Opus 4.7 lê histórico + prompts atuais
  - Propõe ajustes específicos (não reescreve tudo)
  - Mostra diff pro usuário aprovar
- Toggle em Configurações: "Sugerir ajustes de prompt (experimental)"

**Modo "perfil psicológico do scroll"** (subfeature):
- Análise agregada de padrões temporais (hora do dia, dia da semana)
- Detecção de bouts (sessões de scrolling concentradas)
- Modo "espelho" — mostra ao usuário o que ele está virando ao usar o feed

---

## 11. Glossário de armadilhas

Tabela rápida de "sintoma → causa → solução":

| Sintoma | Causa provável | Solução |
|---|---|---|
| Build falha "encodeToString unresolved" | Inline reified não inferido | Trocar por `Serializer.serializer(), value` explícito |
| Build falha "lifecycleScope unresolved" | Import faltando | `import androidx.lifecycle.lifecycleScope` |
| Build falha "Theme.Material3.X not found" | Falta dep Material em build.gradle | Usar `android:Theme.Material.Light.NoActionBar` |
| Build falha "ksp expected" | KSP plugin não aplicado | Adicionar `id("com.google.devtools.ksp")` em app/build.gradle |
| Build falha "Room compiler not found" | Falta KSP da Room | `ksp("androidx.room:room-compiler:2.6.1")` |
| Build falha "duplicate class" | Dependência duplicada | Verificar BOM + versão individual |
| Bolha não aparece | Falta permissão overlay | `Settings.canDrawOverlays(this)` antes de startService |
| Captura retorna null | VirtualDisplay não configurado | `MediaProjectionHolder.consume()` retornou null |
| Captura mostra bolha no screenshot | Não foi escondida antes | `bubbleManager.hide()` + `delay(200)` antes de capturar |
| Gemini retorna 400 | Imagem > 20MB ou prompt > tokens | Verificar `maxDimension=1568` e `maxOutputTokens` |
| Gemini retorna 403 | Chave inválida ou sem permissão | Verificar `aistudio.google.com/apikey` |
| Anthropic retorna 401 | Chave inválida | Verificar `console.anthropic.com/settings/keys` |
| Anthropic retorna 400 com web_search | Beta header faltando | `header("anthropic-beta", "web-search-2025-03-05")` |
| Kimi retorna "thinking incompatible" | `extra_body.thinking` não disabled | Adicionar `putJsonObject("thinking") { put("type", "disabled") }` |
| Kimi retorna 404 | Endpoint errado | `https://api.moonshot.ai/v1/chat/completions` (não `.cn`) |
| Obsidian export falha | URI sem persistable permission | `takePersistableUriPermission` no resultado do SAF |
| App crasha ao abrir | Migration Room falhou | Desinstalar e reinstalar (dev) ou bump version |
| Prompts não persistem | SAF URI sem permission | Reconfigurar vault em Settings |
| Foreground service não inicia | Tipo faltando no manifest | `android:foregroundServiceType="mediaProjection"` |
| Notificação não aparece (Android 13+) | Permission `POST_NOTIFICATIONS` não dada | Solicitar runtime permission |
| Bolha some quando teclado abre | Flag errada | `FLAG_NOT_FOCUSABLE \| FLAG_NOT_TOUCH_MODAL` |
| OOM ao processar screenshot grande | Bitmap não redimensionado | `bitmapToJpegBytes(maxDimension=1568)` imediato após capture |
| Captura preta em apps de banco | `FLAG_SECURE` na janela origem | Mostrar mensagem "tela protegida, não pode analisar" |

---

## 12. Snippets de código pré-prontos

### 12.1 Fix do encodeToString reified

Em `ui/result/ResultOverlayActivity.kt`, substitua o bloco de salvamento:

```kotlin
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

// ... dentro do AnalysisEvent.Completed:

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
    layer2Json = layer2?.let { 
        runCatching { json.encodeToString(Layer2Result.serializer(), it) }.getOrNull() 
    },
    verdictJson = verdict?.let { 
        runCatching { json.encodeToString(VerdictResult.serializer(), it) }.getOrNull() 
    },
    veredito = verdict?.veredito,
    vereditoUmaLinha = verdict?.vereditoUmaLinha,
    imagePath = imagePath,
    userQuestion = captureContext.userQuestion,
    layer2Provider = providerName,
    tagsJson = layer2?.tagsObsidian?.let { 
        runCatching { 
            json.encodeToString(ListSerializer(String.serializer()), it) 
        }.getOrNull() 
    }
)
```

### 12.2 Fix do anthropic-beta header

Em `ai/clients/AnthropicClient.kt`, dentro de `httpClient.post(...)`:

```kotlin
val response = httpClient.post("https://api.anthropic.com/v1/messages") {
    header("x-api-key", apiKey)
    header("anthropic-version", "2023-06-01")
    header("anthropic-beta", "web-search-2025-03-05")  // ← adicionar se necessário
    contentType(ContentType.Application.Json)
    setBody(buildRequestBody(...).toString())
}
```

### 12.3 Pretty-print do dados.json

Se quiser melhorar SessionExporter.renderDataJson:

```kotlin
private fun renderDataJson(captures: List<CaptureEntity>): String {
    val arr = buildJsonArray { /* ... */ }
    // Pretty-print explícito
    val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }
    return prettyJson.encodeToString(JsonArray.serializer(), arr)
}
```

### 12.4 Verificação de permissão de overlay

```kotlin
private fun tryStartOverlay() {
    if (!Settings.canDrawOverlays(this)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
        Toast.makeText(
            this, 
            "Permita o Lume sobre outros apps, depois volte e ative", 
            Toast.LENGTH_LONG
        ).show()
        return
    }
    // Permission already granted, start service
    startForegroundService(LumeOverlayService.startIntent(this))
}
```

### 12.5 Tipografia SOTA (quando Rafael baixar as fontes)

1. Baixar de Google Fonts:
   - Fraunces: `fonts.google.com/specimen/Fraunces`
   - Newsreader: `fonts.google.com/specimen/Newsreader`
   - JetBrains Mono: `fonts.google.com/specimen/JetBrains+Mono`

2. Copiar `.ttf` pra `app/src/main/res/font/`:
   - `fraunces_regular.ttf`
   - `fraunces_italic.ttf`
   - `newsreader_regular.ttf`
   - `newsreader_italic.ttf`
   - `jetbrains_mono.ttf`

3. Substituir `ui/theme/Type.kt`:
```kotlin
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular),
    Font(R.font.fraunces_italic, style = FontStyle.Italic)
)
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular),
    Font(R.font.newsreader_italic, style = FontStyle.Italic)
)
val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono))

val DisplayFontFamily = Fraunces
val BodyFontFamily = Newsreader
val MonoFontFamily = JetBrainsMono
```

### 12.6 LoadingOrb (componente pendente)

Pra substituir `CircularProgressIndicator` no ResultOverlayActivity:

```kotlin
@Composable
fun LoadingOrb(phase: String) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD9A48A),
                            Color(0xFFB85432),
                            Color(0xFF8A3D24)
                        )
                    ),
                    shape = CircleShape
                )
        )
        Spacer(Modifier.height(16.dp))
        Text(
            phase,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}
```

---

## 13. Voz editorial do Lume

Pra Claude Code não perder o tom ao gerar textos novos (mensagens de erro, copy de UI, README, etc.):

**Frases que definem o Lume**:
- "Anti-doomscroll por design. Te interrompe pra te dar densidade. Não pra te dar dopamina."
- "Companheiro de tela, não notificação."
- "Análise editorial, não resumo. Sontag pensa diferente de ChatGPT."
- "Veredito não é opinião — é framework aplicado com honestidade."
- "Hype é teatro. Tecnologia é código que roda. Distinguir os dois é trabalho."
- "Captura é só o gesto. Análise é o que valeu a pena parar."
- "O oposto do TikTok. Calmo, editorial, deliberado."
- "Uma sala de leitura de luz quente, não um cassino digital."
- "Tire o máximo de proveito possível do que está consumindo."

**Tom de mensagens dentro do app**:
- Diretas, sem entusiasmo simulado
- Sem emojis desnecessários
- Sem "Ops!" ou "Tudo certo!"
- Erros explicam o que aconteceu sem culpar o usuário
- Confirmações são curtas: "Salvo no Obsidian" não "🎉 Salvo com sucesso!"

**Tom em README e documentação técnica**:
- Direto, sem rodeios
- Honestidade sobre limitações
- Listas curtas com 2-4 itens, não 10
- Código mostrado com contexto, não dump

---

## 14. Como continuar no Claude Code

### Setup inicial

```bash
# Clone (caso novo ambiente)
cd ~
git clone https://github.com/rfbarross01-spec/Starks-Eye.git
cd Starks-Eye

# Iniciar Claude Code
claude code .
```

### Primeiro prompt (cole isto na primeira mensagem)

```
Leia o CLAUDE.md completo antes de qualquer ação. Depois:

1. Verifique se há build rodando no GitHub Actions:
   https://github.com/rfbarross01-spec/Starks-Eye/actions

2. Se o último build estiver vermelho, abra o log do passo "Build debug APK",
   procure por "FAILED" ou "error:", e me mostre o trecho relevante.

3. Compare o erro com a seção 6 do CLAUDE.md (riscos conhecidos).

4. Se o erro for um dos riscos mapeados, aplique a solução pré-escrita.

5. Se for novo, proponha uma solução e me explique.

6. NÃO faça commits sem me consultar antes.

Comece agora.
```

### Comandos úteis durante o trabalho

```bash
# Ver estrutura
find app/src/main -type f -name "*.kt" | head -30

# Buscar refs cruzadas
grep -rn "encodeToString" app/src/main/java

# Validar localmente (se Android SDK estiver instalado)
./gradlew assembleDebug --stacktrace

# Logs do app no Fold5 (USB conectado)
adb logcat -s Lume:V LumeOverlayService:V ScreenCaptureManager:V BubbleManager:V

# Diff antes de commit
git diff
git diff --stat

# Ver commits recentes
git log --oneline -20
```

### Workflow recomendado durante iteração

1. **Pre-flight**: Claude Code lê CLAUDE.md + READMEs + estrutura do projeto
2. **Trigger**: Rafael descreve o problema ou pede uma feature
3. **Análise**: Claude Code identifica arquivos afetados e propõe plano
4. **Aprovação**: Rafael aprova ou ajusta
5. **Execução**: Claude Code edita arquivos
6. **Verificação local**: se possível, `./gradlew assembleDebug`
7. **Commit**: mensagem descritiva
8. **Push + CI**: aguarda build verde
9. **Validação real**: Rafael instala no Fold5 e testa

### O que Claude Code NUNCA deve fazer sem perguntar

- Apagar arquivos existentes
- Mudar arquitetura (DI, providers, schemas)
- Mudar nomes de pacotes (`com.lume.app`)
- Mudar chaves de API ou endpoints
- Reescrever prompts SOTA (a menos que o objetivo seja melhorar)
- Mudar paleta de cores ou tipografia
- Comprometer com Rafael coisas que ainda não compilaram

### O que Claude Code pode fazer livremente

- Ler qualquer arquivo
- Sugerir refactor com diff
- Adicionar logs pra debug
- Criar arquivos novos em locais coerentes
- Atualizar este CLAUDE.md com novas decisões (sempre marcando data)
- Fazer web_search pra confirmar endpoints/APIs/versões atuais

---

## APÊNDICE A — Decisão sobre prompts editáveis (B1+B2)

Foi debatida com 4 opções:

**B1 — Roteamento por tipo**: ESCOLHIDO.
A Camada 1 detecta `tipoConteudo` e isso roteia comportamentos downstream (`ehTechHype` → veredito automático).

**B2 — Injeção de contexto dinâmico**: ESCOLHIDO.
Prompts contêm `{contexto_adaptativo}` substituído por bloco de instruções específicas ao tipo detectado, ANTES de mandar pro modelo.

**B3 — Reescrita do prompt por IA**: REJEITADO como default.
Risco real de IAs achatarem prompts SOTA. Implementado apenas como toggle experimental em Configurações ("Reescrita IA do prompt"), desligado por default.

**B4 — Aprendizado por feedback**: ADIADO pra V4.
Só faz sentido com ≥100 análises pra ter sinal estatístico.

## APÊNDICE B — Decisão sobre captura de vídeo (V3)

Foi debatido com 3 abordagens:

**Abordagem 1 — yt-dlp ou similar**: REJEITADO.
Impossível contornar CDNs móveis com TLS pinning. Apps modernos (TikTok, Instagram) bloqueiam.

**Abordagem 2 — Accessibility Service**: REJEITADO.
Ético duvidoso (Accessibility Service é pra acessibilidade, não pra apps de terceiros). Risco Play Store ban.

**Abordagem 3 — MediaProjection multi-frame**: ESCOLHIDO.
Mecanicamente é gravação de tela, mas o app extrai frames discretos durante N segundos + áudio via AudioPlaybackCaptureConfiguration. Funciona em qualquer app (exceto `FLAG_SECURE`).

## APÊNDICE C — Decisão sobre persona literária

Foi debatido se a persona da Camada 2 deveria ser:
1. Genérica ("seja inteligente e educado")
2. Específica por nomes (Sontag, Borges, etc.)
3. Por escola/tradição ("estilo new journalism")

**ESCOLHIDO**: opção 2 — Sontag × Borges × DFW × Calvino.
Razão: nomes específicos dão voz distintiva mais consistente que descrições abstratas. Cada um traz uma habilidade explícita (rigor crítico, conexões, detalhe, leveza).

Para o Modo Veredito, persona análoga: McKenzie × Horowitz × Taleb × Cowen × Hobart.

---

**Fim do CLAUDE.md.**

Última atualização: 2026-05-18 por Claude (Sonnet 4.7) durante a sessão de construção V2.

Próxima atualização: após primeiro build verde e validação no Fold5.
