package flight_finder

import org.joda.time.LocalDate


class RoutePlanner(private val tickets: Collection<Ticket>) {
  val fromMunich = findFrom(tickets, "MUC-sky")
  val fromLondon = findFrom(tickets, "LOND-sky")

  private fun findFrom(tickets: Collection<Ticket>, originSky: String): Collection<Ticket> {
    return tickets.filter { ticket ->
      ticket.originSky == originSky
    }
  }

  fun findAndPrint() {
    for (munich in fromMunich) {
      for (london in fromLondon) {
        if (hasCommonStop(munich, london)) {
          println(munich.toString())
          println(london.toString())
          println()
        }
      }
    }
  }

  private data class FlightIdentity(val flight: String, val date: LocalDate) {
    constructor(flight: Flight) : this(flight.flightId, flight.departureTime.toLocalDate())
  }

  private fun hasCommonStop(left: Ticket, right: Ticket): Boolean {
    val leftOutbounds = left.outboundFlights.map { FlightIdentity(it) }
    val rightOutbounds = right.outboundFlights.map { FlightIdentity(it) }
    val leftInbounds = left.inboundFlights.map { FlightIdentity(it) }
    val rightInbounds = right.inboundFlights.map { FlightIdentity(it) }

    return leftOutbounds.intersect(rightOutbounds).any() && leftInbounds.intersect(rightInbounds).any()
  }
}