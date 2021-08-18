package rahimklaber.me.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object User : IntIdTable(){
    var name = varchar("name",255).uniqueIndex()
    var password = text("password")
    var muxedId = long("muxedid") references  Balance.id
}

object Balance : Table(){
    var id = long("muxedid").autoIncrement()
    var balance = float("balance")

    override val primaryKey = PrimaryKey(id)

}

@Serializable
data class UserModel(val username: String, val password: String)

@Serializable
data class BalanceModel(val muxedId: Long, val balance : Float )