package fusion.ai

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import fusion.ai.features.sync.SyncUserManager
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class HandyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var syncUserManager: SyncUserManager

    override fun onCreate() {
        super.onCreate()
        if (firebaseAuth.currentUser != null) {
            syncUserManager.execute()
        }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsLogTree(FirebaseCrashlytics.getInstance()))
        }
    }

    class CrashlyticsLogTree(private val firebaseCrashlytics: FirebaseCrashlytics) : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }

            if (t != null) {
                firebaseCrashlytics.recordException(CrashlyticsNonFatalError("$tag : $message", t))
            } else {
                firebaseCrashlytics.log("$priority/$tag: $message")
            }
        }

        class CrashlyticsNonFatalError constructor(message: String, cause: Throwable) :
            RuntimeException(message, cause)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
