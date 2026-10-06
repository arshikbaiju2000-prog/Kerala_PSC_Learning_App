package com.keralapsc.prep.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Design Tokens for the Kerala PSC Prep application.
 * Ensures consistent colors, typography, dimensions, shapes, and accessibility
 * across all screens without hardcoded values.
 */
object PSCTokens {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val cornerRadii: CornerRadii
        @Composable
        @ReadOnlyComposable
        get() = LocalCornerRadii.current

    val elevations: Elevations
        @Composable
        @ReadOnlyComposable
        get() = LocalElevations.current

    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = SuccessLight

    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = WarningLight

    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = BrandAccentLight

    val minTouchTarget: Dp = 48.dp
}
