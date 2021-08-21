package rahimklaber.me.services

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.stellar.sdk.*
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import rahimklaber.me.models.Balance
import rahimklaber.me.models.ProcessedOperations
import rahimklaber.me.repositories.BalanceRepository
import shadow.com.google.common.base.Optional
import shadow.com.google.common.io.BaseEncoding
import shadow.com.google.common.primitives.Bytes
import shadow.com.google.common.primitives.Longs
import shadow.okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException

sealed class PayResult(val statusCode: HttpStatusCode){
    class Ok() : PayResult(HttpStatusCode.NoContent)
    class DestinationNotExists() : PayResult(HttpStatusCode.NotFound)
    class InsufficientBalance() : PayResult(HttpStatusCode.Conflict)
    class MalformedDestination() : PayResult(HttpStatusCode.BadRequest)
}

/**
 * Handles wallet operations. Currently only supports paying xlm to an address.
 */
object WalletService {
    val server = Server("https://horizon-testnet.stellar.org")
    lateinit var keyPair : KeyPair
    private val baseEncoding = BaseEncoding.base32().upperCase().omitPadding()
    private val mutex = Mutex() // use mutex so a user cannot "Double spend"
    lateinit var streamEvent : SSEStream<OperationResponse>

    //start streaming from Horizon.
    operator fun invoke(secret: String){
       keyPair = KeyPair.fromSecretSeed(secret)
            streamEvent =  server.payments().forAccount(keyPair.accountId).stream(object : EventListener<OperationResponse>{
                override fun onEvent(payment : OperationResponse) {
                    //Todo fix when Java sdk has proper support for muxed accounts.
                    try {
                        if (payment is PaymentOperationResponse && payment.asset is AssetTypeNative && payment.to == keyPair.accountId) {
                            val uri = payment.links.self.uri
                            val request = Request.Builder().url(uri.toURL()).get().build()
                            val response = server.httpClient.newCall(request).execute()
                            val json = Json.parseToJsonElement(response.body().string()).jsonObject
                            if (json["to_muxed_id"] != null) {
                                val checkIfProcessed = transaction {
                                    ProcessedOperations.select { ProcessedOperations.id eq payment.id }
                                        .any()
                                }
                                // dont handle if we allready handeled it.
                                if(checkIfProcessed){
                                    return
                                }
                                val muxedId =
                                    json["to_muxed_id"]?.jsonPrimitive?.toString()?.removeSurrounding("\"")?.toLong()!!
                                println("got ${payment.amount} for $muxedId")
                                transaction {
                                    val balance = Balance
                                        .select { Balance.id eq muxedId }
                                        .first()
                                    Balance.update({ Balance.id eq muxedId }) {
                                        it[Balance.balance] =
                                            balance[Balance.balance] + payment.amount.toFloat()
                                    }
                                    ProcessedOperations.insert {
                                        it[id] = payment.id
                                    }
                                }
                            }
                        }
                    }catch (e: Throwable){
                        println(e)
                    }
                }

                override fun onFailure(p0: Optional<Throwable>?, p1: Optional<Int>?) {

                }

            })
    }

    // copied from StrKey.java
    // Todo: fix when java sdk has this available.
    fun calculateChecksum(bytes: ByteArray): ByteArray {
        // This code calculates CRC16-XModem checksum
        // Ported from https://github.com/alexgorbatchev/node-crc

        // This code calculates CRC16-XModem checksum
        // Ported from https://github.com/alexgorbatchev/node-crc
        var crc = 0x0000
        var count: Int = bytes.size
        var i = 0
        var code: Int

        while (count > 0) {
            code = crc ushr 8 and 0xFF
            code = code xor (bytes[i++].toInt() and  0xFF)
            code = code xor (code ushr 4)
            crc = crc shl 8 and 0xFFFF
            crc = crc xor code
            code = code shl 5 and 0xFFFF
            crc = crc xor code
            code = code shl 7 and 0xFFFF
            crc = crc xor code
            count--
        }

        // little-endian

        return byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }
    // copied from StrKey.java
    // Todo: fix when java sdk has this available.
    fun muxedAddressFromId(muxedId: Long) : String{
        val publicKeyBytes = keyPair.xdrPublicKey.ed25519.uint256
        val muxedIdBytes = Longs.toByteArray(muxedId)
        val data = Bytes.concat(publicKeyBytes,muxedIdBytes)
        val muxedVersionByte = (12 shl 3)

        return try {
            val outputStream = ByteArrayOutputStream()
            outputStream.write(muxedVersionByte)
            outputStream.write(data)
            val payload = outputStream.toByteArray()
            val checksum = calculateChecksum(payload)
            outputStream.write(checksum)
            val unencoded = outputStream.toByteArray()

            baseEncoding.encode(unencoded)
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    /**
     * Handle payments for users of the service.
     *
     * A mutex is used to prevent a user from making two requests in quick succession and "Double spending".
     *
     * @param username Payment source username.
     * @param destination Payment destination
     * @param amount Payment amount
     * @return Payment result.
     */
    suspend fun pay(muxedId: Long, destination: String, amount : Float): PayResult = mutex.withLock {
        val source = withContext(Dispatchers.IO){
            BalanceRepository.findByMuxedId(muxedId)
        }

        if(source.balance < amount){
            return PayResult.InsufficientBalance()
        }

        val muxedAddress = muxedAddressFromId(source.muxedId)
        val sequence = withContext(Dispatchers.IO){ server.accounts().account(keyPair.accountId).sequenceNumber}
        val account = Account(muxedAddress,sequence)
        val tx = Transaction.Builder(AccountConverter.enableMuxed(),account, Network.TESTNET)
            .addOperation(
                PaymentOperation.Builder(destination,AssetTypeNative(),amount.toString()).build()
            )
            .setBaseFee(120)
            .setTimeout(0)
            .build()
        try {
            tx.sign(keyPair)
        }catch (e: FormatException){
            // destination address is malformed
            return PayResult.DestinationNotExists()
        }

        val res = withContext(Dispatchers.IO){ server.submitTransaction(tx)}
        if(res.isSuccess){
            transaction {
                Balance.update({Balance.id eq source.muxedId}) { it[Balance.balance] = source.balance - amount }
            }
            return PayResult.Ok()
        }
        return when(res.extras.resultCodes.operationsResultCodes[0]){
            "op_underfunded" -> PayResult.InsufficientBalance()
            "op_no_destination" -> PayResult.DestinationNotExists()
            else -> throw Error("Unknown payment error")
        }
    }

}