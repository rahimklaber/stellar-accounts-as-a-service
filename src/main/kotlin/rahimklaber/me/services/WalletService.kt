package rahimklaber.me.services

import org.stellar.sdk.*
import shadow.com.google.common.io.BaseEncoding
import shadow.com.google.common.primitives.Bytes
import shadow.com.google.common.primitives.Longs
import java.io.ByteArrayOutputStream
import java.io.CharArrayWriter
import java.io.IOException
import java.util.*

/**
 *
 */
object WalletService {
    val server = Server("https://horizon-testnet.stellar.org")
    val keyPair = KeyPair.fromSecretSeed("SBFVAEGVZX74FVYIOUACXV27MFCA4O4QGKQ533DXULUNVLAYQR442Q2F")
    val baseEncoding = BaseEncoding.base32().upperCase().omitPadding()
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

    fun pay(sourceMuxedId : Long, destination: String, amount : Float){
        val muxedAddress = muxedAddressFromId(sourceMuxedId)
        val account = Account(muxedAddress,0)
        println(sourceMuxedId)
        println(keyPair.accountId)
        println(muxedAddress)
        println(muxedAddress.length)
        val tx = Transaction.Builder(AccountConverter.enableMuxed(),account, Network.TESTNET)
            .addOperation(
                PaymentOperation.Builder(destination,AssetTypeNative(),amount.toString()).build()

            )
    }

}