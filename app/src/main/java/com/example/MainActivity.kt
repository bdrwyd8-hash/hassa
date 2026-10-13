package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.MainSection
import com.example.ui.components.JsonExportDialog
import com.example.ui.components.PngExportPreviewDialog
import com.example.ui.screens.ChecklistScreen
import com.example.ui.screens.ExpeditionScreen
import com.example.ui.screens.SavedCharactersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.TrailKitTheme
import com.example.viewmodel.TrailKitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val trailKitViewModel: TrailKitViewModel = viewModel(
                factory = TrailKitViewModel.provideFactory(context)
            )
            TrailKitApp(viewModel = trailKitViewModel)
        }
    }
}

@Composable
fun TrailKitApp(
    viewModel: TrailKitViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val strings = AppStrings.get(prefs.language)

    // BackHandler for non-home sections to return cleanly to Studio
    if (uiState.currentSection != MainSection.STUDIO) {
        BackHandler {
            viewModel.navigateTo(MainSection.STUDIO)
        }
    }

    TrailKitTheme(
        darkTheme = prefs.themeMode == AppThemeMode.DARK,
        isRtl = prefs.language.isRtl
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 700.dp

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("trailkit_root_scaffold"),
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = strings.appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.appSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Quick Language Toggle Pill (AR <-> EN) in header
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        val nextLang = if (prefs.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
                                        viewModel.setLanguage(nextLang)
                                    }
                                    .testTag("header_language_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (prefs.language == AppLanguage.AR) "EN" else "عربي",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentSection == MainSection.STUDIO,
                                onClick = { viewModel.navigateTo(MainSection.STUDIO) },
                                icon = { Icon(Icons.Default.Checkroom, contentDescription = strings.navStudio) },
                                label = { Text(strings.navStudio) },
                                modifier = Modifier.testTag("nav_tab_studio")
                            )
                            NavigationBarItem(
                                selected = uiState.currentSection == MainSection.EXPEDITION,
                                onClick = { viewModel.navigateTo(MainSection.EXPEDITION) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = strings.navExpedition) },
                                label = { Text(strings.navExpedition) },
                                modifier = Modifier.testTag("nav_tab_expedition")
                            )
                            NavigationBarItem(
                                selected = uiState.currentSection == MainSection.MY_CHARACTERS,
                                onClick = { viewModel.navigateTo(MainSection.MY_CHARACTERS) },
                                icon = { Icon(Icons.Default.Group, contentDescription = strings.navCharacters) },
                                label = { Text(strings.navCharacters) },
                                modifier = Modifier.testTag("nav_tab_characters")
                            )
                            NavigationBarItem(
                                selected = uiState.currentSection == MainSection.CHECKLIST,
                                onClick = { viewModel.navigateTo(MainSection.CHECKLIST) },
                                icon = { Icon(Icons.Default.Checklist, contentDescription = strings.navChecklist) },
                                label = { Text(strings.navChecklist) },
                                modifier = Modifier.testTag("nav_tab_checklist")
                            )
                            NavigationBarItem(
                                selected = uiState.currentSection == MainSection.SETTINGS,
                                onClick = { viewModel.navigateTo(MainSection.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                                label = { Text(strings.navSettings) },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("side_navigation_rail")
                        ) {
                            NavigationRailItem(
                                selected = uiState.currentSection == MainSection.STUDIO,
                                onClick = { viewModel.navigateTo(MainSection.STUDIO) },
                                icon = { Icon(Icons.Default.Checkroom, contentDescription = strings.navStudio) },
                                label = { Text(strings.navStudio) },
                                modifier = Modifier.testTag("nav_tab_studio")
                            )
                            NavigationRailItem(
                                selected = uiState.currentSection == MainSection.EXPEDITION,
                                onClick = { viewModel.navigateTo(MainSection.EXPEDITION) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = strings.navExpedition) },
                                label = { Text(strings.navExpedition) },
                                modifier = Modifier.testTag("nav_tab_expedition")
                            )
                            NavigationRailItem(
                                selected = uiState.currentSection == MainSection.MY_CHARACTERS,
                                onClick = { viewModel.navigateTo(MainSection.MY_CHARACTERS) },
                                icon = { Icon(Icons.Default.Group, contentDescription = strings.navCharacters) },
                                label = { Text(strings.navCharacters) },
                                modifier = Modifier.testTag("nav_tab_characters")
                            )
                            NavigationRailItem(
                                selected = uiState.currentSection == MainSection.CHECKLIST,
                                onClick = { viewModel.navigateTo(MainSection.CHECKLIST) },
                                icon = { Icon(Icons.Default.Checklist, contentDescription = strings.navChecklist) },
                                label = { Text(strings.navChecklist) },
                                modifier = Modifier.testTag("nav_tab_checklist")
                            )
                            NavigationRailItem(
                                selected = uiState.currentSection == MainSection.SETTINGS,
                                onClick = { viewModel.navigateTo(MainSection.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                                label = { Text(strings.navSettings) },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Status notification banner
                        AnimatedVisibility(visible = !uiState.statusBannerMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("status_notification_banner"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = uiState.statusBannerMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = strings.closeBtn,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.currentSection) {
                                MainSection.STUDIO -> StudioScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                MainSection.EXPEDITION -> ExpeditionScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                MainSection.MY_CHARACTERS -> SavedCharactersScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                MainSection.CHECKLIST -> ChecklistScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                                MainSection.SETTINGS -> SettingsScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global PNG Export Preview Dialog
        val pngRes = uiState.exportedPngResult
        if (pngRes != null) {
            PngExportPreviewDialog(
                exportResult = pngRes,
                characterName = uiState.activeCharacterName,
                language = prefs.language,
                strings = strings,
                onExportNeutralToggle = { useNeutral ->
                    viewModel.triggerPngExport(context, useNeutralBackground = useNeutral)
                },
                onDismiss = { viewModel.dismissPngExportDialog() }
            )
        }

        // Global JSON Export Preview Dialog
        val jsonRes = uiState.exportedJsonPreview
        if (jsonRes != null) {
            JsonExportDialog(
                jsonContent = jsonRes,
                characterName = uiState.activeCharacterName,
                strings = strings,
                onDismiss = { viewModel.dismissJsonExportDialog() }
            )
        }
    }
}
