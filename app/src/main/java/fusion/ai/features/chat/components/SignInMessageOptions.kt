package fusion.ai.features.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fusion.ai.R
import fusion.ai.util.openUrl

@Composable
fun SignInMessageOptions(
    onSignInClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OptionRow(modifier) {
        OutlinedButton(
            onClick = { context.openUrl(context.getString(R.string.why_sign_in_url)) },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.why_sign_in),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = .5.sp,
                fontSize = 12.sp
            )
        }
        Button(
            onClick = onSignInClicked,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                letterSpacing = .5.sp,
                fontSize = 12.sp
            )
        }
    }
}
