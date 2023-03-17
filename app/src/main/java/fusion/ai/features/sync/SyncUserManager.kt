package fusion.ai.features.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject

class SyncUserManager @Inject constructor(
    private val workManager: WorkManager
) {
    fun execute() {
        val request = buildRequest()
        request.enqueueWorker()
    }

    private fun OneTimeWorkRequest.enqueueWorker() {
        workManager.enqueueUniqueWork(
            SyncUserWorker.TAG,
            ExistingWorkPolicy.KEEP,
            this
        )
    }

    private fun buildRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<SyncUserWorker>()
            .addTag(SyncUserWorker.TAG)
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
