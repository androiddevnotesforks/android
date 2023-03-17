package fusion.ai.features.chat.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fusion.ai.R
import fusion.ai.datasource.cache.entity.MessageEntity
import fusion.ai.datasource.cache.entity.MessageRole
import fusion.ai.ui.theme.InterFontFamily
import fusion.ai.util.copyToClipboard
import fusion.ai.util.showToast
import timber.log.Timber

private data class MessageContentTheme(
    val headerColor: Color,
    val contentColor: Color,
    val headerText: String,
    val letterSpacing: Double,
    val chatBoxAlignment: Alignment,
    val chatBoxBackgroundColor: Color,
    val shape: RoundedCornerShape
)

@Composable
private fun buildMessageContentTheme(role: MessageRole): MessageContentTheme {
    return when (role) {
        MessageRole.User -> MessageContentTheme(
            headerColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(.6f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            headerText = stringResource(id = R.string.message_from_you),
            letterSpacing = .3,
            chatBoxAlignment = Alignment.CenterEnd,
            chatBoxBackgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topEnd = 4.dp,
                topStart = 12.dp,
                bottomEnd = 12.dp,
                bottomStart = 12.dp
            )
        )

        else -> MessageContentTheme(
            headerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(.6f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            headerText = stringResource(id = R.string.message_from_ai),
            letterSpacing = .3,
            chatBoxAlignment = Alignment.CenterStart,
            chatBoxBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(
                topEnd = 14.dp,
                topStart = 12.dp,
                bottomEnd = 12.dp,
                bottomStart = 12.dp
            )
        )
    }
}

@Composable
fun MessageContent(
    message: MessageEntity,
    modifier: Modifier = Modifier,
    options: (@Composable () -> Unit)? = null
) {
    val contentTheme = buildMessageContentTheme(role = message.role)
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(
        modifier = modifier,
        contentAlignment = contentTheme.chatBoxAlignment
    ) {
        Column {
            Column(
                modifier = Modifier
                    .widthIn(
                        min = 200.dp,
                        max = LocalConfiguration.current.screenWidthDp.dp - 50.dp
                    )
                    .pointerInput(message.content) {
                        detectTapGestures(
                            onLongPress = {
                                Timber.d("Message Copied $message")
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                context.copyToClipboard(message.content)
                                if (Build.VERSION.SDK_INT <= 30) {
                                    context.showToast("Message copied")
                                }
                            }
                        )
                    }
                    .background(
                        color = contentTheme.chatBoxBackgroundColor,
                        shape = contentTheme.shape
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (message.role == MessageRole.Assistant) {
                    Text(
                        text = contentTheme.headerText,
                        color = contentTheme.headerColor,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily
                    )
                }
                Text(
                    text = message.content,
                    color = contentTheme.contentColor,
                    letterSpacing = contentTheme.letterSpacing.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                    fontSize = 16.sp,
                    fontFamily = InterFontFamily
                )
            }
            options?.invoke()
        }
    }
}
