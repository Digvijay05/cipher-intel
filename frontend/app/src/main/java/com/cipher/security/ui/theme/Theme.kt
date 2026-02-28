package com.cipher.security.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ScamGuardDarkScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = Navy900,
    primaryContainer = CyberGreenDim,
    onPrimaryContainer = CyberGreen,
    secondary = InfoBlue,
    onSecondary = Navy900,
    secondaryContainer = InfoBlueDim,
    onSecondaryContainer = InfoBlue,
    tertiary = WarningAmber,
    onTertiary = Navy900,
    tertiaryContainer = WarningAmberDim,
    onTertiaryContainer = WarningAmber,
    error = ThreatRed,
    onError = Navy900,
    errorContainer = ThreatRedDim,
    onErrorContainer = ThreatRed,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy800,
    onSurface = TextPrimary,
    surfaceVariant = Navy700,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder
)

@Composable
fun CipherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ScamGuardDarkScheme,
        typography = ScamGuardTypography,
        content = content
    )
}
