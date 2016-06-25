package flight_finder

import org.joda.time.Duration
import org.joda.time.format.PeriodFormatterBuilder

data class Ticket(val price: Price,
                  val originSky: String,
                  val outboundFlights: Collection<Flight>,
                  val inboundFlights: Collection<Flight>,
                  val inboundDuration: Duration,
                  val outboundDuration: Duration) {
  override fun toString(): String {
    val priceString = price.toString()
    val outbound = formatDirection(outboundFlights, outboundDuration)
    val inbound = formatDirection(inboundFlights, inboundDuration)
    return "$priceString: $outbound; $inbound"
  }

  private fun formatDirection(flights: Collection<Flight>, duration: Duration): String {
    val route = formatRoute(flights)
    val departureTime = flights.first().departureTime.toString("d MMM HH:mm")
    val durationFormatter = PeriodFormatterBuilder()
        .printZeroAlways()
        .minimumPrintedDigits(2)
        .appendHours()
        .appendSeparator(":")
        .appendMinutes()
        .toFormatter()
    val timeInFlight = durationFormatter.print(duration.toPeriod())
    return "$route, $departureTime(+$timeInFlight)"
  }

  private fun formatRoute(flights: Collection<Flight>): String {
    return flights.fold(mutableListOf<String>(), { mutableList, flight ->
      if (mutableList.isEmpty()) {
        mutableList.add(flight.origin)
        mutableList.add(flight.destination)
      } else mutableList.add(flight.destination)
      mutableList
    }).joinToString("->")
  }
}



