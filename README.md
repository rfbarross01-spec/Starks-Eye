# Lume V2 — Companheiro de tela editorial

App Android Kotlin/Compose que põe uma bolha flutuante sobre qualquer app. Toque pra capturar screenshot, ML Kit faz triagem on-device, Gemini Flash identifica, Claude Sonnet 4.5 (ou Kimi K2.6) faz análise editorial profunda ou veredito anti-hype com 5 frameworks.

**Anti-doomscroll por design**: o Lume te interrompe pra te dar densidade. Não pra te dar dopamina.

## O que mudou da V1 pra V2

**V1 era o esqueleto**: APK compilando, chaves criptografadas, chamadas HTTP reais a Gemini/Anthropic funcionando, mas sem bolha, sem captura, sem análise real.

**V2 é o app**: tudo da V1 + bolha flutuante draggable + MediaProjection + pipeline editorial em camadas + Modo Veredito com 5 frameworks + 3 providers IA selecionáveis + prompts editáveis (no app ou direto no Obsidian) + exportação Obsidian individual + zip da sessão pronto pra meta-análise no Opus 4.7.

## Arquitetura cognitiva

```
┌──────────────────────────────────────────────────────────────┐
│  Camada 0 — TRIAGEM ON-DEVICE (ML Kit, gratuito, instantâneo)│
│  • OCR (TextRecognition) extrai texto                        │
│  • Image Labeling extrai objetos                             │
│  • Detecta conteúdo sensível (senhas, CPF, etc.) e cancela   │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│  Camada 1 — IDENTIFICAÇÃO (Gemini 2.5 Flash, ~2s, ~$0.001)   │
│  • Tipo de conteúdo + título evocativo                       │
│  • Observação aguda (1-3 frases)                             │
│  • Decisão: vale aprofundar? é tech/hype?                    │
└──────────────────────────────────────────────────────────────┘
                          ↓
        ┌─────────────────┴──────────────────┐
        ↓                                    ↓
┌─────────────────┐              ┌──────────────────────┐
│  Camada 2       │              │  Modo Veredito       │
│  Análise        │              │  Análise crítica     │
│  editorial      │              │  anti-hype           │
│                 │              │                      │
│  Sontag×        │              │  McKenzie×Horowitz×  │
│  Borges×DFW×    │              │  Taleb×Cowen×Hobart  │
│  Calvino        │              │                      │
│                 │              │  5 frameworks:       │
│  • O que é      │              │  • Gartner Hype Cycle│
│  • Contexto     │              │  • Wardley Mapping   │
│  • Camadas      │              │  • Crossing the Chasm│
│  • Tensões      │              │  • Lindy Effect      │
│  • Para refletir│              │  • Infra vs Wrapper  │
│  • Conexões     │              │                      │
│  • Para ir além │              │  GO / NO_GO / WATCH /│
│  • Flashcards   │              │  OBSOLETO / DEPENDE  │
│  • Tags Obsidian│              │                      │
└─────────────────┘              └──────────────────────┘

Provider selecionável nas configurações:
  • Claude Sonnet 4.5 (default) — web_search nativo, vision
  • Kimi K2.6 — $web_search builtin tool, alternativa mais barata
```

## Como construir o APK

### Opção A — GitHub Actions (recomendada)

1. Substitua os arquivos do seu repo `rfbarross01-spec/Starks-Eye` por estes
2. Faça push pra branch `main`
3. GitHub Actions roda automaticamente (já tem workflow `.github/workflows/build.yml`)
4. Quando completar (5-8 min), baixe o APK em **Actions → último run → Artifacts → lume-debug-apk**

### Opção B — Android Studio local

1. Abra o projeto no Android Studio Narwhal+
2. Espere o sync (pode demorar 5 min na 1ª vez)
3. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
4. Ou conecte celular via USB e `Run → Run 'app'`

## Como configurar no celular

1. Instale APK (`adb install` ou abra via WhatsApp/Files)
2. Abra o Lume, cole 3 chaves no Onboarding:
   - **Gemini** (obrigatório): https://aistudio.google.com/app/apikey
   - **Anthropic** (obrigatório): https://console.anthropic.com/settings/keys
   - **Kimi** (opcional): https://platform.moonshot.ai/console/api-keys
3. Toque "Salvar e continuar"
4. Em Configurações:
   - Escolha o vault Obsidian (SAF picker)
   - Escolha provider Camada 2 (Sonnet 4.5 ou Kimi K2.6)
5. Na Home, toque **"Ativar bolha"**:
   - Sistema pedirá permissão de overlay → confirme
   - Sistema pedirá permissão de captura de tela → confirme
6. Bolha aparece sobre qualquer app
   - **Toque**: captura e analisa em camadas
   - **Mantenha pressionada (500ms)**: força Modo Veredito
   - **Arraste**: reposiciona (com snap-to-edge)

## Como editar os prompts (feature crítica V2)

**Pelo app**: Home → "Editar prompts" → escolha aba (Camada 1, Camada 2, Veredito, Meta-Sessão) → edite → "Salvar"

**Pelo Obsidian (mesmo prompt)**: abra a pasta `<seu-vault>/lume-prompts/` → edite `layer1.md`, `layer2.md`, `verdict.md`, `meta_session.md` → a próxima análise usa sua versão

**Resetar pro padrão**: dentro do editor de prompts, botão "Resetar" volta pro default empacotado no APK.

**Placeholder dinâmico**: use `{contexto_adaptativo}` em qualquer lugar do seu prompt — o Lume substitui por um bloco de instruções específicas ao tipo de conteúdo detectado pela Camada 1.

## Exportar sessão pro Opus 4.7

Em Configurações → "Exportar sessão", o Lume empacota TODAS suas análises num zip:

```
lume-sessao-YYYY-MM-DD_HH-mm.zip
├── README.md             — instruções
├── meta-prompt-opus.md   — prompt pronto pra colar no claude.ai
├── analises/
│   ├── 001_titulo.md
│   ├── 002_titulo.md
│   └── ...
├── imagens/
│   ├── 001_titulo.jpg
│   └── ...
└── dados.json            — versão estruturada
```

Suba esse zip no claude.ai (web ou desktop) usando Opus 4.7, cole o conteúdo de `meta-prompt-opus.md` como primeira mensagem. Opus faz meta-análise: cartografia de obsessões, tensões transversais, padrões que o Lume sozinho não enxerga.

## Stack técnica

- Kotlin 2.0.20 + Compose 2024.09 + Material 3
- Ktor 2.3.12 (HTTP client multiplatform)
- Room 2.6.1 + KSP (persistência local)
- ML Kit Text Recognition + Image Labeling (on-device)
- MediaProjection + ImageReader (captura de tela)
- DocumentFile + SAF (acesso ao vault Obsidian)
- EncryptedSharedPreferences (chaves criptografadas)
- kotlinx.serialization (JSON com tipagem forte)
- minSdk 29 (Android 10), targetSdk 34 (Android 14)

## Roadmap

**V3** (próxima):
- Captura de vídeo (frames + áudio) para Reels/TikTok/Stories
- Long-press na bolha já está estruturado pra isso
- Áudio via AudioPlaybackCaptureConfiguration (Android 10+)
- Gemini 2.5 Pro aceita áudio nativo
- Análise temporal: identifica momentos-chave do vídeo

**V4** (depois de acumular ~100 análises):
- Aprendizado por feedback (👍/👎/💾) ajusta prompts automaticamente
- Modo "perfil psicológico do scroll" baseado em padrões

## Notas

- **Privacidade**: imagens com OCR sensível (senhas, cartão de crédito) são detectadas e a captura é cancelada antes de qualquer envio
- **Custos**: ~$0.001 por captura simples (só Camada 1), ~$0.02 por análise profunda com web_search, ~$0.04 por veredito completo
- **Latência típica**: Camada 1 ~2s, Camada 2 ~8-15s, Veredito ~15-25s (depende de quantas web searches são feitas)

---

Lume V2 está pronto pra ser usado em produção pessoal.
