package rahimklaber.me.routes

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import rahimklaber.me.models.InfoResponse
import rahimklaber.me.repositories.BalanceRepository
import rahimklaber.me.repositories.UserRepository
import rahimklaber.me.services.WalletService

object Info {

    fun Routing.info(){
        authenticate("auth-jwt") {
            get("/info"){
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.subject ?: throw Error("Jwt principal is null")
                val balance = BalanceRepository.findByUsername(username)
                if(balance == null){
                    call.respond(HttpStatusCode.Unauthorized,"")
                }else{
                    val address = WalletService.muxedAddressFromId(balance.muxedId)
                    call.respond(HttpStatusCode.OK,InfoResponse(address,balance.balance.toString()))
                }
            }
        }
    }

    operator fun invoke(router: Routing){
        router.info()
    }

}