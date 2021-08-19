package rahimklaber.me.repositories

import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import rahimklaber.me.models.Balance
import rahimklaber.me.models.BalanceModel
import rahimklaber.me.models.User

object BalanceRepository {
    fun findByUsername(username: String) : BalanceModel{
        return transaction{
           val balance =  User.innerJoin(Balance)
                .select { User.name eq username}
                .first()
            BalanceModel(balance[User.name],balance[Balance.id],balance[Balance.balance])
        }
    }
    fun findByMuxedId(muxedId : Long) : BalanceModel{
        return transaction {
            val balance =  User.innerJoin(Balance)
                .select { Balance.id eq muxedId}
                .first()
            BalanceModel(balance[User.name],balance[Balance.id],balance[Balance.balance])
        }
    }
}