package rahimklaber.me

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.stellar.sdk.KeyPair
import rahimklaber.me.models.InfoResponse
import rahimklaber.me.models.UserModel
import rahimklaber.me.services.WalletService
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class AuthTest {

    private fun Application.setupEnv() {
        if(!Files.exists(Path("test")))
            Files.createDirectory(Path("test"))
        (environment.config as MapApplicationConfig).apply {
            put("jwt.audience", "test")
            put("jwt.realm", "test")
            put("jwt.domain", "test")
            put("stellar.secret", "SAKAV6SBYWF72ELNN5M4LDZDTQLLW6DOZNEVEKS6VBBBIHHXZFV4FWZE")
            put("database.dbname", "test/test-${Random.nextLong()}-.db")
        }
    }

    @Test
    fun `should return unauthorized when calling info without api key`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Get, "/info").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `should return 200 if info request successful`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }
            val apiKey = Json.parseToJsonElement(handleRequest(HttpMethod.Post, "/login") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }.response.content!!).jsonObject["apikey"]!!.jsonPrimitive.content
            handleRequest(HttpMethod.Get, "/info") {
                addHeader("Authorization", "Bearer $apiKey")
            }.apply {
                val infoResponse = Json.decodeFromString<InfoResponse>(response.content!!)

                assertEquals(WalletService.muxedAddressFromId(1), infoResponse.address)
                assertEquals("0.0", infoResponse.balance)
            }
        }
    }

    @Test
    fun `should return a 204 if successfully registered`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }

    }

    @Test
    fun `should return a 409 when registering if account already exists`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
        }
    }

    @Test
    fun `should return 400 if body is invalid when registering`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody("")
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun `should return 200 and apikey if login successful`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }
            handleRequest(HttpMethod.Post, "/login") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                val apiKeyResponse = Json.parseToJsonElement(response.content!!).jsonObject
                val jwtString = apiKeyResponse["apikey"]!!.jsonPrimitive.content
                val jwt = JWT.decode(jwtString)
                assertEquals("testUser", jwt.subject)
                val verifyResult = kotlin.runCatching {
                    JWT.require(Algorithm.HMAC256(JwtConfig.secret)).build().verify(jwt)
                }
                if (verifyResult.isFailure) {
                    fail("Jwt is invalid")
                }
            }
        }
    }

    @Test
    fun `should return 400 if body invalid when logging in`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }
            handleRequest(HttpMethod.Post, "/login") {
                setBody("")
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
        }
    }

    @Test
    fun `should return 401 if credentials are incorrect when logging in`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post, "/register") {
                setBody(Json.encodeToString(UserModel("testUser", "testPass")))
                addHeader("Content-Type", "application/json")
            }
            handleRequest(HttpMethod.Post, "/login") {
                setBody(Json.encodeToString(UserModel("testUser", "invalidpass")))
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }


}