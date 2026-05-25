package com.example.fajrapp.ui.components

import android.app.ActivityManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    blurEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val shouldUseSimpleGlass = remember {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val isLowRamDevice = activityManager?.isLowRamDevice ?: false
        // Older devices and low-RAM phones struggle with realtime blur.
        isLowRamDevice || Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
    }
    val useBlur = blurEnabled && !shouldUseSimpleGlass

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (useBlur) {
                    Modifier.hazeChild(
                        state = hazeState,
                        shape = RoundedCornerShape(cornerRadius),
                        style = HazeStyle(
                            tint = Color.White.copy(alpha = 0.3f),
                            blurRadius = 16.dp
                        )
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.18f))
                }
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
