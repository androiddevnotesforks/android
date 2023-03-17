package fusion.ai.billing.api

import com.android.billingclient.api.Purchase

interface PurchaseVerifier {
    suspend fun verifyServerSide(purchase: Purchase)

    fun verifyLocally(
        base64PublicKey: String,
        signedData: String?,
        signature: String?
    ): Boolean
}
