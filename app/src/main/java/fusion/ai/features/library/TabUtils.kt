package fusion.ai.features.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.ui.graphics.vector.ImageVector
import fusion.ai.R

data class TabRowItem(
    @StringRes val title: Int,
    val screen: Screen,
    val icon: ImageVector
)

val navigationItems = listOf(
    TabRowItem(
        title = R.string.home_tab,
        screen = Screen.Chat,
        icon = Icons.Rounded.Home
    ),
    TabRowItem(
        title = R.string.library_tab,
        screen = Screen.Library,
        icon = Icons.Rounded.List
    )
)

enum class Screen(val route: String) {
    Chat("chat_screen?presetId={presetId}/?toolId={toolId}/?extrasId={extrasId}"),
    Library("library_screen"),
    Pricing("pricing_screen");

    fun buildChatRoute(
        presetId: Int,
        toolId: Int,
        extrasId: Int?
    ): String {
        return if (extrasId == null) {
            "chat_screen?presetId=$presetId/?toolId=$toolId/?extrasId={extrasId}"
        } else {
            "chat_screen?presetId=$presetId/?toolId=$toolId/?extrasId=$extrasId"
        }
    }
}
