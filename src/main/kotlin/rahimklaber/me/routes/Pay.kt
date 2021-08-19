package rahimklaber.me.routes

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rahimklaber.me.models.PayRequestModel
import rahimklaber.me.repositories.BalanceRepository
import rahimklaber.me.services.WalletService


object Pay {

    fun Routing.pay(){
        authenticate("auth-jwt") {
            post("/pay") {
                val principal = call.principal<JWTPrincipal>()
                val payRequest = call.receive<PayRequestModel>()
                val username = principal?.payload?.subject ?: throw Error("Jwt principal is null")
                val balance = withContext(Dispatchers.IO){ BalanceRepository.findByUsername(username)}
                val response = WalletService.pay(balance.muxedId,payRequest.destination,payRequest.amount.toFloat())

                call.respond(response.statusCode,"")
            }
        }
    }

    operator fun invoke(routing: Routing){
        routing.pay()
    }
}