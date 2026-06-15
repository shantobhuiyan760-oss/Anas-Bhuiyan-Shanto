package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Reusable Language data structure with flags
data class Language(val name: String, val code: String, val flag: String)

val sourceLanguages = listOf(
    Language("Bengali", "bn", "🇧🇩"),
    Language("English", "en", "🇺🇸"),
    Language("Spanish", "es", "🇪🇸"),
    Language("French", "fr", "🇫🇷"),
    Language("German", "de", "🇩🇪"),
    Language("Italian", "it", "🇮🇹"),
    Language("Portuguese", "pt", "🇵🇹"),
    Language("Arabic", "ar", "🇸🇦"),
    Language("Russian", "ru", "🇷🇺"),
    Language("Chinese", "zh", "🇨🇳")
)

val targetLanguages = listOf(
    Language("English", "en", "🇺🇸"),
    Language("Bengali", "bn", "🇧🇩"),
    Language("Spanish", "es", "🇪🇸"),
    Language("French", "fr", "🇫🇷"),
    Language("German", "de", "🇩🇪"),
    Language("Hindi", "hi", "🇮🇳"),
    Language("Japanese", "ja", "🇯🇵"),
    Language("Chinese", "zh", "🇨🇳"),
    Language("Korean", "ko", "🇰🇷"),
    Language("Russian", "ru", "🇷🇺")
)

@Composable
fun LanguageSelectorComponent(
    sourceLang: String,
    targetLang: String,
    onSourceLangSelected: (String) -> Unit,
    onTargetLangSelected: (String) -> Unit,
    onSwapLanguages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    var swapRotationDegree by remember { mutableStateOf(0f) }

    // Dynamic rotation animation for the swap button
    val swapRotation by animateFloatAsState(
        targetValue = swapRotationDegree,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "SwapRotation"
    )

    // Find the active language meta to access flags
    val activeSource = sourceLanguages.firstOrNull { it.name.equals(sourceLang, ignoreCase = true) }
        ?: Language(sourceLang, "unknown", "🌐")
    
    val activeTarget = targetLanguages.firstOrNull { it.name.equals(targetLang, ignoreCase = true) }
        ?: Language(targetLang, "unknown", "🌐")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Source Language Selector Button
        Box(
            modifier = Modifier
                .weight(1f)
                .testTag("source_language_dropdown_trigger")
        ) {
            LanguageSelectButton(
                label = "FROM",
                language = activeSource,
                onClick = { expandedFrom = true }
            )

            SearchableLanguageMenu(
                expanded = expandedFrom,
                onDismissRequest = { expandedFrom = false },
                activeLanguageName = sourceLang,
                languageList = sourceLanguages,
                onLanguageSelected = { lang ->
                    expandedFrom = false
                    onSourceLangSelected(lang.name)
                },
                testTagPrefix = "source"
            )
        }

        // Animated Tactile Swap Button
        IconButton(
            onClick = {
                swapRotationDegree += 180f
                onSwapLanguages()
            },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .rotate(swapRotation)
                .testTag("swap_languages_button")
        ) {
            Text(
                text = "⇄",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Target Language Selector Button
        Box(
            modifier = Modifier
                .weight(1f)
                .testTag("target_language_dropdown_trigger")
        ) {
            LanguageSelectButton(
                label = "TO",
                language = activeTarget,
                onClick = { expandedTo = true }
            )

            SearchableLanguageMenu(
                expanded = expandedTo,
                onDismissRequest = { expandedTo = false },
                activeLanguageName = targetLang,
                languageList = targetLanguages,
                onLanguageSelected = { lang ->
                    expandedTo = false
                    onTargetLangSelected(lang.name)
                },
                testTagPrefix = "target"
            )
        }
    }
}

@Composable
fun LanguageSelectButton(
    label: String,
    language: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        
        Spacer(modifier = Modifier.height(3.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = language.flag,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SearchableLanguageMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    activeLanguageName: String,
    languageList: List<Language>,
    onLanguageSelected: (Language) -> Unit,
    testTagPrefix: String
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(200.dp)
            .heightIn(max = 280.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredList = remember(searchQuery, languageList) {
            if (searchQuery.isBlank()) {
                languageList
            } else {
                languageList.filter {
                    it.name.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Dropdown Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("${testTagPrefix}_search_field"),
                placeholder = { Text("Search...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        modifier = Modifier.size(16.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Scrollable List of Language Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    filteredList.forEach { language ->
                        val isSelected = language.name.equals(activeLanguageName, ignoreCase = true)
                        
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = language.flag,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = language.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = { onLanguageSelected(language) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .testTag("${testTagPrefix}_lang_${language.code}")
                        )
                    }
                }
            }
        }
    }
}
