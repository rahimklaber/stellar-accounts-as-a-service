package rahimklaber.me.routes

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.valiktor.functions.contains
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate
import rahimklaber.me.models.PayRequestModel
import rahimklaber.me.repositories.BalanceRepository
import rahimklaber.me.services.WalletPayRequest
import rahimklaber.me.services.WalletService
import rahimklaber.me.toProjectBigDecimal
import java.math.BigDecimal
import java.math.MathContext


object Pay {

    fun Routing.pay() {
        authenticate("auth-jwt") {
            post("/pay") {
                val principal = call.principal<JWTPrincipal>()
                val tryDecode = runCatching {
                    val body = call.receive<PayRequestModel>()
                    val amount = body.amount.toBigDecimal()
                    require(
                        (amount.compareTo(
                            BigDecimal.ZERO
                        ) == 1) // amount > 0
                                &&
                                (amount.scale() <= 7)
                    )
                    body
                }
                if (tryDecode.isSuccess) {
                    val payRequest = tryDecode.getOrThrow()
                    val username =
                        principal?.payload?.subject ?: throw Error("Jwt principal is null")
                    val balance =
                        withContext(Dispatchers.IO) { BalanceRepository.findByUsername(username) }
                    if(balance == null){
                        call.respond(HttpStatusCode.Unauthorized,"")
                    }else{
                        val walletServicePayRequest = WalletPayRequest(
                            balance.muxedId,
                            payRequest.destination,
                            payRequest.amount.toProjectBigDecimal()
                        )
                        WalletService.sendPaymentRequest(walletServicePayRequest)
                        val response = walletServicePayRequest.receiveResult()
                        call.respond(response.statusCode, "")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "")
                }

            }
        }
    }

    operator fun invoke(routing: Routing) {
        routing.pay()
    }
}