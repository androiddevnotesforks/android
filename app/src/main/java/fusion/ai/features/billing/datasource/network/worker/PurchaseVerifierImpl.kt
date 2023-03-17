package fusion.ai.features.billing.datasource.network.worker

import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import fusion.ai.billing.Security
import fusion.ai.billing.api.PurchaseVerifier
import javax.inject.Inject

class PurchaseVerifierImpl @Inject constructor(
    private val verifyPurchaseManager: VerifyPurchaseManager,
    private val gson: Gson
) : PurchaseVerifier {
    override suspend fun verifyServerSide(
        purchase: Purchase
    ) {
        verifyPurchaseManager.execute(purchase = gson.toJson(purchase))
    }

    override fun verifyLocally(
        base64PublicKey: String,
        signedData: String?,
        signature: String?
    ): Boolean {
        return Security.verifyPurchase(base64PublicKey, signedData, signature)
    }
}
