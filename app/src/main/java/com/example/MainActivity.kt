package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TranslationItem
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.LanguageSelectorComponent
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val accentIndex by viewModel.accentColorIndex.collectAsState()
            val solidBgIndex by viewModel.solidBgIndex.collectAsState()
            val fontStyleIndex by viewModel.fontStyleIndex.collectAsState()

            TransKeyTheme(
                accentColorIndex = accentIndex,
                solidBgIndex = solidBgIndex,
                fontStyleIndex = fontStyleIndex
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isInstalling by viewModel.isInstalling.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Layout views depending on screen state
        when (currentScreen) {
            AppScreen.TRANSLATE -> TranslateScreen(viewModel)
            AppScreen.THEMES -> ThemesScreen(viewModel)
            AppScreen.HISTORY -> HistoryScreen(viewModel)
            AppScreen.INSTALL_PROMPT -> {
                // Show installation prompt overlaid above translation environment as an attractive onboarding modal
                TranslateScreen(viewModel)
                InstallPromptOverlay(viewModel)
            }
            AppScreen.INSTALLING -> InstallingProgressScreen(viewModel)
        }

        // Animated loader if installation simulation is active
        if (isInstalling && currentScreen != AppScreen.INSTALLING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TranslateScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val inputText by viewModel.inputText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val sourceLang by viewModel.sourceLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()
    val detectionStatus by viewModel.detectionStatus.collectAsState()
    val sugWords by viewModel.suggestedWords.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()

    var showQuickMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "文",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TransKey",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Box {
                IconButton(
                    onClick = { showQuickMenu = !showQuickMenu },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Quick Toggle",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                DropdownMenu(
                    expanded = showQuickMenu,
                    onDismissRequest = { showQuickMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Install Mobile Native PWA") },
                        onClick = {
                            showQuickMenu = false
                            viewModel.currentScreen.value = AppScreen.INSTALL_PROMPT
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear Input Context") },
                        onClick = {
                            showQuickMenu = false
                            viewModel.onClearInput()
                        },
                        leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) }
                    )
                }
            }
        }

        // Dual Language Selector Bar
        LanguageSelectorComponent(
            sourceLang = sourceLang,
            targetLang = targetLang,
            onSourceLangSelected = { lang -> viewModel.selectLanguages(lang, targetLang) },
            onTargetLangSelected = { lang -> viewModel.selectLanguages(sourceLang, lang) },
            onSwapLanguages = { viewModel.swapLanguages() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Main Scrolling Input / Output Panels
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            item {
                // Glass Input Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INPUT ($sourceLang)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (inputText.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.onClearInput() }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom scrolling representation of text area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Start typing on the on-screen keyboard below...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            } else {
                                Text(
                                    text = inputText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Simulated Microphone button
                    var micListening by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            micListening = !micListening
                            if (micListening) {
                                viewModel.onKeyPress("Hello ")
                                Toast.makeText(context, "Voice input simulation activated: Added 'Hello'", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(
                                color = if (micListening) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = "🎙",
                            fontSize = 18.sp,
                            color = if (micListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Translate Output Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSLATION ($targetLang)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            // Status processing shimmer light
                            if (isTranslating) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AI Processing",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Text Body
                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = if (translatedText.startsWith("Waiting")) FontStyle.Italic else FontStyle.Normal,
                            color = if (translatedText.startsWith("Waiting")) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions under translation card: copy/clipboard
                        if (inputText.isNotEmpty() && !translatedText.startsWith("Waiting")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("translation", translatedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied translation to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        text = "COPY",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Assist Action Grid
                Text(
                    text = "AI ASSIST",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fix Grammar call
                    Button(
                        onClick = { viewModel.fixGrammar() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("grammar_fix_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fix Grammar", fontSize = 12.sp)
                    }

                    // Rephrase text call
                    Button(
                        onClick = { viewModel.rephrase() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rephrase_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔄",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rephrase", fontSize = 12.sp)
                    }
                }

                // Inline quick phrase insertion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Hello", "How are you?", "Namaste", "Thanks").forEach { phrase ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                                .clickable {
                                    viewModel.onKeyPress(phrase + " ")
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Detection status and custom suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detectionStatus.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sugWords.forEach { word ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable {
                                    viewModel.onKeyPress("$word ")
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Fixed onscreen tactile keyboard
        KeyboardComponent(viewModel)

        // Bottom Tab system
        TransKeyBottomNavigation(viewModel)
    }
}

@Composable
fun KeyboardComponent(viewModel: MainViewModel) {
    val activeLangInput by viewModel.sourceLang.collectAsState()

    var isShiftActive by remember { mutableStateOf(false) }

    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.02f))
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Sidebar buttons for rapid dynamic language shortcut toggling
            Column(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(42.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                listOf("BN", "EN", "ES", "FR").forEach { langShortValue ->
                    val isCurr = (langShortValue == "BN" && activeLangInput == "Bengali") ||
                            (langShortValue == "EN" && activeLangInput == "English") ||
                            (langShortValue == "ES" && activeLangInput == "Spanish") ||
                            (langShortValue == "FR" && activeLangInput == "French")

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                color = if (isCurr) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCurr) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                when (langShortValue) {
                                    "BN" -> viewModel.selectLanguages("Bengali", "English")
                                    "EN" -> viewModel.selectLanguages("English", "Bengali")
                                    "ES" -> viewModel.selectLanguages("Spanish", "English")
                                    "FR" -> viewModel.selectLanguages("French", "English")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = langShortValue,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Keyboard core entry key matrices
            Column(modifier = Modifier.weight(1f)) {
                // Row 1 keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row1.forEach { keyLabel ->
                        val letter = if (isShiftActive) keyLabel else keyLabel.lowercase()
                        KeyboardKey(
                            label = letter,
                            modifier = Modifier.weight(1f),
                            onTap = { viewModel.onKeyPress(letter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2 keys (Centered offset)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row2.forEach { keyLabel ->
                        val letter = if (isShiftActive) keyLabel else keyLabel.lowercase()
                        KeyboardKey(
                            label = letter,
                            modifier = Modifier.weight(1f),
                            onTap = { viewModel.onKeyPress(letter) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3 keys: Shift, ZXCVBNM, Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Key
                    Box(
                        modifier = Modifier
                            .size(height = 42.dp, width = 42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if (isShiftActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
                            )
                            .clickable { isShiftActive = !isShiftActive },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Shift",
                            tint = if (isShiftActive) Color.Black else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Standard Letters
                    row3.forEach { keyLabel ->
                        val letter = if (isShiftActive) keyLabel else keyLabel.lowercase()
                        KeyboardKey(
                            label = letter,
                            modifier = Modifier.weight(1f),
                            onTap = { viewModel.onKeyPress(letter) }
                        )
                    }

                    // Backspace Key
                    Box(
                        modifier = Modifier
                            .size(height = 42.dp, width = 42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.onBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Row keys: 123, space, period, Enter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Symbol numbers shortcut entry state
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .weight(1.5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                viewModel.onKeyPress("?")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "123",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Spacebar Key
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .weight(5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { viewModel.onSpace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "space",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Period Key
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.onKeyPress(".") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ".",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Enter Key
                    Box(
                        modifier = Modifier
                            .height(42.dp)
                            .weight(1.8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.onKeyPress("\n")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ENTER",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ThemesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activeTheme by viewModel.activePredefinedTheme.collectAsState()
    val accentIndex by viewModel.accentColorIndex.collectAsState()
    val solidBgIndex by viewModel.solidBgIndex.collectAsState()
    val fontStyleIndex by viewModel.fontStyleIndex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Simple top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen.value = AppScreen.TRANSLATE }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Dynamic Key Themes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Inner customizer choices bento layout scrollable
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                Text(
                    text = "PREMIUM PRESETS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Midnight Pro Preset Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.activePredefinedTheme.value = "Midnight Pro"
                            viewModel.accentColorIndex.value = 0 // Purple
                            viewModel.solidBgIndex.value = 0 // Charcoal
                            viewModel.fontStyleIndex.value = 0 // Inter
                            Toast.makeText(context, "Applied Midnight Pro Preset Theme!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Midnight Pro",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Deep Charcoal backing with purple glows",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeTheme == "Midnight Pro") {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Glass Aurora Preset Card (Predefined template)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.activePredefinedTheme.value = "Glass Aurora"
                            viewModel.accentColorIndex.value = 1 // Blue accent
                            viewModel.solidBgIndex.value = 1 // Cosmic Navy bg
                            viewModel.fontStyleIndex.value = 1 // Roboto font
                            Toast.makeText(context, "Applied Glass Aurora Preset Theme!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Glass Aurora",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Cosmic Navy backing with blue visual hues",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeTheme == "Glass Aurora") {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                // Customized manual settings
                Text(
                    text = "CUSTOMIZE BRAND ACCENTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Accent color choice bubbles row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val colors = listOf(AccentPurple, AccentBlue, AccentPink, AccentYellow, AccentTeal)
                    colors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (accentIndex == index) 3.dp else 1.dp,
                                    color = if (accentIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.accentColorIndex.value = index
                                    viewModel.activePredefinedTheme.value = "Custom"
                                }
                        ) {
                            if (accentIndex == index) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "BACKGROUND CANVASES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val labels = listOf("Solid Midnight", "Cosmic Navy", "Deep Space")
                    labels.forEachIndexed { index, label ->
                        val isSel = solidBgIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.solidBgIndex.value = index
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "TYPOGRAPHY FONTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fonts = listOf("Inter (Modern Sans Serif)", "Roboto (Default Dynamic)", "Playfair Display (Classy Serif)")
                    fonts.forEachIndexed { index, fontName ->
                        val isSel = fontStyleIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.fontStyleIndex.value = index
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = fontName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            RadioButton(
                                selected = isSel,
                                onClick = { viewModel.fontStyleIndex.value = index }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Nav Area
        TransKeyBottomNavigation(viewModel)
    }
}

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val history by viewModel.historyList.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // Header with clean quick action clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.currentScreen.value = AppScreen.TRANSLATE }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Saved Translations",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        Toast.makeText(context, "Translation log history cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear all",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Translation Logs list card grids
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📜",
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your translations will appear here.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Any translated words typed above inside the input box is automatically saved locally in your Room database log.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history) { logItem ->
                        HistoryCard(logItem, viewModel, context)
                    }
                }
            }
        }

        // Bottom Tab System
        TransKeyBottomNavigation(viewModel)
    }
}

@Composable
fun HistoryCard(logItem: TranslationItem, viewModel: MainViewModel, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.inputText.value = logItem.sourceText
                viewModel.translatedText.value = logItem.translatedText
                viewModel.sourceLang.value = logItem.fromLanguage
                viewModel.targetLang.value = logItem.toLanguage
                viewModel.currentScreen.value = AppScreen.TRANSLATE
                Toast.makeText(context, "Loaded translation into workspace", Toast.LENGTH_SHORT).show()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Breadcrumb Indicators
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌐",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${logItem.fromLanguage} → ${logItem.toLanguage}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete single log
                IconButton(
                    onClick = { viewModel.deleteHistoryItem(logItem.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Source Query text
            Text(
                text = logItem.sourceText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Translated Response text
            Text(
                text = logItem.translatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InstallPromptOverlay(viewModel: MainViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { viewModel.currentScreen.value = AppScreen.TRANSLATE },
        contentAlignment = Alignment.Center
    ) {
        // Attractively stylized custom modal centered on native PWA onboarding metrics
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}, // Intercept bubble taps
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle Translate Icon logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "文",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Install TransKey Now",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get the full integrated mobile experience with offline conversion caches, faster touch responsiveness, and automated clipboard integrations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Feature grid list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.02f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InstallationBenefitRow("Native Keyboard Layout Support", Icons.Default.Settings)
                    InstallationBenefitRow("Instant Offline Dictionary Cache", Icons.Default.Check)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerInstallAppSimulation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("install_now_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Install Now", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { viewModel.currentScreen.value = AppScreen.TRANSLATE },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Not Now",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstallationBenefitRow(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InstallingProgressScreen(viewModel: MainViewModel) {
    val progress by viewModel.installProgress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App top identity
        Text(
            text = "TransKey App Installation",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp)
        )

        // Progress loader and ring
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.05f)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "INSTALLING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Onboarding details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OnboardingBenefitTip("Fast Access: Launch keyboard translator instantly from any background view.")
            OnboardingBenefitTip("Offline translation: Secure local translation cache requires zero data.")
        }
    }
}

@Composable
fun OnboardingBenefitTip(tip: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = tip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TransKeyBottomNavigation(viewModel: MainViewModel) {
    val currScreen by viewModel.currentScreen.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.02f))
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Translate Tab
        IconButton(
            onClick = { viewModel.currentScreen.value = AppScreen.TRANSLATE },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⌨",
                    color = if (currScreen == AppScreen.TRANSLATE || currScreen == AppScreen.INSTALL_PROMPT) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    fontSize = 18.sp
                )
                Text(
                    text = "Translate",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 9.sp,
                    color = if (currScreen == AppScreen.TRANSLATE || currScreen == AppScreen.INSTALL_PROMPT) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }
        }

        // Themes Tab
        IconButton(
            onClick = { viewModel.currentScreen.value = AppScreen.THEMES },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎨",
                    color = if (currScreen == AppScreen.THEMES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 18.sp
                )
                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 9.sp,
                    color = if (currScreen == AppScreen.THEMES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        // History Tab
        IconButton(
            onClick = { viewModel.currentScreen.value = AppScreen.HISTORY },
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📜",
                    color = if (currScreen == AppScreen.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 18.sp
                )
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 9.sp,
                    color = if (currScreen == AppScreen.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Custom symmetric padding descriptor to avoid missing parameters in early APIs
fun symmetricPadding(horizontal: Int, vertical: Int): Modifier = Modifier
val pxToDp = 1.dp
