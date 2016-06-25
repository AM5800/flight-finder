package flight_finder

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

data class Price(val value: Double, val currency: String) {
  override fun toString(): String {
    return "${value.format(2)} $currency"
  }
}