package io.github.erenkng.vhscam.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Generated with Material Color Utilities from the jade seed #00A86B: the fidelity scheme, so the
// containers are the jade itself, with the tonal-spot scheme's sea-glass teal as the tertiary.
// The app's colours unless the wallpaper colours are turned on (Android 12+).

/** The jade the schemes are built from, for the launcher icon and anything else branded. */
val Jade = Color(0xFF00A86B)

val BrandLightColors = lightColorScheme(
    primary = Color(0xFF006D43),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF00A86B),
    onPrimaryContainer = Color(0xFF00331D),
    inversePrimary = Color(0xFF59DE9B),
    secondary = Color(0xFF3A684D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB9EBCA),
    onSecondaryContainer = Color(0xFF3E6C51),
    tertiary = Color(0xFF3C6471),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F9),
    onTertiaryContainer = Color(0xFF224C59),
    background = Color(0xFFF4FBF3),
    onBackground = Color(0xFF161D18),
    surface = Color(0xFFF4FBF3),
    onSurface = Color(0xFF161D18),
    surfaceVariant = Color(0xFFD8E6DA),
    onSurfaceVariant = Color(0xFF3D4A41),
    surfaceTint = Color(0xFF006D43),
    inverseSurface = Color(0xFF2B322D),
    inverseOnSurface = Color(0xFFECF3EB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    outline = Color(0xFF6D7A70),
    outlineVariant = Color(0xFFBCCABE),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF4FBF3),
    surfaceContainer = Color(0xFFE9F0E8),
    surfaceContainerHigh = Color(0xFFE3EAE2),
    surfaceContainerHighest = Color(0xFFDDE4DD),
    surfaceContainerLow = Color(0xFFEEF6ED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD5DCD4),
    primaryFixed = Color(0xFF78FBB6),
    primaryFixedDim = Color(0xFF59DE9B),
    onPrimaryFixed = Color(0xFF002111),
    onPrimaryFixedVariant = Color(0xFF005232),
    secondaryFixed = Color(0xFFBCEECD),
    secondaryFixedDim = Color(0xFFA0D2B2),
    onSecondaryFixed = Color(0xFF002111),
    onSecondaryFixedVariant = Color(0xFF214F37),
    tertiaryFixed = Color(0xFFBFE9F9),
    tertiaryFixedDim = Color(0xFFA3CDDC),
    onTertiaryFixed = Color(0xFF001F27),
    onTertiaryFixedVariant = Color(0xFF224C59),
)

val BrandDarkColors = darkColorScheme(
    primary = Color(0xFF59DE9B),
    onPrimary = Color(0xFF003921),
    primaryContainer = Color(0xFF00A86B),
    onPrimaryContainer = Color(0xFF00331D),
    inversePrimary = Color(0xFF006D43),
    secondary = Color(0xFFA0D2B2),
    onSecondary = Color(0xFF053822),
    secondaryContainer = Color(0xFF245239),
    onSecondaryContainer = Color(0xFF93C3A4),
    tertiary = Color(0xFFA3CDDC),
    onTertiary = Color(0xFF043541),
    tertiaryContainer = Color(0xFF224C59),
    onTertiaryContainer = Color(0xFFBFE9F9),
    background = Color(0xFF0E1510),
    onBackground = Color(0xFFDDE4DD),
    surface = Color(0xFF0E1510),
    onSurface = Color(0xFFDDE4DD),
    surfaceVariant = Color(0xFF3D4A41),
    onSurfaceVariant = Color(0xFFBCCABE),
    surfaceTint = Color(0xFF59DE9B),
    inverseSurface = Color(0xFFDDE4DD),
    inverseOnSurface = Color(0xFF2B322D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF869489),
    outlineVariant = Color(0xFF3D4A41),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF343B36),
    surfaceContainer = Color(0xFF1A211C),
    surfaceContainerHigh = Color(0xFF252C27),
    surfaceContainerHighest = Color(0xFF2F3631),
    surfaceContainerLow = Color(0xFF161D18),
    surfaceContainerLowest = Color(0xFF09100B),
    surfaceDim = Color(0xFF0E1510),
    primaryFixed = Color(0xFF78FBB6),
    primaryFixedDim = Color(0xFF59DE9B),
    onPrimaryFixed = Color(0xFF002111),
    onPrimaryFixedVariant = Color(0xFF005232),
    secondaryFixed = Color(0xFFBCEECD),
    secondaryFixedDim = Color(0xFFA0D2B2),
    onSecondaryFixed = Color(0xFF002111),
    onSecondaryFixedVariant = Color(0xFF214F37),
    tertiaryFixed = Color(0xFFBFE9F9),
    tertiaryFixedDim = Color(0xFFA3CDDC),
    onTertiaryFixed = Color(0xFF001F27),
    onTertiaryFixedVariant = Color(0xFF224C59),
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
