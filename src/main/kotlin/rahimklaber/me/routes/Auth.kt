package rahimklaber.me.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.serialization.Serializable
import rahimklaber.me.models.UserModel
import rahimklaber.me.services.AuthService
import rahimklaber.me.services.LoginResponse

object Auth{

    /**
     * Route for handeling registration of users.
     * Todo: validate body
     */
    fun Routing.register(){
        post("/register") {
            val tryDecode = kotlin.runCatching { call.receive<UserModel>() }
            if(tryDecode.isSuccess){
                val user = tryDecode.getOrThrow()
                val response = AuthService.register(user.username,user.password)
                call.respond(response.responseCode,"")
            }else{
                call.respond(HttpStatusCode.BadRequest,"")
            }
        }
    }

    /**
     * Route for handeling the login of users.
     *
     * responds with a jwt token.
     *
     */
    fun Routing.login(){
        post("/login"){
            val tryDecode = kotlin.runCatching { call.receive<UserModel>() }
            if(tryDecode.isSuccess) {
                val user = tryDecode.getOrThrow()
                when (val response = AuthService.login(user.username, user.password)) {
                    is LoginResponse.Success -> call.respond(
                        response.response,
                        mapOf("apikey" to response.token)
                    )
                    else -> call.respond(response.response, "")
                }
            }else{
                call.respond(HttpStatusCode.BadRequest,"")
            }

        }
    }

    operator fun invoke(routing: Routing){
        routing.register()
        routing.login()
    }
}