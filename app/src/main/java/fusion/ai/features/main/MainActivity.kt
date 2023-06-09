package fusion.ai.features.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.BillingClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import fusion.ai.BuildConfig
import fusion.ai.HandyNavHost
import fusion.ai.R
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
import fusion.ai.features.chat.components.ApiKeyDialog
import fusion.ai.features.library.Screen
import fusion.ai.features.library.components.MaterialBadge
import fusion.ai.features.library.navigationItems
import fusion.ai.features.signin.components.UserProfileView
import fusion.ai.navigate
import fusion.ai.ui.theme.HandyAITheme
import fusion.ai.ui.theme.InterFontFamily
import fusion.ai.util.openUrl
import fusion.ai.util.showToast
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    private val viewModel: MainVM by viewModels()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val signInCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(activityResult.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.authWithFirebase(account)
            } catch (e: ApiException) {
                if (e.statusCode == 12501) {
                    // User cancelled
                    viewModel.updateSignInState(SignInState.Unknown)
                } else {
                    Timber.e(e)
                    viewModel.updateSignInState(SignInState.Failure(e.message))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel.lifecycleObserver)

        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            val (displayApiKeyDialog, updateDisplayApiKeyDialog) = rememberSaveable {
                mutableStateOf(false)
            }
            val (isMenuDisplayed, updateIsMenuDisplayed) = rememberSaveable {
                mutableStateOf(false)
            }
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val snackbarHostState = remember { SnackbarHostState() }
            val newPurchase =
                viewModel.newPurchase.collectAsStateWithLifecycle(initialValue = null).value
            val streamResponse = viewModel.streamResponse.collectAsStateWithLifecycle().value
            val initialMessageShown =
                viewModel.initialMessageShown.collectAsStateWithLifecycle().value

            LaunchedEffect(key1 = newPurchase) {
                if (newPurchase != null) {
                    val (responseCode, purchase) = newPurchase
                    when (responseCode) {
                        BillingClient.BillingResponseCode.OK -> purchase?.let {
                            Timber.d("Purchase Successful")
                            snackbarHostState.showSnackbar(
                                message = getString(R.string.purchase_successful),
                                withDismissAction = true
                            )
                        }

                        BillingClient.BillingResponseCode.USER_CANCELED -> {
                            Timber.d("Purchase of premium membership was cancelled by the user")
                            viewModel.updateLocalBillingState(BillingRepository.SkuState.NOT_PURCHASED)
                        }

                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                            Timber.d("Item is already owned by the user")
//                            viewModel.updateLocalBillingState(BillingRepository.SkuState.PURCHASED_AND_ACKNOWLEDGED)
                        }

                        else -> {
                            Timber.d("Purchase of premium membership failed due to an unknown error during the purchase flow: $responseCode")
                            snackbarHostState.showSnackbar(
                                message = getString(R.string.purchase_error_after_payment)
                            )
                            viewModel.updateLocalBillingState(BillingRepository.SkuState.NOT_PURCHASED)
                        }
                    }
                }
            }

            LaunchedEffect(key1 = state.signInState) {
                when (state.signInState) {
                    is SignInState.Failure -> {
                        Timber.d("onCreate: ${state.signInState.message}")
                        snackbarHostState.showSnackbar(
                            message = state.signInState.message
                                ?: context.getString(R.string.generic_error_message)
                        )
                    }
                    else -> Unit
                }
            }

            HandyAITheme {
                if (displayApiKeyDialog) {
                    ApiKeyDialog(
                        onDismissRequest = { updateDisplayApiKeyDialog(false) },
                        apiKeyConfigured = state.apiKeyConfigured
                    ) { key ->
                        viewModel.updateApiKeyLocally(key)
                        updateDisplayApiKeyDialog(false)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.updated),
                                withDismissAction = true
                            )
                        }
                    }
                }

                if (initialMessageShown == false) {
                    AlertDialog(
                        onDismissRequest = { /** Can only be dismissed by the button */ },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Changelog ${BuildConfig.VERSION_NAME}",
                                    fontFamily = InterFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = .5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            item {
                                Text(
                                    text = stringResource(R.string.feedback_request),
                                    fontFamily = InterFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface.copy(.8f),
                                    fontSize = 14.sp
                                )
                            }

                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            viewModel.updateInitialMessageShown()
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Text(text = "Sure", fontFamily = InterFontFamily)
                                    }
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.app_name),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium
                                    )

                                    val userPlan = state.userPlan
                                    if (userPlan != null) {
                                        if (userPlan == Plan.Trial) {
                                            MaterialBadge(
                                                modifier = Modifier
                                                    .padding(
                                                        horizontal = 8.dp,
                                                        vertical = 2.dp
                                                    ),
                                                text = when (currentRoute) {
                                                    Screen.Pricing.route -> "Roadmap"
                                                    else -> "Get Premium"
                                                },
                                                textSize = 14,
                                                onClick = {
                                                    when (currentRoute) {
                                                        Screen.Pricing.route -> context.openUrl(
                                                            context.getString(
                                                                R.string.limitations_and_roadmap_url
                                                            )
                                                        )
                                                        else -> navController.navigate(Screen.Pricing.route)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                if (state.userPlan == Plan.ThreeMonthly) {
                                    IconButton(onClick = { updateDisplayApiKeyDialog(true) }) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val emailAddresses = arrayOf(
                                            "fusiondevelopers90@gmail.com",
                                            "kasemsm79@gmail.com"
                                        )
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data =
                                                Uri.parse("mailto:${emailAddresses.joinToString(",")}")
                                        }
                                        startActivity(
                                            Intent.createChooser(
                                                emailIntent,
                                                "Send email using..."
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Feedback,
                                        contentDescription = "Feedback"
                                    )
                                }

                                IconButton(onClick = { updateIsMenuDisplayed(true) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings"
                                    )
                                }

                                UserProfileView(
                                    currentUserProfile = firebaseAuth.currentUser?.photoUrl?.toString(),
                                    onUserProfileClick = {
                                        if (firebaseAuth.currentUser == null) {
                                            val signInIntent = viewModel.requestSignIn()
                                            signInCallback.launch(signInIntent)
                                        }
                                    },
                                    signInState = state.signInState
                                )
                            }
                        )
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = currentRoute != Screen.Pricing.route,
                            exit = fadeOut(tween(400)),
                            enter = fadeIn(tween(400))
                        ) {
                            NavigationBar {
                                navigationItems.forEach { item ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                modifier = Modifier.size(22.dp),
                                                contentDescription = stringResource(id = item.title),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(id = item.title),
                                                fontFamily = InterFontFamily
                                            )
                                        },
                                        selected = currentRoute == item.screen.route,
                                        onClick = {
                                            navController.navigate(item.screen)
                                        },
                                        alwaysShowLabel = false
                                    )
                                }
                            }
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    }
                ) { innerPadding ->
                    if (isMenuDisplayed) {
                        SettingsDialog(
                            onDismiss = { updateIsMenuDisplayed(false) },
                            streamResponse = streamResponse,
                            onStreamResponseChange = { value ->
                                viewModel.updateStreamResponse(value)
                                context.showToast(
                                    message = getString(R.string.restart_app_message)
                                )
                            }
                        )
                    }
                    HandyNavHost(
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        onSignInClicked = {
                            signInCallback.launch(viewModel.requestSignIn())
                        },
                        showApiKeyDialog = {
                            updateDisplayApiKeyDialog(true)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    streamResponse: Boolean,
    onStreamResponseChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            stickyHeader {
                Text(
                    text = "Settings",
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .5.sp
                )
            }
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stream_response_setting),
                            fontFamily = InterFontFamily
                        )
                        Switch(checked = streamResponse, onCheckedChange = onStreamResponseChange)
                    }
                    Text(
                        text = stringResource(R.string.stream_response_settings_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(.6f)
                    )
                }
            }
            item {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(.1f))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = stringResource(R.string.privacy_policy_menu_title),
                        fontFamily = InterFontFamily,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                context.openUrl(url = context.getString(R.string.privacy_policy_url))
                            }
                            .padding(4.dp),
                        fontSize = 12.sp
                    )
                    Text(
                        text = stringResource(R.string.open_source_license_menu_title),
                        fontFamily = InterFontFamily,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                val intent = Intent(
                                    context,
                                    OssLicensesMenuActivity::class.java
                                )
                                context.startActivity(intent)
                            }
                            .padding(4.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
