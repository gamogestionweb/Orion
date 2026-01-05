package com.orion.proyectoorion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.orion.proyectoorion.data.DataManager
import com.orion.proyectoorion.emergency.ScreenEmergencyMode
import com.orion.proyectoorion.models.CloudProvider
import com.orion.proyectoorion.ui.*
import com.orion.proyectoorion.ui.screens.*
import com.orion.proyectoorion.viewmodel.BrainViewModel
import com.orion.proyectoorion.vivo.ScreenVivoMode
import com.orion.proyectoorion.vivo.UserMemory
import com.orion.proyectoorion.vivo.getVivoStrings

// ==========================================================
// MAIN ACTIVITY
// ==========================================================

class MainActivity : ComponentActivity() {

    private val viewModel: BrainViewModel by viewModels()

    // StateFlow para notificar navegación a emergencia sin recrear Activity
    private val _navigateToEmergency = kotlinx.coroutines.flow.MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verificar si venimos de una notificación de emergencia
        val openEmergency = intent?.getBooleanExtra("open_emergency", false) ?: false
        if (openEmergency) {
            _navigateToEmergency.value = true
        }

        setContent {
            // Observar el estado de navegación
            val navigateToEmergency by _navigateToEmergency.collectAsState()

            OrionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OrionBlack
                ) {
                    OrionApp(
                        viewModel = viewModel,
                        navigateToEmergency = navigateToEmergency,
                        onEmergencyNavigationHandled = { _navigateToEmergency.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // En vez de recrear, solo señalamos que hay que navegar
        if (intent.getBooleanExtra("open_emergency", false)) {
            _navigateToEmergency.value = true
        }
    }
}

// ==========================================================
// ORION THEME
// ==========================================================

@Composable
fun OrionTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = OrionPurple,
        secondary = OrionBlue,
        tertiary = OrionGreen,
        background = OrionBlack,
        surface = OrionDarkGrey,
        onPrimary = OrionText,
        onSecondary = OrionText,
        onTertiary = OrionText,
        onBackground = OrionText,
        onSurface = OrionText,
        error = OrionRed
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ==========================================================
// NAVEGACIÓN
// ==========================================================

enum class Screen {
    LANGUAGE,
    ENGINE_SELECT,
    CLOUD_PROVIDERS,
    PROVIDER_MODELS,
    LOCAL_MODELS,
    SETUP_API,
    CHAT,
    MEMORY,
    VIVO,
    EMERGENCY
}

// ==========================================================
// ORION APP
// ==========================================================

@Composable
fun OrionApp(
    viewModel: BrainViewModel,
    navigateToEmergency: Boolean = false,
    onEmergencyNavigationHandled: () -> Unit = {}
) {
    val ctx = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.LANGUAGE) }
    var selectedProvider by remember { mutableStateOf<CloudProvider?>(null) }
    var showMemory by remember { mutableStateOf(false) }
    var cameFromVivo by remember { mutableStateOf(false) }
    var returnToVivo by remember { mutableStateOf(false) }

    // Manejar navegación a emergencia desde notificación (sin recrear Activity)
    LaunchedEffect(navigateToEmergency) {
        if (navigateToEmergency) {
            currentScreen = Screen.EMERGENCY
            onEmergencyNavigationHandled()
        }
    }

    // Inicialización
    LaunchedEffect(Unit) {
        viewModel.init(ctx)

        // Determinar pantalla inicial (ya no dependemos de openEmergencyFromNotification aquí)
        if (viewModel.isLanguageConfigured()) {
            if (viewModel.restoreState(ctx)) {
                currentScreen = Screen.CHAT
            } else {
                currentScreen = Screen.ENGINE_SELECT
            }
        }
    }

    // Back handler
    BackHandler(enabled = currentScreen != Screen.LANGUAGE) {
        when {
            showMemory -> showMemory = false
            currentScreen == Screen.CHAT -> {
                currentScreen = if (viewModel.isLocalMode) Screen.LOCAL_MODELS else Screen.CLOUD_PROVIDERS
                viewModel.startNewChat()
            }
            currentScreen == Screen.LOCAL_MODELS -> currentScreen = Screen.ENGINE_SELECT
            currentScreen == Screen.SETUP_API -> currentScreen = Screen.CLOUD_PROVIDERS
            currentScreen == Screen.PROVIDER_MODELS -> currentScreen = Screen.CLOUD_PROVIDERS
            currentScreen == Screen.CLOUD_PROVIDERS -> currentScreen = Screen.ENGINE_SELECT
            currentScreen == Screen.VIVO -> currentScreen = Screen.ENGINE_SELECT
            currentScreen == Screen.EMERGENCY -> currentScreen = Screen.ENGINE_SELECT
            currentScreen == Screen.MEMORY -> {
                if (returnToVivo) {
                    returnToVivo = false
                    currentScreen = Screen.VIVO
                } else {
                    currentScreen = Screen.CHAT
                }
            }
            currentScreen == Screen.ENGINE_SELECT -> { /* No hacer nada */ }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(OrionBlack)
    ) {
        when (currentScreen) {
            Screen.LANGUAGE -> {
                ScreenLanguage(
                    onLanguageSelected = { code ->
                        viewModel.setLang(code)
                        currentScreen = Screen.ENGINE_SELECT
                    }
                )
            }

            Screen.ENGINE_SELECT -> {
                ScreenEngineSelect(
                    viewModel = viewModel,
                    onCloudSelected = {
                        viewModel.selectEngine(false)
                        currentScreen = Screen.CLOUD_PROVIDERS
                    },
                    onLocalSelected = {
                        viewModel.selectEngine(true)
                        currentScreen = Screen.LOCAL_MODELS
                    },
                    onVivoSelected = {
                        val dm = DataManager(ctx)
                        val openaiKey = dm.getApiKey(CloudProvider.OPENAI)
                        if (!openaiKey.isNullOrBlank()) {
                            currentScreen = Screen.VIVO
                        } else {
                            cameFromVivo = true
                            selectedProvider = CloudProvider.OPENAI
                            viewModel.selectCloudProvider(CloudProvider.OPENAI)
                            currentScreen = Screen.SETUP_API
                        }
                    },
                    onEmergencySelected = {
                        currentScreen = Screen.EMERGENCY
                    }
                )
            }

            Screen.CLOUD_PROVIDERS -> {
                ScreenCloudProviders(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.ENGINE_SELECT },
                    onProviderSelected = { provider ->
                        selectedProvider = provider
                        viewModel.selectCloudProvider(provider)
                        if (viewModel.hasApiKeyFor(provider)) {
                            currentScreen = Screen.CHAT
                        } else {
                            currentScreen = Screen.SETUP_API
                        }
                    },
                    onSelectModels = { provider ->
                        selectedProvider = provider
                        currentScreen = Screen.PROVIDER_MODELS
                    }
                )
            }

            Screen.PROVIDER_MODELS -> {
                selectedProvider?.let { provider ->
                    ScreenProviderModels(
                        viewModel = viewModel,
                        provider = provider,
                        onBack = { currentScreen = Screen.CLOUD_PROVIDERS },
                        onModelSelected = { model ->
                            viewModel.selectCloudModel(provider, model)
                        }
                    )
                }
            }

            Screen.SETUP_API -> {
                selectedProvider?.let { provider ->
                    ScreenSetupApi(
                        viewModel = viewModel,
                        provider = provider,
                        onBack = {
                            cameFromVivo = false
                            currentScreen = Screen.CLOUD_PROVIDERS
                        },
                        onApiConfigured = {
                            if (cameFromVivo && provider == CloudProvider.OPENAI) {
                                cameFromVivo = false
                                currentScreen = Screen.VIVO
                            } else {
                                currentScreen = Screen.CHAT
                            }
                        }
                    )
                }
            }

            Screen.LOCAL_MODELS -> {
                ScreenLocalModels(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.ENGINE_SELECT },
                    onModelReady = {
                        currentScreen = Screen.CHAT
                    }
                )
            }

            Screen.CHAT -> {
                ScreenChat(
                    viewModel = viewModel,
                    showMemory = showMemory,
                    onToggleMemory = { showMemory = !showMemory },
                    onChangeEngine = {
                        currentScreen = if (viewModel.isLocalMode) Screen.LOCAL_MODELS else Screen.CLOUD_PROVIDERS
                    },
                    onLogout = {
                        viewModel.logout()
                        showMemory = false
                        currentScreen = Screen.ENGINE_SELECT
                    }
                )
            }

            Screen.MEMORY -> {
                ScreenMemory(
                    viewModel = viewModel,
                    onBack = {
                        if (returnToVivo) {
                            returnToVivo = false
                            currentScreen = Screen.VIVO
                        } else {
                            currentScreen = Screen.CHAT
                        }
                    }
                )
            }

            Screen.VIVO -> {
                val dm = remember { DataManager(ctx) }
                val openaiKey = remember { dm.getApiKey(CloudProvider.OPENAI) ?: "" }

                // Convertir memoria del viewModel a UserMemory para el modo vivo
                val vivoMemory = remember(viewModel.memoryJson) {
                    try {
                        val root = org.json.JSONObject(viewModel.memoryJson)
                        val memorias = root.optJSONArray("memorias")
                        val facts = mutableListOf<String>()
                        var userName = ""

                        if (memorias != null) {
                            for (i in 0 until memorias.length()) {
                                val m = memorias.optString(i, "")
                                if (m.isNotBlank()) {
                                    facts.add(m)
                                    // Intentar extraer nombre del usuario
                                    if (userName.isBlank()) {
                                        val namePrefixes = listOf("se llama ", "mi nombre es ", "soy ", "me llamo ")
                                        namePrefixes.forEach { prefix ->
                                            val idx = m.lowercase().indexOf(prefix)
                                            if (idx >= 0) {
                                                val afterPrefix = m.substring(idx + prefix.length).trim()
                                                val name = afterPrefix.split(" ", ",", ".").firstOrNull()?.trim()
                                                if (!name.isNullOrBlank()) {
                                                    userName = name.replaceFirstChar { it.uppercase() }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (facts.isNotEmpty()) {
                            UserMemory(
                                userName = userName,
                                facts = facts,
                                language = viewModel.uiStrings.langCode
                            )
                        } else null
                    } catch (e: Exception) { null }
                }

                ScreenVivoMode(
                    apiKey = openaiKey,
                    vivoStrings = getVivoStrings(viewModel.uiStrings.langCode),
                    userMemory = vivoMemory,
                    onBack = { currentScreen = Screen.ENGINE_SELECT },
                    onMemoryScreen = {
                        // Navegar a la pantalla de memoria desde modo vivo
                        returnToVivo = true
                        currentScreen = Screen.MEMORY
                    }
                )
            }

            Screen.EMERGENCY -> {
                ScreenEmergencyMode(
                    langCode = viewModel.uiStrings.langCode,
                    onBack = { currentScreen = Screen.ENGINE_SELECT }
                )
            }
        }
    }
}