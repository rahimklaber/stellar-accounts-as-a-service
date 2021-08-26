package rahimklaber.me

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Some extension functions to convert strings/floats to bigDecimals.
 */




fun String.toProjectBigDecimal(): BigDecimal {
    return BigDecimal(this).setScale(7) // 7 decimals after comma
}

fun Double.toProjectBigDecimal(): BigDecimal{
    return BigDecimal(this).setScale(7)  // 7 decimals after comma
}
