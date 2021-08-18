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
            val user = call.receive<UserModel>()
            val response = AuthService.register(user.username,user.password)
            call.respond(status = response.responseCode,"")
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
            val user = call.receive<UserModel>()
            val response = AuthService.login(user.username,user.password)
            when(response){
                is LoginResponse.Success -> call.respond(response.response, mapOf("apikey" to response.token))
                else -> call.respond(response.response,"")
            }

        }
    }

    operator fun invoke(routing: Routing){
        routing.register()
        routing.login()
    }
}