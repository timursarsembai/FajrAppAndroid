package com.example.fajrapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp,
    hazeState: HazeState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(cornerRadius),
                style = HazeStyle(
                    tint = Color.White.copy(alpha = 0.3f),
                    blurRadius = 16.dp
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}
