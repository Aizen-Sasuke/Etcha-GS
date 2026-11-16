package com.example.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shared design tokens ensuring consistent spacing, radii, and control heights across all screens.
 */
object DesignTokens {
    val ScreenHorizontalPadding = 16.dp
    val SectionSpacing = 16.dp
    val CardPadding = 16.dp
    val CompactCardPadding = 12.dp

    val CardRadius = 16.dp
    val ControlRadius = 12.dp
    val ChipRadius = 8.dp
    val SmallBadgeRadius = 6.dp

    val CardShape = RoundedCornerShape(CardRadius)
    val ControlShape = RoundedCornerShape(ControlRadius)
    val ChipShape = RoundedCornerShape(ChipRadius)

    val ActionButtonHeight = 40.dp
    val HeaderActionSize = 40.dp
    val SegmentedTabHeight = 44.dp
    val CompactChipHeight = 32.dp
}
