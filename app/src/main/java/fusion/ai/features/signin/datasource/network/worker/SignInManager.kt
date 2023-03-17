package fusion.ai.features.signin.datasource.network.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fusion.ai.util.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

class SignInManager @Inject constructor(
    private val workManager: WorkManager
) {
    fun getSignInStatus(tokenId: String): Flow<Result<Unit>> {
        return channelFlow {
            val request = buildRequest(tokenId)
            request.enqueueWorker()

            workManager.getWorkInfoByIdLiveData(request.id)
                .asFlow()
                .collectLatest { workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> send(Result.success(Unit))
                            WorkInfo.State.FAILED -> send(Result.failure(UnknownError()))
                            else -> Unit
                        }
                    }
                }
        }
    }

    private fun OneTimeWorkRequest.enqueueWorker() {
        workManager.enqueueUniqueWork(
            SignInWorker.TAG,
            ExistingWorkPolicy.KEEP,
            this
        )
    }

    private fun buildRequest(tokenId: String): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<SignInWorker>()
            .setInputData(SignInWorker.buildData(tokenId))
            .addTag(SignInWorker.TAG)
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
