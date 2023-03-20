package fusion.ai.features.signin.datasource.network.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import fusion.ai.billing.Plan
import fusion.ai.billing.toPlan
import fusion.ai.datasource.cache.datastore.SettingsDataStore
import fusion.ai.features.signin.datasource.network.repository.UserRepository
import timber.log.Timber

@HiltWorker
class SignInWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, workParams) {

    override suspend fun doWork(): Result {
        // starts from 0
        if (runAttemptCount >= MAXIMUM_RETRIES) return Result.failure()

        val tokenId = inputData.getString(TOKEN_KEY) ?: return Result.failure()
        Timber.d("Token $tokenId")

        val response = userRepository.verifyUser(tokenId).getOrElse {
            Timber.e(it)
            return Result.retry()
        }

        Timber.d(response.toString())

        return if (response.success) {
            val data = requireNotNull(response.data)
            Timber.d("$TAG $response")
            data.apply {
                settingsDataStore.setUserId(uid)
                settingsDataStore.updatePremiumStatus(
                    status = plan != Plan.Trial.token,
                    plan = plan.toPlan()
                )
            }
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val MAXIMUM_RETRIES = 3
        const val TAG = "sign-in-task"
        const val TOKEN_KEY = "sign-in-token"

        fun buildData(tokenId: String) = Data.Builder()
            .putString(TOKEN_KEY, tokenId)
            .build()
    }
}
