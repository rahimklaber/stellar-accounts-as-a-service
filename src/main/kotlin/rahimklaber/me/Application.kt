package rahimklaber.me

import io.ktor.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.stellar.sdk.*
import org.stellar.sdk.xdr.SignerKey
import rahimklaber.me.models.Balance
import rahimklaber.me.models.ChannelAccount
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
    val walletname = environment.config.property("database.dbname").getString()
    Database.connect(
        url = "jdbc:sqlite:$walletname"
    )
    transaction{
        SchemaUtils.create(User,Balance,ProcessedOperations,ChannelAccount)
    }
    val secretKey = environment.config.property("stellar.secret").getString()
    val channels = transaction {
        val all = ChannelAccount.selectAll()
        if(all.any()){
            all.map { it[ChannelAccount.address] }
        }else{
            val keypair = KeyPair.fromSecretSeed(secretKey)
            val seq = WalletService.server.accounts().account(keypair.accountId).sequenceNumber
            val channelsToCreate = (0..5).map { KeyPair.random() }
            val txBuilder = Transaction.Builder(Account(keypair.accountId,seq), Network.TESTNET)
                    channelsToCreate.forEach {
                        txBuilder.addOperation(CreateAccountOperation.Builder(it.accountId,"5").build())
                            .addOperation(
                                SetOptionsOperation.Builder()
                                    .setSigner(keypair.xdrSignerKey,1)
                                    .setSourceAccount(it.accountId)
                                    .build()
                            )

                    }
            val tx = txBuilder.setBaseFee(120).setTimeout(0).build()
            tx.sign(keypair)
            channelsToCreate.forEach { tx.sign(it) }
            val submitRes = WalletService.server.submitTransaction(tx)
            if(submitRes.isSuccess){
                transaction {
                    channelsToCreate.forEach { channelkeypair ->
                        ChannelAccount.insert {
                            it[address] = channelkeypair.accountId
                        }
                    }
                }
                channelsToCreate.map { it.accountId }
            }else{
                listOf()
            }
        }
    }
    WalletService(secretKey,channels)
}
