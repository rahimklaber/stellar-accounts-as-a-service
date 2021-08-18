package rahimklaber.me.plugins

import io.ktor.auth.*
import io.ktor.util.*
import io.ktor.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import rahimklaber.me.JwtConfig
import kotlin.random.Random

fun Application.configureSecurity() {

    authentication {
        jwt("auth-jwt") {
            val jwtAudience = environment.config.property("jwt.audience").getString()
            realm = environment.config.property("jwt.realm").getString()
            val issuer = environment.config.property("jwt.domain").getString()
            JwtConfig(Random.nextBytes(100).toString(),issuer,jwtAudience,realm)
            verifier(
                JWT
                    .require(Algorithm.HMAC256(JwtConfig.secret))
                    .withAudience(jwtAudience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }

}
