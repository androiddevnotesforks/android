package fusion.ai.features.billing

import android.app.Activity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fusion.ai.BuildConfig
import fusion.ai.R
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
import fusion.ai.billing.PurchaseType
import fusion.ai.billing.toName
import fusion.ai.features.billing.Features.Companion.reformedText
import fusion.ai.ui.theme.InterFontFamily
import fusion.ai.util.openUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillingScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: BillingVM = hiltViewModel()
) {
    val context = LocalContext.current
    val pager = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    val state = viewModel.billingState.collectAsStateWithLifecycle().value

    LaunchedEffect(key1 = state.errorEvent) {
        if (state.errorEvent != null) {
            snackbarHostState.showSnackbar(
                message = context.getString(state.errorEvent.message),
                withDismissAction = true
            )
        }
    }

    LaunchedEffect(
        state.billingState,
        state.enabled,
        state.onClick,
        state.summary,
        state.billingPricing
    ) {
        when (state.billingState) {
            BillingRepository.SkuState.UNKNOWN -> {
                viewModel.updateEnable(false)
                viewModel.updateSummary(context.getString(R.string.settings_buy_button_not_possible))
                viewModel.updateOnClick { }
            }

            BillingRepository.SkuState.NOT_PURCHASED -> {
                viewModel.updateEnable(true)
                viewModel.updateSummary(
                    context.getString(
                        R.string.settings_buy_button_get,
                        Tabs.values()[pager.currentPage].getPlan().first().toName()
                    )
                )
                viewModel.updateOnClick {
                    viewModel.updateEnable(false)
                    viewModel.updateSummary(context.getString(R.string.processing))
                    viewModel.makePurchase(
                        (context as Activity),
                        type = PurchaseType.Premium,
                        Tabs.values()[pager.currentPage].getPlan().first()
                    )
                }
            }

            BillingRepository.SkuState.PENDING -> {
                // Disable the Purchase button and set its text to "Processing..."
                viewModel.updateEnable(false)
                viewModel.updateSummary(context.getString(R.string.purchase_error_pending_payment))
            }

            BillingRepository.SkuState.PURCHASED -> {
                // Already bought, but not yet acknowledged by the app.
                // This should never happen, as it's already handled within BillingDataSource.
                viewModel.updateEnable(false)
                viewModel.updateSummary(context.getString(R.string.still_processing))
            }

            BillingRepository.SkuState.PURCHASED_AND_ACKNOWLEDGED -> {
                viewModel.updateEnable(BuildConfig.DEBUG)
                viewModel.updateSummary(
                    context.getString(
                        R.string.settings_buy_button_bought,
                        Tabs.values()[pager.currentPage].getPlan().first().toName()
                    )
                )
            }

            else -> Unit
        }
    }

    Column {
        TabRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .then(modifier),
            selectedTabIndex = pager.currentPage,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                TabIndicator(currentTabPosition = tabPositions[pager.currentPage])
            },
            divider = { }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    title = tab.name,
                    textColor = if (index == pager.currentPage) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                            .copy(alpha = .5f)
                    },
                    onClick = {
                        coroutineScope.launch {
                            pager.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
        HorizontalPager(pageCount = tabs.size, state = pager) {
            BillingContent(
                stablePlans = StablePlans(Tabs.values()[it].getPlan()),
                state = state,
                consumePurchase = {
                }
            )
        }
    }
}

@Immutable
data class StablePlans(
    val values: List<Plan>
)

@Composable
fun BillingContent(
    stablePlans: StablePlans,
    state: BillingVMState,
    modifier: Modifier = Modifier,
    consumePurchase: () -> Unit
) {
    val context = LocalContext.current
    val plan = stablePlans.values

    val (selectedPlan, updateSelectedPlan) = remember {
        mutableStateOf(plan.first())
    }

    LazyColumn(
        modifier = Modifier
            .then(modifier)
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        items(selectedPlan.getRewards()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = it.isAvailable,
                    onCheckedChange = {},
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = it.reformedText,
                    fontFamily = InterFontFamily,
                    letterSpacing = .5.sp,
                    fontSize = 14.sp
                )
            }
        }

        item {
            if (selectedPlan == Plan.ThreeMonthly) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.lifetime_disclaimer),
                            fontFamily = InterFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            letterSpacing = .5.sp,
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    context.openUrl(context.getString(R.string.api_key_guide_url))
                                },
                                modifier = Modifier.weight(.3f),
                                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.guide),
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    context.openUrl(context.getString(R.string.get_api_key_url))
                                },
                                modifier = Modifier.weight(.4f),
                                shape = RoundedCornerShape(0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.get_api_key),
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    context.openUrl(context.getString(R.string.api_key_faqs_url))
                                },
                                modifier = Modifier.weight(.3f),
                                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.faq_s),
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    plan.forEach {
                        PricingCard(
                            plan = selectedPlan,
                            isSelected = true,
                            productPricing = state.billingPricing,
                            updateSelection = {
                                updateSelectedPlan(it)
                            },
                            modifier = Modifier.weight(.5f),
                            isEnabled = state.enabled
                        )
                    }
                }
                BillingInfoView(
                    title = when {
                        state.billingState === BillingRepository.SkuState.PURCHASED_AND_ACKNOWLEDGED -> if (BuildConfig.DEBUG) "Consume Purchase/Subscription" else "Subscribed"
                        else -> stringResource(R.string.get_handyai_premium)
                    },
                    description = state.summary,
                    isEnabled = state.enabled
                ) {
                    when {
                        state.billingState === BillingRepository.SkuState.PURCHASED_AND_ACKNOWLEDGED -> if (BuildConfig.DEBUG) consumePurchase() else Unit
                        else -> state.onClick()
                    }
                }
            }
        }
    }
}

@Composable
fun BillingInfoView(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = isEnabled
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = description,
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
        )
    }
}

private val tabs = Tabs.values()

private enum class Tabs {
    Limited, Unlimited, Advance;

    fun getPlan(): List<Plan> {
        return when (this) {
            Unlimited -> listOf(Plan.Monthly)
            Limited -> listOf(Plan.Tokens10K)
            Advance -> listOf(Plan.ThreeMonthly)
        }
    }
}
fun Modifier.customTabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    composed {
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset by animateDpAsState(
            targetValue = currentTabPosition.left,
            animationSpec = tween(),
            label = ""
        )
        fillMaxSize()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .width(currentTabWidth)
    }

@Composable
fun TabIndicator(
    currentTabPosition: TabPosition,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Box(
        modifier = modifier
            .zIndex(1f)
            .fillMaxSize()
            .customTabIndicatorOffset(currentTabPosition)
            .fillMaxSize()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
fun Tab(
    title: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { }
) {
    Box(
        modifier = modifier
            .zIndex(2f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(
                color = Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(
                vertical = 8.dp
            ),
            text = title,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
