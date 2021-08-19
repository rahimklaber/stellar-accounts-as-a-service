package rahimklaber.me.plugins

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import rahimklaber.me.routes.Auth
import rahimklaber.me.routes.Info
import rahimklaber.me.routes.Pay

fun Application.configureRouting() {

    routing {
        Auth(this)
        Info(this)
        Pay(this)
    }
}
