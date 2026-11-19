package com.example.quipu.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val QuipuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object QuipuSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp

    val screenHorizontal = 20.dp
    val screenVertical = 16.dp
    val cardPadding = 18.dp
    val iconButtonSize = 40.dp
}

object QuipuSizes {
    val noteCardMinHeight = 92.dp
    val emptyStateIcon = 56.dp
    val fabSize = 56.dp
    val touchTarget = 48.dp
}
