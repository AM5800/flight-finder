package flight_finder

import org.joda.time.LocalDate


interface TicketsSource {
  fun findRoundTripTickets(originSky: String,
                           destinationSky: String,
                           outboundDate: LocalDate,
                           inboundDate: LocalDate,
                           currency: String): Collection<Ticket>
}