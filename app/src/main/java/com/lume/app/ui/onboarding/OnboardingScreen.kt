package com.lume.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lume.app.ai.ApiTester
import com.lume.app.ai.TestResult
import com.lume.app.data.KeyStore
import com.lume.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    keyStore: KeyStore,
    apiTester: ApiTester,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var geminiKey by remember { mutableStateOf(keyStore.getGeminiKey() ?: "") }
    var anthropicKey by remember { mutableStateOf(keyStore.getAnthropicKey() ?: "") }
    var showKeys by remember { mutableStateOf(false) }

    var geminiResult by remember { mutableStateOf<TestResult?>(null) }
    var anthropicResult by remember { mutableStateOf<TestResult?>(null) }
    var testing by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Paper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            // Header
            Text(
                text = "lume.",
                style = MaterialTheme.typography.displayLarge,
                color = Ink
            )
            Text(
                text = "CONFIGURAÇÃO INICIAL",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Intro
            Text(
                text = "Suas chaves, seu app.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = Ink
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "O Lume não tem servidor. Você usa suas próprias chaves de API e elas ficam no seu celular, encriptadas. Nada vaza pra lugar nenhum.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = InkSoft
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Gemini section
            SectionLabel(text = "1 · CHAVE GOOGLE GEMINI")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = geminiKey,
                onValueChange = {
                    geminiKey = it
                    geminiResult = null
                },
                placeholder = { Text("AIzaSy...", color = InkFaint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    cursorColor = Accent
                ),
                trailingIcon = {
                    geminiResult?.let { result ->
                        when (result) {
                            is TestResult.Success -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "OK",
                                tint = Success
                            )
                            is TestResult.Failure -> Icon(
                                Icons.Default.Error,
                                contentDescription = "Erro",
                                tint = Error
                            )
                        }
                    }
                }
            )

            (geminiResult as? TestResult.Failure)?.let {
                Text(
                    text = it.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinkRow(
                text = "Obtenha grátis em aistudio.google.com/apikey",
                onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Anthropic section
            SectionLabel(text = "2 · CHAVE ANTHROPIC")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = anthropicKey,
                onValueChange = {
                    anthropicKey = it
                    anthropicResult = null
                },
                placeholder = { Text("sk-ant-...", color = InkFaint) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Line,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                    cursorColor = Accent
                ),
                trailingIcon = {
                    anthropicResult?.let { result ->
                        when (result) {
                            is TestResult.Success -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "OK",
                                tint = Success
                            )
                            is TestResult.Failure -> Icon(
                                Icons.Default.Error,
                                contentDescription = "Erro",
                                tint = Error
                            )
                        }
                    }
                }
            )

            (anthropicResult as? TestResult.Failure)?.let {
                Text(
                    text = it.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinkRow(
                text = "Obtenha em console.anthropic.com",
                onClick = { uriHandler.openUri("https://console.anthropic.com/settings/keys") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle mostrar chaves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = showKeys,
                    onCheckedChange = { showKeys = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Accent,
                        checkedTrackColor = AccentSoft,
                        uncheckedThumbColor = InkFaint,
                        uncheckedTrackColor = PaperDeep
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mostrar chaves",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Test & save button
            Button(
                onClick = {
                    scope.launch {
                        testing = true
                        geminiResult = null
                        anthropicResult = null

                        val gResult = apiTester.testGemini(geminiKey.trim())
                        geminiResult = gResult

                        val aResult = apiTester.testAnthropic(anthropicKey.trim())
                        anthropicResult = aResult

                        testing = false

                        if (gResult is TestResult.Success && aResult is TestResult.Success) {
                            keyStore.setGeminiKey(geminiKey.trim())
                            keyStore.setAnthropicKey(anthropicKey.trim())
                            onComplete()
                        }
                    }
                },
                enabled = !testing && geminiKey.isNotBlank() && anthropicKey.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = Paper,
                    disabledContainerColor = InkFaint,
                    disabledContentColor = PaperDeep
                )
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        color = Paper,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "TESTANDO...",
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        text = "TESTAR CHAVES E ATIVAR",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Disclaimer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(PaperDeep)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "PRIVACIDADE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "As chaves são salvas no Android Keystore com criptografia AES256. Toda chamada de API sai direto do seu celular pra Google/Anthropic. O Lume não tem servidor.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = InkSoft
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(1.dp)
                .width(20.dp)
                .background(Accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Accent
        )
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(2.dp))
    ) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Accent),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Accent
            )
        }
    }
}
