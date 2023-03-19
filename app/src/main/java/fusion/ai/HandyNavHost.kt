package fusion.ai

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fusion.ai.features.billing.BillingScreen
import fusion.ai.features.chat.ChatScreen
import fusion.ai.features.library.LibraryScreen
import fusion.ai.features.library.Screen

@ExperimentalMaterial3Api
@Composable
fun HandyNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onSignInClicked: () -> Unit,
    showApiKeyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        modifier = Modifier.then(modifier),
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("presetId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("toolId") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("extrasId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            ChatScreen(
                onSignInClicked = onSignInClicked,
                openBillingSheet = {
                    // Check
                    navController.navigate(Screen.Pricing)
                },
                openApiKeyDialog = showApiKeyDialog,
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            route = Screen.Library.route
        ) {
            LibraryScreen(
                navigateToChatScreen = { presetId, toolId, extrasId ->
                    navController.navigate(
                        Screen.Chat.buildChatRoute(
                            presetId,
                            toolId,
                            extrasId = extrasId
                        )
                    )
                },
                navigateToPricingScreen = {
                    navController.navigate(Screen.Pricing.route)
                },
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            route = Screen.Pricing.route
        ) {
            BillingScreen(
                viewModel = hiltViewModel(),
                snackbarHostState = snackbarHostState
            )
        }
    }
}

fun NavHostController.navigate(route: Screen) {
    navigate(route.route) {
        // Pop up to the start destination of the graph to
        // avoid building up a large stack of destinations
        // on the back stack as users select items
        graph.startDestinationRoute?.let { route ->
            popUpTo(route) {
                saveState = true
            }
        }
        // Avoid multiple copies of the same destination when
        // re-selecting the same item
        launchSingleTop = true
        // Restore state when re-selecting a previously selected item
        restoreState = true
    }
}
