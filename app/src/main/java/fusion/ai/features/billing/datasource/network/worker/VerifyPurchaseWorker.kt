package fusion.ai.features.billing.datasource.network.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.Plan
import fusion.ai.billing.toPlan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.datasource.network.Endpoints
import fusion.ai.features.billing.datasource.network.dto.VerifyPurchaseBody
import fusion.ai.features.billing.datasource.network.dto.VerifyPurchaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class VerifyPurchaseWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workParams: WorkerParameters,
    private val billingRepository: BillingRepository,
    private val settingsDataStore: SettingsDataStore,
    private val httpClient: HttpClient,
    private val gson: Gson
) : CoroutineWorker(context, workParams) {

    override suspend fun doWork(): Result {
        // starts from 0
        if (runAttemptCount >= MAXIMUM_RETRIES) return Result.failure()

        val purchase = gson.fromJson(
            inputData.getString(PURCHASE_CLASS_KEY),
            Purchase::class.java
        ) ?: return Result.failure()

        Timber.d("Purchase Class $purchase")

        val productId = purchase.products.firstOrNull() ?: return Result.failure()

        val purchaseResult = httpClient.post(Endpoints.VerifyPurchase.build()) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                VerifyPurchaseBody(
                    userId = settingsDataStore.getUserId.first() ?: throw IllegalAccessException(),
                    productId = productId,
                    purchaseToken = purchase.purchaseToken
                )
            )
        }.body<VerifyPurchaseResponse>()

        Timber.d("Purchase Result $purchaseResult")

        return if (purchaseResult.success) {
            purchase.products.forEach {
                val userData = requireNotNull(purchaseResult.handyUser?.data)
                Timber.d("Products verified $it")
                settingsDataStore.updatePremiumStatus(
                    status = userData.plan != Plan.Trial.token,
                    plan = userData.plan.toPlan()
                )
                billingRepository.setProductState(
                    it,
                    BillingRepository.SkuState.PURCHASED_AND_ACKNOWLEDGED
                )
            }
            billingRepository.updateNewPurchaseAndConsume(
                responseCode = BillingClient.BillingResponseCode.OK,
                purchase = purchase
            )
            return Result.success()
        } else {
            purchase.products.forEach {
                billingRepository.setProductState(
                    it,
                    BillingRepository.SkuState.NOT_PURCHASED
                )
                billingRepository.updateNewPurchaseAndConsume(
                    responseCode = BillingClient.BillingResponseCode.ERROR,
                    purchase = purchase
                )
            }
            Timber.d("Error verifying or acknowledging")
            Result.retry()
        }
    }

    companion object {
        private const val MAXIMUM_RETRIES = 3
        const val TAG = "verify-purchase-worker"
        const val PURCHASE_CLASS_KEY = "verify-purchase-purchase"

        fun buildData(purchase: String) = Data.Builder()
            .putString(PURCHASE_CLASS_KEY, purchase)
            .build()
    }
}
