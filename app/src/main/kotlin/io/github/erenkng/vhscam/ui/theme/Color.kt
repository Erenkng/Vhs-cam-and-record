package io.github.erenkng.vhscam.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Generated with Material Color Utilities (vibrant scheme) from the "tape orange" seed #FF6A13.
// Used when dynamic colour is unavailable (Android 10-11) or turned off.

val BrandLightColors = lightColorScheme(
    primary = Color(0xFFA33E00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF7C2E00),
    inversePrimary = Color(0xFFFFB596),
    secondary = Color(0xFF7D5636),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC3),
    onSecondaryContainer = Color(0xFF623F21),
    tertiary = Color(0xFF7E561D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB6),
    onTertiaryContainer = Color(0xFF633F06),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF271812),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF271812),
    surfaceVariant = Color(0xFFFCDCCF),
    onSurfaceVariant = Color(0xFF584239),
    surfaceTint = Color(0xFFA33E00),
    inverseSurface = Color(0xFF3D2D26),
    inverseOnSurface = Color(0xFFFFEDE7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF8B7167),
    outlineVariant = Color(0xFFDFC0B4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceContainer = Color(0xFFFFE9E2),
    surfaceContainerHigh = Color(0xFFFEE2D8),
    surfaceContainerHighest = Color(0xFFF8DDD2),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF0D4CA),
    primaryFixed = Color(0xFFFFDBCD),
    primaryFixedDim = Color(0xFFFFB596),
    onPrimaryFixed = Color(0xFF360F00),
    onPrimaryFixedVariant = Color(0xFF7C2E00),
    secondaryFixed = Color(0xFFFFDCC3),
    secondaryFixedDim = Color(0xFFF0BC95),
    onSecondaryFixed = Color(0xFF2F1500),
    onSecondaryFixedVariant = Color(0xFF623F21),
    tertiaryFixed = Color(0xFFFFDDB6),
    tertiaryFixedDim = Color(0xFFF2BD7A),
    onTertiaryFixed = Color(0xFF2A1800),
    onTertiaryFixedVariant = Color(0xFF633F06),
)

val BrandDarkColors = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF581E00),
    primaryContainer = Color(0xFF7C2E00),
    onPrimaryContainer = Color(0xFFFFDBCD),
    inversePrimary = Color(0xFFA33E00),
    secondary = Color(0xFFF0BC95),
    onSecondary = Color(0xFF48290D),
    secondaryContainer = Color(0xFF623F21),
    onSecondaryContainer = Color(0xFFFFDCC3),
    tertiary = Color(0xFFF2BD7A),
    onTertiary = Color(0xFF462B00),
    tertiaryContainer = Color(0xFF633F06),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF1D100A),
    onBackground = Color(0xFFF8DDD2),
    surface = Color(0xFF1D100A),
    onSurface = Color(0xFFF8DDD2),
    surfaceVariant = Color(0xFF584239),
    onSurfaceVariant = Color(0xFFDFC0B4),
    surfaceTint = Color(0xFFFFB596),
    inverseSurface = Color(0xFFF8DDD2),
    inverseOnSurface = Color(0xFF3D2D26),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA78B80),
    outlineVariant = Color(0xFF584239),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF47352E),
    surfaceContainer = Color(0xFF2B1C16),
    surfaceContainerHigh = Color(0xFF362620),
    surfaceContainerHighest = Color(0xFF42312A),
    surfaceContainerLow = Color(0xFF271812),
    surfaceContainerLowest = Color(0xFF180B06),
    surfaceDim = Color(0xFF1D100A),
    primaryFixed = Color(0xFFFFDBCD),
    primaryFixedDim = Color(0xFFFFB596),
    onPrimaryFixed = Color(0xFF360F00),
    onPrimaryFixedVariant = Color(0xFF7C2E00),
    secondaryFixed = Color(0xFFFFDCC3),
    secondaryFixedDim = Color(0xFFF0BC95),
    onSecondaryFixed = Color(0xFF2F1500),
    onSecondaryFixedVariant = Color(0xFF623F21),
    tertiaryFixed = Color(0xFFFFDDB6),
    tertiaryFixedDim = Color(0xFFF2BD7A),
    onTertiaryFixed = Color(0xFF2A1800),
    onTertiaryFixedVariant = Color(0xFF633F06),
)

/** Fixed colours that must not follow the wallpaper: recording red and the OSD amber. */
object CameraColors {
    val Record = Color(0xFFFF3B30)
    val RecordContainer = Color(0xFF5C0B07)
    val OnRecord = Color.White
    val Scrim = Color(0x99000000)
    val ControlSurface = Color(0x66000000)
    val Amber = Color(0xFFFFB238)

    /** The VCR's blue screen; matches VCR_BLUE in present.frag. */
    val VcrBlue = Color(0xFF0D1FB8)
}
