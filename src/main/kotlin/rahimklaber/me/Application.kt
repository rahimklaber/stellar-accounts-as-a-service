package rahimklaber.me

import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import rahimklaber.me.models.Balance
import rahimklaber.me.models.ProcessedOperations
import rahimklaber.me.models.User
import rahimklaber.me.plugins.*
import rahimklaber.me.services.WalletService

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSecurity()
    configureRouting()
    configureHTTP()
    configureSerialization()
    configureSockets()
    val walletname = environment.config.property("dbname").getString()
    Database.connect(
        url = "jdbc:sqlite:$walletname"
    )
    transaction{
        SchemaUtils.create(User,Balance,ProcessedOperations)
    }
    WalletService()
}
