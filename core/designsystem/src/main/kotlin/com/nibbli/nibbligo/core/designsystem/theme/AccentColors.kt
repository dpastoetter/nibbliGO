package com.nibbli.nibbligo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode

data class AccentTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val glow: Color,
)

enum class ThemeBase { LIGHT, DARK, SUPER_DARK }

object AccentColors {
    fun tokens(accent: AppAccentPalette, base: ThemeBase): AccentTokens = when (accent) {
        AppAccentPalette.TEAL -> when (base) {
            ThemeBase.LIGHT -> AccentTokens(
                primary = TealPrimary,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD4F0EB),
                onPrimaryContainer = Color(0xFF1A4A44),
                glow = TealPrimary.copy(alpha = 0.18f),
            )
            ThemeBase.DARK -> AccentTokens(
                primary = Color(0xFF52C9BA),
                onPrimary = Color(0xFF042A26),
                primaryContainer = Color(0xFF1E4A44),
                onPrimaryContainer = Color(0xFFA8F0E6),
                glow = Color(0xFF2A6B63).copy(alpha = 0.45f),
            )
            ThemeBase.SUPER_DARK -> AccentTokens(
                primary = SuperDarkPrimary,
                onPrimary = SuperDarkOnPrimary,
                primaryContainer = SuperDarkPrimaryContainer,
                onPrimaryContainer = SuperDarkOnPrimaryContainer,
                glow = SuperDarkGlowTeal.copy(alpha = 0.55f),
            )
        }
        AppAccentPalette.LAVENDER -> when (base) {
            ThemeBase.LIGHT -> AccentTokens(
                primary = Color(0xFF8B7DB8),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE8E0F5),
                onPrimaryContainer = Color(0xFF2E2648),
                glow = LavenderAccent.copy(alpha = 0.2f),
            )
            ThemeBase.DARK -> AccentTokens(
                primary = Color(0xFFB8A8E0),
                onPrimary = Color(0xFF1E1830),
                primaryContainer = Color(0xFF3A3458),
                onPrimaryContainer = Color(0xFFE4DCF8),
                glow = Color(0xFF4A4068).copy(alpha = 0.5f),
            )
            ThemeBase.SUPER_DARK -> AccentTokens(
                primary = Color(0xFFC4B4E8),
                onPrimary = Color(0xFF1A1428),
                primaryContainer = Color(0xFF2A2540),
                onPrimaryContainer = Color(0xFFE8E0F8),
                glow = SuperDarkGlowLavender.copy(alpha = 0.55f),
            )
        }
        AppAccentPalette.SAGE -> when (base) {
            ThemeBase.LIGHT -> AccentTokens(
                primary = Color(0xFF6F9872),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDFECE0),
                onPrimaryContainer = Color(0xFF243828),
                glow = Color(0xFF7FA882).copy(alpha = 0.2f),
            )
            ThemeBase.DARK -> AccentTokens(
                primary = Color(0xFF98C49C),
                onPrimary = Color(0xFF1A281C),
                primaryContainer = Color(0xFF2E4430),
                onPrimaryContainer = Color(0xFFD0ECD2),
                glow = Color(0xFF3A5840).copy(alpha = 0.45f),
            )
            ThemeBase.SUPER_DARK -> AccentTokens(
                primary = Color(0xFFA8D4AC),
                onPrimary = Color(0xFF142018),
                primaryContainer = Color(0xFF243830),
                onPrimaryContainer = Color(0xFFD8F0DA),
                glow = Color(0xFF3A5840).copy(alpha = 0.5f),
            )
        }
        AppAccentPalette.DUSK -> when (base) {
            ThemeBase.LIGHT -> AccentTokens(
                primary = Color(0xFF5F849E),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDCE8F0),
                onPrimaryContainer = Color(0xFF1E3444),
                glow = Color(0xFF6B8FA8).copy(alpha = 0.2f),
            )
            ThemeBase.DARK -> AccentTokens(
                primary = Color(0xFF8AB0CC),
                onPrimary = Color(0xFF142030),
                primaryContainer = Color(0xFF2A4050),
                onPrimaryContainer = Color(0xFFD0E4F0),
                glow = Color(0xFF3A5870).copy(alpha = 0.45f),
            )
            ThemeBase.SUPER_DARK -> AccentTokens(
                primary = Color(0xFF9EC0DC),
                onPrimary = Color(0xFF101820),
                primaryContainer = Color(0xFF243040),
                onPrimaryContainer = Color(0xFFD8ECF8),
                glow = Color(0xFF3A5870).copy(alpha = 0.5f),
            )
        }
        AppAccentPalette.SAND -> when (base) {
            ThemeBase.LIGHT -> AccentTokens(
                primary = Color(0xFFB89860),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF0E8D8),
                onPrimaryContainer = Color(0xFF443820),
                glow = Color(0xFFC4A574).copy(alpha = 0.22f),
            )
            ThemeBase.DARK -> AccentTokens(
                primary = Color(0xFFD4B888),
                onPrimary = Color(0xFF302818),
                primaryContainer = Color(0xFF504030),
                onPrimaryContainer = Color(0xFFF0E4C8),
                glow = Color(0xFF685840).copy(alpha = 0.45f),
            )
            ThemeBase.SUPER_DARK -> AccentTokens(
                primary = Color(0xFFE0C898),
                onPrimary = Color(0xFF281E10),
                primaryContainer = Color(0xFF3D3020),
                onPrimaryContainer = Color(0xFFF8ECD8),
                glow = Color(0xFF685840).copy(alpha = 0.5f),
            )
        }
    }

    fun swatchColor(accent: AppAccentPalette): Color = when (accent) {
        AppAccentPalette.TEAL -> TealPrimary
        AppAccentPalette.LAVENDER -> Color(0xFF9B8EC4)
        AppAccentPalette.SAGE -> Color(0xFF7FA882)
        AppAccentPalette.DUSK -> Color(0xFF6B8FA8)
        AppAccentPalette.SAND -> Color(0xFFC4A574)
    }

    fun colorScheme(themeMode: AppThemeMode, accent: AppAccentPalette, systemDark: Boolean): ColorScheme {
        val base = when (themeMode) {
            AppThemeMode.LIGHT -> ThemeBase.LIGHT
            AppThemeMode.DARK -> ThemeBase.DARK
            AppThemeMode.SUPER_DARK -> ThemeBase.SUPER_DARK
            AppThemeMode.SYSTEM -> if (systemDark) ThemeBase.DARK else ThemeBase.LIGHT
        }
        val t = tokens(accent, if (themeMode == AppThemeMode.SUPER_DARK) ThemeBase.SUPER_DARK else base)
        return when {
            themeMode == AppThemeMode.SUPER_DARK -> superDarkScheme(t)
            base == ThemeBase.LIGHT -> lightScheme(t)
            else -> darkScheme(t)
        }
    }

    private fun lightScheme(t: AccentTokens) = lightColorScheme(
        primary = t.primary,
        onPrimary = t.onPrimary,
        primaryContainer = t.primaryContainer,
        onPrimaryContainer = t.onPrimaryContainer,
        secondary = LavenderAccent,
        onSecondary = Color.White,
        tertiary = WarmCoral,
        onTertiary = Color.White,
        background = CreamSurface,
        onBackground = DeepSlate,
        surface = Color(0xFFFAF8F5),
        onSurface = DeepSlate,
        surfaceVariant = Color(0xFFE8E4DE),
        onSurfaceVariant = Color(0xFF5A6570),
        surfaceContainerLowest = CreamSurface,
        surfaceContainerLow = Color(0xFFF5F2ED),
        surfaceContainer = Color(0xFFF0ECE6),
        surfaceContainerHigh = Color(0xFFEAE6E0),
        surfaceContainerHighest = Color.White,
        outline = Color(0xFFD0CCC4),
        outlineVariant = Color(0xFFE0DCD4),
    )

    private fun darkScheme(t: AccentTokens) = darkColorScheme(
        primary = t.primary,
        onPrimary = t.onPrimary,
        primaryContainer = t.primaryContainer,
        onPrimaryContainer = t.onPrimaryContainer,
        secondary = LavenderAccent,
        onSecondary = Color.White,
        tertiary = WarmCoral,
        onTertiary = Color.White,
        background = DeepSlate,
        onBackground = MistText,
        surface = SlateSurface,
        onSurface = MistText,
        surfaceVariant = SlateCard,
        onSurfaceVariant = MistText.copy(alpha = 0.85f),
        surfaceContainerLowest = DeepSlate,
        surfaceContainerLow = SlateSurface,
        surfaceContainer = SlateCard,
        surfaceContainerHigh = Color(0xFF3A4854),
        surfaceContainerHighest = Color(0xFF445260),
        outline = Color(0xFF4A5864),
        outlineVariant = Color(0xFF3A4850),
    )

    private fun superDarkScheme(t: AccentTokens) = darkColorScheme(
        primary = t.primary,
        onPrimary = t.onPrimary,
        primaryContainer = t.primaryContainer,
        onPrimaryContainer = t.onPrimaryContainer,
        secondary = SuperDarkSecondary,
        onSecondary = Color(0xFF1E1830),
        secondaryContainer = SuperDarkSecondaryContainer,
        onSecondaryContainer = Color(0xFFD4C8F0),
        tertiary = SuperDarkTertiary,
        onTertiary = Color(0xFF2A1810),
        tertiaryContainer = SuperDarkTertiaryContainer,
        onTertiaryContainer = Color(0xFFF0C8B8),
        background = SuperDarkBackground,
        onBackground = SuperDarkOnBackground,
        surface = SuperDarkSurface,
        onSurface = SuperDarkOnSurface,
        surfaceVariant = SuperDarkSurfaceElevated,
        onSurfaceVariant = SuperDarkMuted,
        surfaceContainerLowest = SuperDarkBackground,
        surfaceContainerLow = SuperDarkSurface,
        surfaceContainer = SuperDarkSurfaceElevated,
        surfaceContainerHigh = SuperDarkSurfaceHigh,
        surfaceContainerHighest = SuperDarkSurfaceBright,
        outline = SuperDarkOutline,
        outlineVariant = SuperDarkOutlineVariant,
        inverseSurface = SuperDarkOnSurface,
        inverseOnSurface = SuperDarkBackground,
    )
}
