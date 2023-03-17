/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
@file:OptIn(ExperimentalAnimationApi::class, ExperimentalAnimationApi::class)

package fusion.ai.features.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import fusion.ai.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

@Composable
fun AnimatedPlaceholder(
    hints: List<String>,
    textStyle: FontFamily = InterFontFamily,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val iterator = hints.listIterator()

    val target by produceState(
        initialValue = hints.first(),
        key1 = hints
    ) {
        if (hints.size == 1) {
            value = hints.first()
        } else {
            iterator.doWhenHasNextOrPrevious {
                value = it
            }
        }
    }

    AnimatedContent(
        targetState = target,
        transitionSpec = { ScrollAnimation() },
        label = "placeholder"
    ) { str ->
        Text(
            text = str,
            fontFamily = textStyle,
            color = textColor,
            fontSize = 14.withScale()
        )
    }
}

object ScrollAnimation {
    operator fun invoke(): ContentTransform {
        return slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween()
        ) + fadeIn() with slideOutVertically(
            targetOffsetY = { -50 },
            animationSpec = tween()
        ) + fadeOut()
    }
}

suspend fun <T> ListIterator<T>.doWhenHasNextOrPrevious(
    delayMills: Long = 3000,
    doWork: suspend (T) -> Unit
) {
    while (hasNext() || hasPrevious()) {
        while (hasNext()) {
            delay(delayMills)
            doWork(next())
        }
        while (hasPrevious()) {
            delay(delayMills)
            doWork(previous())
        }
    }
}

/**
 * This prevents text size from enlarging when
 * user increases their devices' font size
 */

@Composable
fun Int.withScale(): TextUnit {
    return with(LocalDensity.current) {
        (this@withScale / fontScale).sp
    }
}
