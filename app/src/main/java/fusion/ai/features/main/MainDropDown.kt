package fusion.ai.features.main

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import fusion.ai.R
import fusion.ai.ui.theme.InterFontFamily
import fusion.ai.util.openUrl
import me.saket.cascade.CascadeDropdownMenu

@Composable
fun MainDropDown(
    isMenuDisplayed: Boolean,
    onChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    @Composable
    fun InnerDropdown(
        text: String,
        onClick: () -> Unit
    ) {
        DropdownMenuItem(
            onClick = {
                onChange(false)
                onClick()
            },
            text = {
                Text(
                    text = text,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = .3.sp,
                    modifier = Modifier
                        .padding(2.dp)
                )
            }
        )
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onChange(!isMenuDisplayed) }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
        }
        CascadeDropdownMenu(
            expanded = isMenuDisplayed,
            onDismissRequest = { onChange(false) }
        ) {
            InnerDropdown(
                text = stringResource(R.string.github_menu_title),
                onClick = {
                    context.openUrl(url = context.getString(R.string.github_url))
                }
            )

            InnerDropdown(
                text = stringResource(R.string.privacy_policy_menu_title),
                onClick = {
                    context.openUrl(url = context.getString(R.string.privacy_policy_url))
                }
            )

            InnerDropdown(
                text = stringResource(R.string.open_source_license_menu_title)
            ) {
                val intent = Intent(
                    context,
                    OssLicensesMenuActivity::class.java
                )
                context.startActivity(intent)
            }
        }
    }
}
