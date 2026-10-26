package io.github.erenkng.vhscam.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.erenkng.vhscam.R

private fun robotoFlex(weight: Int) = Font(
    resId = R.font.roboto_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Roboto Flex is variable, so every weight the type scale asks for (including the M3E
 *  emphasized styles) is rendered from the same file. */
val RobotoFlex = FontFamily(
    robotoFlex(300),
    robotoFlex(400),
    robotoFlex(500),
    robotoFlex(600),
    robotoFlex(700),
    robotoFlex(800),
    robotoFlex(900),
)

/** VCR on-screen-display font. */
val Vt323 = FontFamily(Font(R.font.vt323))

val AppTypography = Typography(fontFamily = RobotoFlex)
