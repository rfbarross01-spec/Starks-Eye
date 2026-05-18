# Lume

> Companheiro de tela inteligente. Antídoto contra rolagem inconsciente.

App Android pessoal que captura screenshots e analisa com IA (Gemini + Claude), oferecendo contexto, profundidade e vereditos honestos sobre o que você consome.

## Estado atual da V1.0

Esta é a **primeira iteração funcional** com pipeline de build validado. Não é o app completo ainda — é o esqueleto que compila, instala e roda. Funcionalidades entregues:

- ✅ Tela de onboarding pedindo chaves de API
- ✅ Teste real das chaves (chamadas reais a Gemini e Anthropic, sem mock)
- ✅ Armazenamento criptografado (EncryptedSharedPreferences) das chaves
- ✅ Sistema de design editorial Lume (paleta, tipografia)
- ✅ Foreground service estruturado para overlay
- ✅ Tela home + tela de configurações funcionais
- ✅ Pipeline GitHub Actions que gera APK automaticamente

Próximas iterações vão adicionar (nessa ordem):
- ⏳ Bolha flutuante real sobre outros apps (WindowManager)
- ⏳ Captura de tela via MediaProjection
- ⏳ Camada 1 com Gemini (identificação rápida)
- ⏳ Camada 2 com Claude + web search (análise profunda)
- ⏳ Modo Veredito (GO/NO-GO para conteúdo tech)
- ⏳ Exportação Obsidian (.md com frontmatter)
- ⏳ Histórico (Room database)
- ⏳ Memória semântica + vault-awareness

## Como instalar no seu celular — 4 passos

### Passo 1: Criar repositório no GitHub e enviar este código

1. Crie uma conta no GitHub se ainda não tem (github.com — grátis)
2. Crie um repositório novo (botão `+` no topo direito → "New repository")
3. Dê um nome qualquer, ex: `lume`
4. Deixe como **Public** (necessário pra Actions gratuito) ou **Private** (precisa de Actions plan)
5. NÃO marque "Add a README" — vamos subir o próprio
6. Clique "Create repository"

Depois, no seu computador (ou direto pela interface do GitHub fazendo upload de arquivos):

```bash
cd lume-android
git init
git add .
git commit -m "primeira versão"
git branch -M main
git remote add origin https://github.com/SEU-USUARIO/lume.git
git push -u origin main
```

### Passo 2: Esperar o GitHub compilar o APK

1. Vá pra aba **"Actions"** do seu repositório
2. Você vai ver um workflow chamado "Build Android APK" rodando
3. Aguarde uns **5 a 10 minutos** (a primeira vez demora mais por causa do cache)
4. Quando aparecer um ✓ verde, clique no workflow
5. Role pra baixo até a seção **"Artifacts"**
6. Clique em **"lume-debug-apk"** pra baixar o arquivo `.zip`
7. Descompacte → você tem `app-debug.apk`

**Se der erro:** abra o workflow que falhou, role até encontrar a linha vermelha, e me mande o trecho do log com o erro. Eu corrijo.

### Passo 3: Instalar no celular

1. Transfira o arquivo `app-debug.apk` pro celular (Google Drive, cabo USB, Telegram pra você mesmo, etc.)
2. No celular, abra o gerenciador de arquivos e toque no APK
3. Na primeira vez o Android vai pedir permissão pra "Instalar apps desconhecidos" — autorize o app que vai abrir o APK (Files, Drive, etc.)
4. Confirme a instalação
5. Abra o app "Lume" do launcher

### Passo 4: Configurar as chaves de API

Na primeira abertura, o app pede 2 chaves:

**Chave Google Gemini** (grátis, gera em 30 segundos):
- Acesse https://aistudio.google.com/apikey
- Faça login com sua conta Google
- Clique "Create API key" → "Create API key in new project"
- Copie a chave (começa com `AIzaSy...`)
- Cole no campo do Lume

**Chave Anthropic** (precisa de cartão, mas tem créditos free pra testar):
- Acesse https://console.anthropic.com
- Crie conta (precisa adicionar método de pagamento, mas Anthropic dá $5 grátis pra começar)
- Vá em **Settings → API Keys**
- Clique "Create Key"
- Copie (começa com `sk-ant-...`)
- Cole no campo do Lume

Toque em **"TESTAR CHAVES E ATIVAR"**. O app faz uma chamada real a cada API pra confirmar que funciona. Se ambas estão ✓ verde, você está pronto.

## Custos esperados

Sem uso: $0/mês (chaves não custam nada paradas).

Com uso (estimativa pra próximas iterações quando análise estiver implementada):
- Gemini Flash: ~$0.001 a $0.003 por análise rápida
- Claude + web search: ~$0.05 a $0.30 por análise profunda

Para uso pessoal moderado (10-30 análises/dia): $10-50/mês total. Você controla — pode desativar a bolha quando quiser parar de gastar.

## Atestado de honestidade

Cada item abaixo é um compromisso técnico verificável:

- [x] As chaves de API são pedidas ao usuário na primeira abertura
- [x] As chaves ficam salvas em EncryptedSharedPreferences (AES256-GCM via Android Keystore)
- [x] O teste de chaves faz chamadas HTTP REAIS a Gemini e Anthropic (sem mock, sem fake)
- [x] O APK compila via GitHub Actions sem intervenção manual
- [ ] A bolha flutuante aparece sobre outros apps (iteração V2)
- [ ] A captura de tela funciona via MediaProjection (iteração V2)
- [ ] As funções analyzeLayer1, analyzeLayer2 fazem chamadas reais às APIs (iteração V3)

Você pode confirmar olhando os arquivos:
- `app/src/main/java/com/lume/app/data/KeyStore.kt` — armazenamento criptografado
- `app/src/main/java/com/lume/app/ai/ApiTester.kt` — chamadas HTTP reais (procure por `httpClient.post`)

## Estrutura do projeto

```
lume-android/
├── app/
│   ├── build.gradle.kts          # Dependências (Compose, Ktor, ML Kit)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lume/app/
│       │   ├── MainActivity.kt
│       │   ├── LumeApplication.kt
│       │   ├── data/             # KeyStore, AppSettings
│       │   ├── ai/               # Clients de API (Gemini, Anthropic)
│       │   ├── service/          # OverlayService (stub V1)
│       │   ├── ui/
│       │   │   ├── theme/        # Cores, tipografia
│       │   │   ├── onboarding/   # Tela de chaves
│       │   │   ├── home/         # Tela principal
│       │   │   └── settings/     # Configurações
│       │   └── triage/           # ML Kit on-device (iteração V2)
│       └── res/                  # Recursos Android
├── .github/workflows/
│   └── build.yml                 # CI que gera APK
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

## Problemas comuns

**"O app não abre / dá crash"**
Abra "Apps recentes" no celular, force a parada do Lume, e abra de novo. Se persistir, me mande o resultado de `adb logcat` ou abra o app pelo Android Studio pra ver o erro.

**"GitHub Actions falhou na primeira vez"**
Normal — às vezes o setup do cache demora. Vá no workflow falho e clique "Re-run all jobs". Se falhar 2 vezes seguidas, me mande o erro.

**"Já tenho conta Anthropic mas não funciona"**
Verifique se você tem créditos disponíveis em console.anthropic.com → Billing. Conta nova sem cartão tem $5 grátis mas precisa ativar.

**"Quero compilar local em vez de no GitHub"**
Precisa de Android Studio (download em developer.android.com/studio). Abra a pasta do projeto, espere Gradle sync, e clique em Run. Mas o GitHub Actions é mais simples — recomendo manter esse fluxo.

## Licença

Uso pessoal. Não publicado em loja. Não compartilhado comercialmente.
