package com.keralapsc.prep.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val massive: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

@Immutable
data class CornerRadii(
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val pill: Dp = 999.dp
)

val LocalCornerRadii = staticCompositionLocalOf { CornerRadii() }

@Immutable
data class Elevations(
    val flat: Dp = 0.dp,
    val subtle: Dp = 1.dp,
    val raised: Dp = 3.dp,
    val prominent: Dp = 6.dp
)

val LocalElevations = staticCompositionLocalOf { Elevations() }

object AnimationDurations {
    const val FAST = 150
    const val STANDARD = 300
    const val CELEBRATION = 600
}
