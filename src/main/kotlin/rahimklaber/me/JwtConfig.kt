package rahimklaber.me

import io.ktor.application.ApplicationEnvironment

object JwtConfig {
    lateinit var secret : String
    lateinit var issuer : String
    lateinit var audience : String
    lateinit var realm : String

    operator fun invoke(secret: String, issuer: String, audience: String, realm : String){
        this.secret = secret
        this.issuer = issuer
        this.audience = audience
        this.realm = realm
    }
}