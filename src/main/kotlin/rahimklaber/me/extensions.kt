package rahimklaber.me

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Some extension functions to convert strings/floats to bigDecimals.
 */



val mathContext = MathContext(7,RoundingMode.FLOOR)

fun String.toProjectBigDecimal(): BigDecimal {
    return BigDecimal(this, mathContext)
}

fun Double.toProjectBigDecimal(): BigDecimal{
    return BigDecimal(this, mathContext)
}
