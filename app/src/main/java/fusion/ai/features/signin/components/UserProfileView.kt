package fusion.ai.features.signin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fusion.ai.features.main.SignInState

@Composable
fun UserProfileView(
    currentUserProfile: String?,
    onUserProfileClick: () -> Unit,
    signInState: SignInState,
    modifier: Modifier = Modifier
) {
    IconButton(
        modifier = modifier,
        onClick = onUserProfileClick
    ) {
        currentUserProfile?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.profileModifier()
            )
        }

        if (signInState == SignInState.Progress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp),
                strokeWidth = 2.dp
            )
        } else if (currentUserProfile == null) {
            val icon = Icons.Filled.AccountCircle
            Image(
                imageVector = icon,
                contentDescription = icon.name,
                modifier = Modifier.profileModifier(),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun Modifier.profileModifier() = composed {
    size(35.dp)
        .border(
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSecondaryContainer),
            shape = CircleShape
        )
        .padding(4.dp)
        .clip(CircleShape)
}
