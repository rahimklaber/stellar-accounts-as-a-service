package rahimklaber.me.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.Result
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import rahimklaber.me.JwtConfig
import rahimklaber.me.models.Balance
import rahimklaber.me.models.User
import rahimklaber.me.models.UserModel
import java.util.*

enum class RegisterResponse(val responseCode: HttpStatusCode){
    Success(HttpStatusCode.NoContent),
    ExistingAccount(HttpStatusCode.Conflict),
    InvalidUsernameOrPassword(HttpStatusCode.BadRequest) // for if username or password not "strong" enough.
}

sealed class LoginResponse(val response: HttpStatusCode, val token : String){
    class Success(token: String): LoginResponse(HttpStatusCode.OK,token)
    class InvalidCredentials : LoginResponse(HttpStatusCode.Unauthorized,"")
}

/**
 * Check whether a password and username are "strong" enough and that they don't contain unexpected input.
 *
 */
open class CredentialsChecker(){
    /**
     * @return True if valid, false otherwise
     */
    open fun evaluateUsername(username: String) : Boolean{
        if(username.length >  18){
            return false
        }
        return true
    }

    open fun evaluatePassword(password: String) : Boolean{
        if(password.length < 6){
            return false
        }
        return true
    }

}

object AuthService {
    var credentialsChecker : CredentialsChecker = CredentialsChecker()

    operator fun invoke(credentialsChecker: CredentialsChecker){
        this.credentialsChecker = credentialsChecker
    }

    fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    /**
     * Check if the given password matches the database hash.
     *
     * @return true if it matches else false.
     */
    fun verifyPassword(password: String, passwordHash : String) : Boolean{
        val res =  BCrypt.verifyer().verify(password.toCharArray(),passwordHash)
        return res.verified
    }

    fun login(name: String, password: String): LoginResponse {
        val res = transaction {
            runCatching {
                val user = User.select {
                    User.name eq name
                }.first()
                UserModel(user[User.name],user[User.password])
            }
        }
        if(res.isFailure){
            return LoginResponse.InvalidCredentials()
        }
        val user = res.getOrThrow()
        val checkPassword = verifyPassword(password,user.password)

        return when(checkPassword){
            true -> {
                val token = JWT.create()
                    .withAudience(JwtConfig.audience)
                    .withIssuer(JwtConfig.issuer)
                    .withSubject(user.username)
                    .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000/*one day*/ ))
                    .sign(Algorithm.HMAC256(JwtConfig.secret))
                LoginResponse.Success(token)
            }
            false -> LoginResponse.InvalidCredentials()
        }
    }

    fun register(name: String, password: String): RegisterResponse{
        if(!credentialsChecker.evaluateUsername(name) || !credentialsChecker.evaluatePassword(password)){
            return RegisterResponse.InvalidUsernameOrPassword
        }
        val hashedPassword = hashPassword(password)
        val tx = transaction {
            kotlin.runCatching {
               val muxedId = Balance.insert {
                   it[this.balance] = "0.0"
               }[Balance.id]

                User.insert {
                    it[this.name] = name
                    it[this.password] = hashedPassword
                    it[this.muxedId] = muxedId
                }

            }
        }
        return when(tx.isSuccess){
            true -> RegisterResponse.Success
            false -> RegisterResponse.ExistingAccount
        }
    }
}