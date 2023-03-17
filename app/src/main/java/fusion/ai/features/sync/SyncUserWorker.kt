package fusion.ai.features.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fusion.ai.billing.Plan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.features.signin.datasource.network.repository.UserRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class SyncUserWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workParams) {

    override suspend fun doWork(): Result {
        // starts from 0
        if (runAttemptCount >= MAXIMUM_RETRIES) return Result.failure()

        val userId = settingsDataStore.getUserId.first() ?: return Result.failure()

        val response = userRepository.syncUser(userId).getOrElse {
            Timber.e(it)
            return Result.retry()
        }

        Timber.d("$TAG $response")

        return if (response.success) {
            val data = requireNotNull(response.data)
            data.apply {
                settingsDataStore.updatePremiumStatus(
                    status = plan != Plan.Trial.id,
                    plan = when (plan) {
                        Plan.Monthly.id -> Plan.Monthly
                        Plan.Lifetime.id -> Plan.Lifetime
                        else -> Plan.Trial
                    }
                )
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val MAXIMUM_RETRIES = 3
        const val TAG = "sync-user-task"
    }
}
