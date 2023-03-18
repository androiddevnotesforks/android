package fusion.ai.datasource.network

import fusion.ai.BuildConfig

sealed class Endpoints(private val url: String) {
    object ChatSocket :
        Endpoints("$BASE_URL/chat-socket")

    object ChatTools :
        Endpoints("$BASE_URL/chat-tools")

    object VerifyUser :
        Endpoints("$BASE_URL/user-verify")

    object UserInfo :
        Endpoints("$BASE_URL/user-info")

    object VerifyPurchase : Endpoints("-1")

    /* For some unknown reason, ktor default request breaks websocket when we configure it to use
    * http or https according to url host */
    fun build(): String {
        val scheme = if (useLocalUrl) "http://" else "https://"
        val socketScheme = if (useLocalUrl) "ws://" else "wss://"
        return when (this) {
            ChatSocket -> "$socketScheme$url"
            VerifyPurchase -> fusion.ai.billing.BuildConfig.PURCHASE_VERIFY_URL
            else -> "$scheme$url"
        }
    }

    companion object {
        const val useLocalUrl = false

        val BASE_URL = if (useLocalUrl) "192.168.0.103:9766/ktor" else BuildConfig.API_URL
    }
}
