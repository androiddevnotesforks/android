package fusion.ai.features.billing.datasource.network.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject

class VerifyPurchaseManager @Inject constructor(
    private val workManager: WorkManager
) {
    fun execute(purchase: String) {
        val request = buildRequest(purchase)
        request.enqueueWorker()
    }

    private fun OneTimeWorkRequest.enqueueWorker() {
        workManager.enqueueUniqueWork(
            VerifyPurchaseWorker.TAG,
            ExistingWorkPolicy.KEEP,
            this
        )
    }

    private fun buildRequest(purchase: String): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<VerifyPurchaseWorker>()
            .setInputData(VerifyPurchaseWorker.buildData(purchase))
            .addTag(VerifyPurchaseWorker.TAG)
            .setConstraints(getConstraints())
            .build()
    }

    companion object {
        private fun getConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}
