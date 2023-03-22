/*
 * Copyright (C) 2022, Kasem S.M
 * All rights reserved.
 */
package fusion.ai.billing

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.UiThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import fusion.ai.billing.api.PurchaseVerifier
import java.security.Security
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class BillingRepository(
    application: Application,
    private val purchaseVerifier: PurchaseVerifier
) : DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener {

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(application)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    /**
     * How long before the data source tries to reconnect to Google Play
     */
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    /**
     * When was the last successful ProductDetailsResponse?
     */
    private var productDetailsResponseTime = -PRODUCT_DETAILS_RE_QUERY_TIME

    // Maps that are mostly maintained so they can be transformed into observables
    private val skuStateMap = mutableMapOf<String, MutableStateFlow<SkuState>>()
    private val productDetailsMap = mutableMapOf<String, MutableStateFlow<ProductDetails?>>()
    private val purchaseConsumptionInProcess = mutableSetOf<Purchase>()

    val localBillingState = MutableStateFlow(SkuState.UNKNOWN)

    /**
     * Flow of the [premium membership] SKU's [monthlyPrice] and [threeMonthlyPrice]
     */
    val monthlyPrice
        get() = getSubscriptionPrice(Plan.Monthly.token).distinctUntilChanged()

    val threeMonthlyPrice
        get() = getSubscriptionPrice(Plan.ThreeMonthly.token).distinctUntilChanged()

    val tokens10Price
        get() = getSubscriptionPrice(Plan.Tokens10K.token).distinctUntilChanged()

    /**
     * Flow of the [premium membership][PurchaseType.Premium] SKU's [SkuState]
     */
    val subscriptionState
        get() = getSkuState().distinctUntilChanged()

    /**
     * A Flow that reports on purchases that are in the [PurchaseState.PENDING]
     * state, e.g. if the user chooses an instrument that needs additional steps between
     * when they initiate the purchase, and when the payment method is processed.
     *
     * @see <a href="https://developer.android.com/google/play/billing/integrate#pending">Handling pending transactions</a>
     * @see [setSkuStateFromPurchase]
     */
    private val _pendingPurchase = MutableStateFlow<Purchase?>(null)

    @Suppress("unused")
    val pendingPurchase: StateFlow<Purchase?>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _pendingPurchase

    /**
     * A Flow that reports on purchases that have changed their state,
     * e.g. [PurchaseState.PENDING] -> [PurchaseState.PURCHASED].
     *
     * @see [setSkuStateFromPurchase]
     */
    private val _purchaseStateChange = MutableStateFlow<Purchase?>(null)

    @Suppress("unused")
    val purchaseStateChange: StateFlow<Purchase?>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _purchaseStateChange

    private val _newPurchase = MutableSharedFlow<Pair<Int, Purchase?>>(extraBufferCapacity = 1)
    val newPurchase = _newPurchase.distinctUntilChanged().onEach { (responseCode, purchase) ->
        log { "$TAG, [newPurchaseFlow] $responseCode: ${purchase?.products}" }
    }

    /**
     * A Flow that reports if a billing flow is in process, meaning that
     * [launchBillingFlow] has returned a successful [BillingResponseCode.OK],
     * but [onPurchasesUpdated] hasn't been called yet.
     */
    private val _billingFlowInProcess = MutableStateFlow(false)

    @Suppress("unused")
    val billingFlowInProcess: StateFlow<Boolean>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _billingFlowInProcess

    private val _consumedPurchaseSkus = MutableSharedFlow<List<String>>()
    private val consumedPurchaseSkus = _consumedPurchaseSkus.distinctUntilChanged().onEach {
        log { "$TAG, [consumedPurchaseSkusFlow] $it" }
        // Take action (e.g. update models) on each consumed purchase
        setProductState(SUB_SKUS.first(), SkuState.UNKNOWN)
    }

    init {
        // Initialize flows for all known SKUs, so that state & details can be
        // observed in ViewModels. This repository exposes mappings that are
        // more useful for the rest of the application (via ViewModels).
        addSkuFlows()

        billingClient.startConnection(this)
    }

    /**
     * Called by initializeFlows to create the various Flow objects we're planning to emit
     *
     * @param skuList a List<String> of SKUs representing purchases and subscriptions
     */
    private fun addSkuFlows(skuList: List<String> = SUB_SKUS) =
        skuList.forEach { sku ->
            val skuState = MutableStateFlow(SkuState.UNKNOWN)
            val details = MutableStateFlow<ProductDetails?>(null)

            // Flow is considered "active" if there's at least one subscriber.
            // `distinctUntilChanged`: ensure we only react to true<->false changes.
            details.subscriptionCount.map { it > 0 }.distinctUntilChanged().onEach { active ->
                if (active && (SystemClock.elapsedRealtime() - productDetailsResponseTime > PRODUCT_DETAILS_RE_QUERY_TIME)) {
                    log { "$TAG, [addSkuFlows] stale SKUs; re-querying" }
                    productDetailsResponseTime = SystemClock.elapsedRealtime()
                    queryProductDetails()
                }
            }.launchIn(mainScope) // launch it

            skuStateMap[sku] = skuState
            productDetailsMap[sku] = details
        }

    /**
     * It's recommended to re-query purchases during onResume
     */
    override fun onResume(owner: LifecycleOwner) {
        log { "$TAG, [onResume]" }
        // Avoids an extra purchase refresh after we finish a billing flow
        if (!_billingFlowInProcess.value && billingClient.isReady) {
            ioScope.launch {
                refreshPurchases()
            }
        }
    }

    /**
     * Called by [BillingClient] when new purchases are detected; typically in
     * response to [launchBillingFlow]
     *
     * @param result billing result of the purchase flow
     * @param list of new purchases
     */
    override fun onPurchasesUpdated(result: BillingResult, list: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> list?.let {
                processPurchaseList(it, null)
                return
            } ?: log { "$TAG, [onPurchasesUpdated] null purchase list returned from OK response" }

            BillingResponseCode.USER_CANCELED -> log { "$TAG, [onPurchasesUpdated] USER_CANCELED" }
            BillingResponseCode.ITEM_ALREADY_OWNED -> log { "$TAG, [onPurchasesUpdated] ITEM_ALREADY_OWNED" }
            BillingResponseCode.DEVELOPER_ERROR -> log { "$TAG, [onPurchasesUpdated] DEVELOPER_ERROR" }
            else -> log { "$TAG, [onPurchasesUpdated] ${result.responseCode}: ${result.debugMessage}" }
        }

        _newPurchase.tryEmit(Pair(result.responseCode, null))

        ioScope.launch {
            _billingFlowInProcess.emit(false)
        }
    }

    /**
     * Rare occurrence; could happen if Play Store self-updates or is force-closed
     */
    override fun onBillingServiceDisconnected() = reconnectWithExponentialBackoff()

    /**
     * Reconnect to [BillingClient] with exponential backoff, with a max of
     * [RECONNECT_TIMER_MAX_TIME_MILLISECONDS]
     */
    private fun reconnectWithExponentialBackoff() = handler.postDelayed(
        { billingClient.startConnection(this) },
        reconnectMilliseconds
    ).let {
        reconnectMilliseconds = min(
            reconnectMilliseconds * 2,
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS
        )
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        val responseCode = result.responseCode
        log { "$TAG, [onBillingSetupFinished] $responseCode: ${result.debugMessage}" }
        when (responseCode) {
            BillingResponseCode.OK -> ioScope.launch {
                queryProductDetails()
                refreshPurchases()
            }.also {
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
            }

            else -> reconnectWithExponentialBackoff()
        }
    }

    /**
     * GPBLv3 queried purchases synchronously, while v4 supports async.
     *
     * Note that the billing client only returns active purchases.
     */
    private suspend fun refreshPurchases() = withContext(Dispatchers.IO) {
        refreshSubscriptions()
    }

    private suspend fun refreshSubscriptions() {
        log { "$TAG [refreshSubscriptions] start" }

        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val billingResult = purchasesResult.billingResult
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        if (responseCode == BillingResponseCode.OK) {
            log { "$TAG [refreshSubscriptions] Response code OK ${purchasesResult.purchasesList}" }
            processPurchaseList(purchasesResult.purchasesList, SUB_SKUS)
        } else {
            log { "$TAG [refreshSubscriptions] Subs $responseCode: $debugMessage" }
        }

        log { "$TAG, [refreshSubscriptions] finish" }
    }

    fun makePurchase(activity: Activity, sku: String, plan: Plan) {
        launchBillingFlow(activity, sku, plan)
    }

    /**
     * Launch the billing flow. This will launch an external Activity for a result, so it requires
     * an Activity reference. For subscriptions, it supports upgrading from one SKU type to another
     * by passing in SKUs to be upgraded.
     *
     * @param activity active activity to launch our billing flow from
     * @param sku SKU (Product ID) to be purchased
     *
     * @return true if launch is successful
     */
    @UiThread
    fun launchBillingFlow(
        activity: Activity,
        sku: String,
        plan: Plan
    ) = productDetailsMap[sku]?.value?.let { details ->
        val offerToken = details.subscriptionOfferDetails?.find { product ->
            product.basePlanId == plan.getPlanPurchaseToken()
        }?.offerToken ?: kotlin.run {
            Timber.e("Offer Token null")
            return@let
        }

        val builder: BillingFlowParams.Builder = BillingFlowParams.newBuilder()
        builder.setProductDetailsParamsList(
            listOf(
                ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
            )
        )

        mainScope.launch {
            val result = billingClient.launchBillingFlow(activity, builder.build())
            if (result.responseCode == BillingResponseCode.OK) {
                _billingFlowInProcess.emit(true)
            } else {
                log { "$TAG, [launchBillingFlow] $result  ${result.debugMessage}" }
            }
        }
    } ?: log { "$TAG, [launchBillingFlow] unknown SKU: $sku" }

    /**
     * Receives the result from [queryProductDetails].
     *
     * Store the ProductDetails and post them in the [productDetailsMap]. This allows other
     * parts of the app to use the [ProductDetails] to show SKU information and make purchases.
     */
    private fun onProductDetailsResponse(
        result: BillingResult,
        detailsList: List<ProductDetails>?
    ) {
        val responseCode = result.responseCode
        val debugMessage = result.debugMessage

        when (responseCode) {
            BillingResponseCode.OK -> {
                log { "$TAG, [onProductDetailsResponse] Response code 0: $debugMessage" }
                if (detailsList.isNullOrEmpty()) {
                    log { "$TAG, [onProductDetailsResponse] null/empty List<ProductDetails>" }
                } else {
                    detailsList.forEach {
                        val id = it.productId
                        productDetailsMap[it.productId]?.tryEmit(it)
                            ?: log { "$TAG, [onProductDetailsResponse] unknown product: $id" }
                    }
                }
            }

            BillingResponseCode.USER_CANCELED -> log { "$TAG, [onProductDetailsResponse] USER_CANCELED: $debugMessage" }
            BillingResponseCode.SERVICE_DISCONNECTED -> log { "$TAG, [onProductDetailsResponse] SERVICE_DISCONNECTED: $debugMessage" }
            BillingResponseCode.SERVICE_UNAVAILABLE -> log { "$TAG, [onProductDetailsResponse] SERVICE_UNAVAILABLE: $debugMessage" }
            BillingResponseCode.BILLING_UNAVAILABLE -> log { "$TAG, [onProductDetailsResponse] BILLING_UNAVAILABLE: $debugMessage" }
            BillingResponseCode.ITEM_UNAVAILABLE -> log { "$TAG, [onProductDetailsResponse] ITEM_UNAVAILABLE: $debugMessage" }
            BillingResponseCode.DEVELOPER_ERROR -> log { "$TAG, [onProductDetailsResponse] DEVELOPER_ERROR: $debugMessage" }
            BillingResponseCode.ERROR -> log { "$TAG, [onProductDetailsResponse] ERROR: $debugMessage" }
            BillingResponseCode.FEATURE_NOT_SUPPORTED -> log { "$TAG, [onProductDetailsResponse] FEATURE_NOT_SUPPORTED: $debugMessage" }
            BillingResponseCode.ITEM_ALREADY_OWNED -> log { "$TAG, [onProductDetailsResponse] ITEM_ALREADY_OWNED: $debugMessage" }
            BillingResponseCode.ITEM_NOT_OWNED -> log { "$TAG, [onProductDetailsResponse] ITEM_NOT_OWNED: $debugMessage" }
            else -> {
                log { "$TAG, [onProductDetailsResponse] $responseCode: $debugMessage" }
            }
        }

        productDetailsResponseTime = if (responseCode == BillingResponseCode.OK) {
            SystemClock.elapsedRealtime()
        } else {
            -PRODUCT_DETAILS_RE_QUERY_TIME
        }
    }

    /**
     * Calls the billing client functions to query sku details for the inapp SKUs.
     * SKU details are useful for displaying item names and price lists to the user, and are
     * required to make a purchase.
     */
    private suspend fun queryProductDetails() = withContext(Dispatchers.IO) {
        querySubscriptionDetails()
    }

    private suspend fun querySubscriptionDetails() {
        val products = buildList {
            addAll(
                SUB_SKUS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
        }

        billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()
        ).also {
            onProductDetailsResponse(it.billingResult, it.productDetailsList)
        }
    }

    /**
     * Calling this means that we have the most up-to-date information for an SKU
     * in a purchase object. This uses [PurchaseState]s & the acknowledged state.
     *
     * @param purchase an up-to-date object to set the state for the SKU
     */
    private fun setSkuStateFromPurchase(purchase: Purchase) = purchase.products.forEach {
        val skuStateFlow = skuStateMap[it] ?: kotlin.run {
            log { "$TAG, [setSkuStateFromPurchase] unknown SKU: $it" }
            return@forEach
        }

        val state = purchase.purchaseState
        if ((state == PurchaseState.PURCHASED || state == PurchaseState.PENDING) && !isSignatureValid(
                purchase
            )
        ) {
            log { "$TAG, [setSkuStateFromPurchase] invalid signature" }
            // Don't set SkuState if signature validation fails
            return@forEach
        }

        val oldState = skuStateFlow.value
        when (state) {
            PurchaseState.PENDING -> SkuState.PENDING
            PurchaseState.UNSPECIFIED_STATE -> SkuState.NOT_PURCHASED
            PurchaseState.PURCHASED -> if (purchase.isAcknowledged) {
                SkuState.PURCHASED_AND_ACKNOWLEDGED
            } else {
                SkuState.PURCHASED
            }

            else -> null
        }?.let { newState ->
            if (newState == SkuState.PENDING) {
                _pendingPurchase.tryEmit(purchase)
            } else if (newState != oldState) {
                _purchaseStateChange.tryEmit(purchase)
            }
            skuStateFlow.tryEmit(newState)
        } ?: log { "$TAG, [setSkuStateFromPurchase] unknown purchase state: $state" }
    }

    /**
     * Goes through each purchase and makes sure that the purchase state is processed and the state
     * is available through Flows. Verifies signature and acknowledges purchases. PURCHASED isn't
     * returned until the purchase is acknowledged.
     *
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     *
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     *
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged unless the user has successfully
     * received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     *
     * If a [skusToUpdate] list is passed-into this method, any purchases not in the list of
     * purchases will have their state set to [SkuState.NOT_PURCHASED].
     *
     * @param purchases the List of purchases to process.
     * @param skusToUpdate a list of skus that we want to update the state from --- this allows us
     * to set the state of non-returned SKUs to [SkuState.NOT_PURCHASED].
     */
    private fun processPurchaseList(
        purchases: List<Purchase>?,
        skusToUpdate: List<String>?
    ) {
        val updatedSkus = HashSet<String>()
        purchases?.forEach { purchase ->
            purchase.products.forEach { sku ->
                skuStateMap[sku]?.let {
                    updatedSkus.add(sku)
                } ?: log { "$TAG, [processPurchaseList] unknown SKU: $sku" }
            }

            // Make sure the SkuState is set
            setSkuStateFromPurchase(purchase)

            if (purchase.purchaseState == PurchaseState.PURCHASED) {
                log { "$TAG, [processPurchaseList] found purchase with SKUs: ${purchase.products}" }

                ioScope.launch {
                    val isPromotionalPurchase =
                        purchase.orderId.isEmpty() && purchase.isAcknowledged

                    /** Promotional purchase are already acknowledged */
                    log { "[purchase] $purchase" }
                    if (!purchase.isAcknowledged || isPromotionalPurchase) {
                        log { "Promotional Purchase $isPromotionalPurchase" }
                        try {
                            log { "$[processPurchaseList] acknowledging purchase started" }
                            purchaseVerifier.verifyServerSide(purchase)
                        } catch (e: Exception) {
                            log { "$[error] ${e.message}" }
                        }
                    }
                }
            } else {
                log { "$TAG, [processPurchaseList] Purchase State ${purchase.purchaseState}" }
            }
        } ?: log { "$TAG, [processPurchaseList] null purchase list" }

        // Clear purchase state of anything that didn't come with this purchase list if this is
        // part of a refresh.
        skusToUpdate?.forEach {
            if (productDetailsMap[it]?.value == null) {
                setProductState(it, SkuState.UNKNOWN)
            } else if (!updatedSkus.contains(it)) {
                setProductState(it, SkuState.NOT_PURCHASED)
            }
        }
    }

    /**
     * Since we (mostly) are getting sku states when we actually make a purchase or update
     * purchases, we keep some internal state when we do things like acknowledge or consume.
     *
     * @param sku product ID to change the state of
     * @param newState the new state of the sku
     */
    fun setProductState(
        sku: String,
        newState: SkuState
    ) = skuStateMap[sku]?.tryEmit(newState) ?: log { "$TAG, [setSkuState] unknown SKU: $sku" }

    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary.
     *
     * @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase) = purchaseVerifier.verifyLocally(
        base64Key,
        purchase.originalJson,
        purchase.signature
    )

    private fun getSubscriptionPrice(id: String) = productDetailsMap[SKU_SUBS_Pro]?.map {
        it?.subscriptionOfferDetails?.find { product ->
            product.basePlanId == id
        }?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice
    } ?: flowOf(null).also {
        log { "$TAG, [getSubscriptionPrice] unknown SKU: $SKU_SUBS_Pro" }
    }

    private fun getSkuState(sku: String = SKU_SUBS_Pro) = skuStateMap[sku] ?: flowOf(SkuState.UNKNOWN).also {
        log { "$TAG, [getSkuState] unknown SKU: $sku" }
    }

    /**
     * **Only for DEBUG use**: consume an in-app purchase so it can be bought again.
     *
     * This helps to rapidly test billing functionality (Play Store caches all
     * purchases the user owns, and it can take a long time for it be evicted).
     *
     * Note that this function should be unused throughout the app, except
     * while under testing by "license testers", in which case a developer
     * sends them an APK (different from what's publicly distributed) — it'll
     * have a button in the UI somewhere that calls this function.
     *
     * License testers are shown a few test instruments by Google Play (e.g.
     * always approves, always declines, approves after delay, etc).
     *
     * @see <a href="https://developer.android.com/google/play/billing/test">Test Google Play Billing Library integration</a>
     */
    @Suppress("Unused")
    fun debugConsume() = CoroutineScope(Dispatchers.IO).launch {
        if (BuildConfig.DEBUG) {
            consumeInAppPurchase(SKU_SUBS_Pro)
        }
    }

    /**
     * Consumes an in-app purchase. Interested listeners can watch the [consumedPurchaseSkus] LiveEvent.
     * To make things easy, you can send in a list of SKUs that are auto-consumed by the
     * [BillingRepository].
     */
    private suspend fun consumeInAppPurchase(
        sku: String
    ) = withContext(Dispatchers.IO) {
        when (sku) {
            SKU_SUBS_Pro -> {
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                ).let { result ->
                    val billingResult = result.billingResult
                    val purchasesList = result.purchasesList
                    val responseCode = billingResult.responseCode
                    if (responseCode == BillingResponseCode.OK) {
                        purchasesList.forEach { purchase ->
                            // For right now any bundle of SKUs must all be consumable
                            purchase.products.find { it == sku }?.also {
                                consumePurchase(purchase)
                                return@let
                            }
                        }
                    } else {
                        log { "$TAG, [consumeInAppPurchase] $responseCode: ${billingResult.debugMessage}" }
                    }

                    log { "$TAG, [consumeInAppPurchase] unknown SKU: $sku" }
                }
            }
            else -> throw IllegalAccessException()
        }
    }

    /**
     * Internal call only. Assumes that all signature checks have been completed and the purchase
     * is ready to be consumed. If the sku is already being consumed, does nothing.
     * @param purchase purchase to consume
     */
    private suspend fun consumePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        // weak check to make sure we're not already consuming the sku
        if (purchaseConsumptionInProcess.contains(purchase)) {
            // already consuming
            return@withContext
        }

        purchaseConsumptionInProcess.add(purchase)
        val consumePurchaseResult = billingClient.consumePurchase(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        )
        purchaseConsumptionInProcess.remove(purchase)

        when (consumePurchaseResult.billingResult.responseCode) {
            BillingResponseCode.OK -> {
                log { "$TAG, [consumePurchase] successful, emitting SKU" }
                _consumedPurchaseSkus.emit(purchase.products)
                // Since we've consumed the purchase
                purchase.products.forEach {
                    setProductState(it, SkuState.NOT_PURCHASED)
                }
            }

            BillingResponseCode.ITEM_NOT_OWNED -> {
                log { "$TAG, [consumePurchase] Item is not owned by the user" }
                purchase.products.forEach {
                    setProductState(it, SkuState.NOT_PURCHASED)
                }
            }

            else -> {
                log { "$TAG, [consumePurchase] ${consumePurchaseResult.billingResult.debugMessage}" }
            }
        }
    }

    fun updateNewPurchaseAndConsume(
        responseCode: Int,
        purchase: Purchase,
    ) {
        _newPurchase.tryEmit(Pair(responseCode, purchase))
    }

    fun updateLocalBillingState(state: SkuState) {
        localBillingState.update { state }
    }

    enum class SkuState {
        UNKNOWN,
        NOT_PURCHASED,
        PENDING,
        PURCHASED,
        PURCHASED_AND_ACKNOWLEDGED
    }

    companion object {
        fun log(message: () -> String) {
            Timber.d("BillingRepository ${message()}")
        }

        private const val TAG = "BillingRepository"

        /**
         * 1 second
         */
        private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L

        /**
         * 15 minutes
         */
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L

        /**
         * 4 hours
         */
        private const val PRODUCT_DETAILS_RE_QUERY_TIME = 1000L * 60L/* * 60L * 4L*/

        private val SKU_SUBS_Pro = PurchaseType.Premium.sku

        private val SUB_SKUS = listOf(SKU_SUBS_Pro)

        private val handler = Handler(Looper.getMainLooper())

        private const val base64Key = BuildConfig.BASE_64_KEY
    }
}
