package rahimklaber.me.models

import org.jetbrains.exposed.sql.Table
import rahimklaber.me.models.Balance.autoIncrement


object ProcessedOperations : Table(){
    var id = long("operation_id")

    override val primaryKey = PrimaryKey(id)
}