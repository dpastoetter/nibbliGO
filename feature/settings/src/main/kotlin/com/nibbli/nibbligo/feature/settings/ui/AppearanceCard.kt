package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.model.AppThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                AppThemeMode.SYSTEM -> "System"
                AppThemeMode.LIGHT -> "Light"
                AppThemeMode.DARK -> "Dark"
                AppThemeMode.SUPER_DARK -> "Super dark"
            }
            NibbliSuggestionChip(
                label = label,
                selected = selected == mode,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
fun AppearanceCard(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Text(
            "Light, dark, super dark (OLED black), or match your phone's system setting.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        ThemeModeSelector(
            selected = themeMode,
            onSelect = onThemeModeChange,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
