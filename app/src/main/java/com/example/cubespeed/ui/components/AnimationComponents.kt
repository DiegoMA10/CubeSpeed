package com.example.cubespeed.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

/**
 * A composable that maintains the size of its content even when it's not visible.
 * This prevents layout shifts during animations.
 *
 * @param visible Whether the content should be visible
 * @param modifier Modifier to be applied to the composable
 * @param enter The enter transition to be used when the content becomes visible
 * @param exit The exit transition to be used when the content becomes invisible
 * @param content The content to be displayed when visible
 */
@Composable
fun FixedSizeAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable () -> Unit
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit
        ) {
            Box(
                Modifier
                    .onSizeChanged { contentSize = it }
            ) {
                content()
            }
        }
        if (!visible && contentSize != IntSize.Zero) {
            Spacer(
                Modifier
                    .requiredWidth(with(density) { contentSize.width.toDp() })
                    .requiredHeight(with(density) { contentSize.height.toDp() })
            )
        }
    }
}