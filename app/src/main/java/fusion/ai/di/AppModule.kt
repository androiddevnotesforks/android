package fusion.ai.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fusion.ai.billing.BillingRepository
import fusion.ai.billing.api.PurchaseVerifier
import fusion.ai.datasource.cache.Database
import fusion.ai.features.billing.datasource.network.worker.PurchaseVerifierImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.gson.gson
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindPurchaseApi(bind: PurchaseVerifierImpl): PurchaseVerifier

    companion object {
        @Provides
        @Singleton
        fun provideHttpClient(): HttpClient {
            return HttpClient(CIO) {
                install(Logging)
                install(WebSockets)
                install(ContentNegotiation) {
                    // I had to use Gson :/
                    gson {
                        setPrettyPrinting()
                    }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = TimeUnit.SECONDS.toMillis(60)
                    socketTimeoutMillis = TimeUnit.SECONDS.toMillis(60)
                    connectTimeoutMillis = TimeUnit.SECONDS.toMillis(60)
                }
            }
        }

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }

        @Provides
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
            return WorkManager.getInstance(context)
        }

        @Provides
        @Singleton
        fun provideDatabase(
            app: Application
        ): Database =
            Room.databaseBuilder(app, Database::class.java, "handy_db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        @Singleton
        fun provideBillingRepository(
            app: Application,
            purchaseVerifier: PurchaseVerifier
        ): BillingRepository {
            return BillingRepository(
                application = app,
                purchaseVerifier = purchaseVerifier
            )
        }

        @Provides
        @Singleton
        fun provideGson() = Gson()
    }
}
