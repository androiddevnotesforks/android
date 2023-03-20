package fusion.ai.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import fusion.ai.R
import fusion.ai.datasource.cache.entity.MessageType
import fusion.ai.features.chat.components.AnimatedPlaceholder
import fusion.ai.features.chat.components.ErrorMessageContent
import fusion.ai.features.chat.components.MessageContent
import fusion.ai.features.chat.components.SignInMessageOptions
import fusion.ai.features.chat.components.WelcomeMessageOptions
import fusion.ai.ui.theme.InterFontFamily
import fusion.ai.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onSignInClicked: () -> Unit,
    openBillingSheet: () -> Unit,
    openApiKeyDialog: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ChatVM = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val apiKey = viewModel.apiKey.collectAsStateWithLifecycle(initialValue = null).value
    val isSendEnabled = viewModel.isSendEnabled.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, state.isAuthenticated) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (state.isAuthenticated) {
                    viewModel.connectToChat()
                } else {
                    viewModel.sendSignInMessage()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(key1 = apiKey) {
        if (apiKey != null) {
            viewModel.connectToChat(forceConnect = true)
        }
    }

    LaunchedEffect(key1 = state.messages) {
        listState.animateScrollToItem(0)
    }

    LaunchedEffect(key1 = state.errorEvent) {
        if (state.errorEvent != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(state.errorEvent.message),
                withDismissAction = true,
                actionLabel = "Add"
            )
            if (result == SnackbarResult.ActionPerformed) {
                openApiKeyDialog()
            }
        }
    }

    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxSize()
            // TODO: Padding?
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            state = listState
        ) {
            items(state.messages) { response ->
                when (response.type) {
                    MessageType.TextGeneration -> {
                        if (response.message != null) {
                            MessageContent(
                                modifier = Modifier.fillMaxWidth(),
                                message = response.message
                            )
                        }
                    }

                    MessageType.Error -> ErrorMessageContent(errorMessage = response.message?.content)

                    MessageType.SignInRequest -> {
                        if (response.message != null) {
                            MessageContent(
                                modifier = Modifier.fillMaxWidth(),
                                message = response.message
                            ) {
                                SignInMessageOptions(
                                    onSignInClicked = onSignInClicked
                                )
                            }
                        }
                    }

                    MessageType.Welcome -> {
                        if (response.message != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Spacer(modifier = Modifier.height(5.dp))

                                MessageContent(
                                    modifier = Modifier.fillMaxWidth(),
                                    message = response.message
                                ) {
                                    WelcomeMessageOptions()
                                }
                            }
                        }
                    }

                    MessageType.ImageGeneration -> {
                        if (response.message != null && response.message.contentIsImage
                        ) {
                            AsyncImage(
                                model = response.message.content,
                                contentDescription = "Generated Image",
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterHorizontally)
                                    .clip(
                                        RoundedCornerShape(8.dp)
                                    )
                                    .size(200.dp)
                                    .background(Color.Black),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    MessageType.LowChatToken -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = response.message?.content
                                        ?: "Oops! Looks like your free message limit is over.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AssistChip(
                                        onClick = openBillingSheet,
                                        label = {
                                            Text(text = stringResource(id = R.string.upgrade))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    MessageType.LowImageToken -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = response.message?.content
                                        ?: "Oops! Only Premium users can use this feature!",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AssistChip(
                                        onClick = openBillingSheet,
                                        label = {
                                            Text(text = stringResource(R.string.upgrade))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    MessageType.ApiKeyMissing -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = response.message?.content ?: "API key is missing",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontFamily = InterFontFamily
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AssistChip(
                                        onClick = openApiKeyDialog,
                                        label = {
                                            Text(text = "Add API Key", fontFamily = InterFontFamily)
                                        }
                                    )
                                    AssistChip(
                                        onClick = {
                                            context.openUrl(context.getString(R.string.api_key_faqs_url))
                                        },
                                        label = {
                                            Text(
                                                text = "What is this?",
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        AnimatedVisibility(
            visible = state.selectedTool != null,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                state.selectedTool?.let {
                    AssistChip(
                        onClick = { viewModel.removeTool() },
                        label = {
                            Text(
                                text = it.title,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.primary.copy(.8f)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(.8f)
                            )
                        },
                        border = AssistChipDefaults.assistChipBorder(
                            borderWidth = 1.dp,
                            borderColor = MaterialTheme.colorScheme.primary.copy(.5f)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            Column {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::onMessageChange,
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.hand_write_emoji),
                            contentDescription = stringResource(
                                id = R.string.app_name
                            ),
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    placeholder = {
                        val selectedTool = state.selectedTool
                        val hints = if (selectedTool == null) {
                            listOf(
                                "Act as ‘Character’ from ‘Movie/Book’",
                                "Recommend me some books to read",
                                "Translate or Rephrase my sentence",
                                "Ask anything 🤯"
                            )
                        } else {
                            listOf(
                                selectedTool.placeholder
                                    ?: context.getString(R.string.default_placeholder_ask_handy_ai)
                            )
                        }

                        AnimatedPlaceholder(hints)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage()
                            },
                            enabled = state.isAuthenticated &&
                                isSendEnabled &&
                                state.prompt.isNotBlank() &&
                                state.prompt.length <= state.maxPromptLength
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.inverseSurface.copy(
                                    if (state.prompt.isBlank() || !state.isAuthenticated ||
                                        !isSendEnabled
                                    ) {
                                        .5f
                                    } else {
                                        1f
                                    }
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        letterSpacing = .4.sp,
                        fontSize = 15.sp
                    ),
                    enabled = state.isAuthenticated,
                    maxLines = 5
                )
                AnimatedVisibility(
                    visible = state.prompt.length >= state.maxPromptLength
                ) {
                    Text(
                        text = "${state.prompt.length} / ${state.maxPromptLength}",
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(10.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
