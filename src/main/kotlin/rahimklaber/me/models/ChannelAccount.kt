package rahimklaber.me.models

import org.jetbrains.exposed.dao.id.IntIdTable
import rahimklaber.me.models.User.references
import rahimklaber.me.models.User.uniqueIndex

object ChannelAccount : IntIdTable(){
    var address = varchar("address",56).uniqueIndex()
}
