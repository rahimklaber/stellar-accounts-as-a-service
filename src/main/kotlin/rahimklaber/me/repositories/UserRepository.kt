package rahimklaber.me.repositories

import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import rahimklaber.me.models.Balance
import rahimklaber.me.models.User
import rahimklaber.me.models.UserModel

object UserRepository {
    fun findByName(username: String) : UserModel{
        return transaction {
            val res = User.innerJoin(Balance)
                .select { User.name eq username }.first()
            UserModel(res[User.name],res[User.password])
        }
    }
}