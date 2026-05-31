package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.designsystem.theme.AccentColors
import com.nibbli.nibbligo.core.model.AppAccentPalette
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccentPaletteSelector(
    selected: AppAccentPalette,
    onSelect: (AppAccentPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppAccentPalette.entries.forEach { palette ->
            AccentSwatch(
                palette = palette,
                selected = selected == palette,
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun AccentSwatch(
    palette: AppAccentPalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (palette) {
        AppAccentPalette.TEAL -> "Teal"
        AppAccentPalette.LAVENDER -> "Lavender"
        AppAccentPalette.SAGE -> "Sage"
        AppAccentPalette.DUSK -> "Dusk"
        AppAccentPalette.SAND -> "Sand"
    }
    Column(
        modifier = modifier.semantics { contentDescription = "$label accent" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = AccentColors.swatchColor(palette),
            border = if (selected) {
                BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            },
            modifier = Modifier.size(if (selected) 40.dp else 36.dp),
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun AppearanceCard(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    accentPalette: AppAccentPalette,
    onAccentPaletteChange: (AppAccentPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Text(
            "Light, dark, super dark (OLED black), or match your phone's system setting.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThemeModeSelector(
            selected = themeMode,
            onSelect = onThemeModeChange,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Accent color",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Applies across Home, chat, and onboarding.",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AccentPaletteSelector(
            selected = accentPalette,
            onSelect = onAccentPaletteChange,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
