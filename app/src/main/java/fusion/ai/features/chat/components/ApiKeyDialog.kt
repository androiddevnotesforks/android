package fusion.ai.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fusion.ai.R
import fusion.ai.ui.theme.InterFontFamily

/**
 * API Key is saved locally and is attached to headers which in turn
 * is directly forwarded to OpenAI headers.
 * */
@Composable
fun ApiKeyDialog(
    apiKeyConfigured: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    save: (String) -> Unit
) {
    val (apiKey, updateApiKey) = remember {
        mutableStateOf("")
    }
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .then(modifier),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (apiKeyConfigured) {
                Text(
                    text = stringResource(R.string.api_key_already_setup),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = stringResource(R.string.api_key_disclaimer),
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(.5f),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = updateApiKey,
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = {
                    Text(text = stringResource(R.string.api_key_paste_here))
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = InterFontFamily)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Button(
                    onClick = {
                        save(apiKey)
                    },
                    enabled = apiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (apiKeyConfigured) {
                            stringResource(R.string.api_key_update_btn)
                        } else {
                            stringResource(R.string.api_key_set_btn)
                        }
                    )
                }
            }
        }
    }
}
