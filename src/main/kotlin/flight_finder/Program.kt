package flight_finder

import flight_finder.skyscanner.SkyscannerSource
import org.joda.time.Days
import org.joda.time.Duration
import org.joda.time.Hours
import org.joda.time.LocalDate

fun main(args: Array<String>) {
  val skyscanner = SkyscannerSource()
  val maxPrice = 900.0
  val currency = "EUR"
  val maxLegDuration = Hours.hours(18).toStandardDuration()
  val maxStops = 1
  val tickets = getTickets(
      skyscanner,
      listOf("MUC-sky", "LOND-sky"),
      listOf("TYOA-sky"),
      LocalDate(2016, 11, 5),
      LocalDate(2016, 11, 6),
      LocalDate(2016, 11, 19),
      LocalDate(2016, 11, 20),
      currency)

  RoutePlanner(filter(tickets, maxPrice, maxLegDuration, maxStops)).findAndPrint()
}

fun getTickets(source: TicketsSource,
               originAirports: List<String>,
               destinationAirports: List<String>,
               outboundDateStart: LocalDate,
               outboundDateEnd: LocalDate,
               inboundDateStart: LocalDate,
               inboundDateEnd: LocalDate,
               currency: String): List<Ticket> {
  val result = mutableListOf<Ticket>()
  for (originAirport in originAirports) {
    for (destinationAirport in destinationAirports) {
      for (outboundDate in dates(outboundDateStart, outboundDateEnd)) {
        for (inboundDate in dates(inboundDateStart, inboundDateEnd)) {
          result.addAll(source.findRoundTripTickets(originAirport, destinationAirport, outboundDate, inboundDate, currency))
        }
      }
    }
  }
  return result
}

fun dates(start: LocalDate, end: LocalDate): List<LocalDate> {
  if (end < start) throw Exception("invalid dates")
  var date = start
  val result = mutableListOf<LocalDate>()
  while (true) {
    result.add(date)
    if (date == end) break
    date = date.plus(Days.ONE)
  }

  return result
}

fun filter(tickets: List<Ticket>, maxPrice: Double, maxLegDuration: Duration, maxStops: Int): Collection<Ticket> {
  return tickets.filter {
    it.inboundFlights.size - 1 <= maxStops
        && it.outboundFlights.size - 1 <= maxStops
        && it.inboundDuration <= maxLegDuration
        && it.outboundDuration <= maxLegDuration
        && it.price.value <= maxPrice
  }
}

