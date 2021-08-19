package rahimklaber.me.models

import kotlinx.serialization.Serializable

@Serializable
class PayRequestModel(val destination : String, val amount: String)