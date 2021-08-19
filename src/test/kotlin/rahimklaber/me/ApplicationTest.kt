package rahimklaber.me

import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import rahimklaber.me.plugins.configureRouting
import rahimklaber.me.plugins.configureSecurity
import kotlin.test.Test
import kotlin.test.assertEquals


class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                put("jwt.audience", "test")
                put("jwt.realm", "test")
                put("jwt.domain", "test")
            }
            module()
        }) {
            handleRequest(HttpMethod.Get, "/info").apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
}