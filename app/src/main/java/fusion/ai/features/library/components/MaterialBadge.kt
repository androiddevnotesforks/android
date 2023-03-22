/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package fusion.ai.features.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fusion.ai.ui.theme.InterFontFamily

@Composable
fun MaterialBadge(
    text: String,
    textSize: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier,
            fontFamily = InterFontFamily,
            fontSize = textSize.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MaterialBadgeOutline(
    text: String,
    textSize: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable(
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier,
            fontFamily = InterFontFamily,
            fontSize = textSize.sp,
            letterSpacing = 0.5.sp
        )
    }
}
