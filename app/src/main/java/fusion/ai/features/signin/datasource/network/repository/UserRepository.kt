package fusion.ai.features.signin.datasource.network.repository

import fusion.ai.datasource.network.Endpoints
import fusion.ai.features.signin.datasource.network.dto.HandyUser
import fusion.ai.features.signin.datasource.network.request.VerifyToken
import fusion.ai.util.withResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val client: HttpClient
) {

    suspend fun verifyUser(tokenId: String): Result<HandyUser> {
        return withResult {
            client.post(Endpoints.VerifyUser.build()) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(VerifyToken(tokenId))
            }.body()
        }
    }

    suspend fun syncUser(userId: String): Result<HandyUser> {
        return withResult {
            client.get(Endpoints.UserInfo.build()) {
                parameter("id", userId)
            }.body()
        }
    }
}
