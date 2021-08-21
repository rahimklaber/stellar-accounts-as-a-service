package rahimklaber.me

import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import rahimklaber.me.models.Balance
import rahimklaber.me.models.PayRequestModel
import rahimklaber.me.models.ProcessedOperations
import rahimklaber.me.models.UserModel
import rahimklaber.me.plugins.configureRouting
import rahimklaber.me.plugins.configureSecurity
import rahimklaber.me.services.WalletService
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals


class PayTest {
    private fun Application.setupEnv() {
        (environment.config as MapApplicationConfig).apply {
            put("jwt.audience", "test")
            put("jwt.realm", "test")
            put("jwt.domain", "test")
            put("database.dbname", "test/test-${Random.nextLong()}-.db")
        }
    }

    private fun TestApplicationEngine.login(): String {
        handleRequest(HttpMethod.Post, "/register") {
            setBody(Json.encodeToString(UserModel("testUser", "testPass")))
            addHeader("Content-Type", "application/json")
        }
        val apiKey = Json.parseToJsonElement(handleRequest(HttpMethod.Post, "/login") {
            setBody(Json.encodeToString(UserModel("testUser", "testPass")))
            addHeader("Content-Type", "application/json")
        }.response.content!!).jsonObject["apikey"]!!.jsonPrimitive.content
        return apiKey
    }

    @Test
    fun `pay should fail with invalid body and return a 400`() {
        withTestApplication({
           setupEnv()
            module()
        }) {
            val apikey = login()
            handleRequest(HttpMethod.Post,"/pay"){
                addHeader("Authorization", "Bearer $apikey")
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest,response.status())
            }

        }
    }

    @Test
    fun `pay should fail with invalid api key and return a 401`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            handleRequest(HttpMethod.Post,"/pay"){
                addHeader("Authorization", "Bearer ")
                addHeader("Content-Type", "application/json")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized,response.status())
            }

        }
    }

    @Test
    fun `pay should fail with negative amount and return a 400`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            val apikey = login()
            handleRequest(HttpMethod.Post,"/pay"){
                addHeader("Authorization", "Bearer $apikey")
                addHeader("Content-Type", "application/json")
                setBody(Json.encodeToString(PayRequestModel(WalletService.keyPair.accountId,"-100")))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest,response.status())
            }

        }
    }

    @Test
    fun `pay should fail with invalid destination address and return a 404`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            val apikey = login()
            transaction {
                Balance.update({ Balance.id eq 1 }) {
                    it[Balance.balance] =
                        100.0f
                }
            }
            handleRequest(HttpMethod.Post,"/pay"){
                addHeader("Authorization", "Bearer $apikey")
                addHeader("Content-Type", "application/json")
                setBody(Json.encodeToString(PayRequestModel("GAT5TPOKT6CXBJPKBM6GJ6PET56EXWGMCQPXDPSB4BPYL5SO74Q5HWDI","0.001")))
            }.apply {
                assertEquals(HttpStatusCode.NotFound,response.status())
            }

        }
    }

    @Test
    fun `successful payment should return a 200`() {
        withTestApplication({
            setupEnv()
            module()
        }) {
            val apikey = login()
            handleRequest(HttpMethod.Post,"/pay"){
                addHeader("Authorization", "Bearer $apikey")
                addHeader("Content-Type", "application/json")
                setBody(Json.encodeToString(PayRequestModel(WalletService.keyPair.accountId,"1")))
            }.apply {
                assertEquals(HttpStatusCode.NoContent,response.status())
            }

        }
    }


}