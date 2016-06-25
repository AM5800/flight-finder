package flight_finder.skyscanner

import com.goebl.david.Response
import com.goebl.david.Webb
import flight_finder.Flight
import flight_finder.Price
import flight_finder.Ticket
import flight_finder.TicketsSource
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Minutes
import org.json.JSONObject

class SkyscannerSource : TicketsSource {
  private val apiKey = "aa512617735747648470929858114718"
  private val url = "http://partners.api.skyscanner.net/apiservices/pricing/v1.0"

  override fun findRoundTripTickets(originSky: String,
                                    destinationSky: String,
                                    outboundDate: LocalDate,
                                    inboundDate: LocalDate,
                                    currency: String): Collection<Ticket> {

    println("Looking for $originSky -> $destinationSky ${outboundDate.toString("d MMM")}, ${inboundDate.toString("d MMM")}")
    val session = createSession(originSky, destinationSky, outboundDate, inboundDate, currency)
    return pollSession(session, currency, originSky)
  }

  private fun pollSession(session: String, currency: String, originSky: String): Collection<Ticket> {
    Thread.sleep(1000)
    val url = if (session.contains("&")) "$session&apiKey=$apiKey" else "$session?apiKey=$apiKey"
    val webb = Webb.create()
    try {
      val result = webb.get(url)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .header("Accept", "application/json")
          .ensureSuccess()
          .asJsonObject()
      return parseResult(result!!, currency, originSky)
    } catch(e: Exception) {
      // TODO: how to handle graceful?
      if (e.message?.contains("304 Not Modified") == false) throw e
      return emptyList()
    }
  }

  private fun parseResult(response: Response<JSONObject>,
                          currency: String,
                          originSky: String): Collection<Ticket> {
    val json = response.body
    val result = mutableListOf<Ticket>()

    val airports = getAirports(json)
    val carriers = getCarrierCodes(json)
    val segments = getSegments(airports, carriers, json)
    val legs = getLegs(segments, json)

    result.addAll(processItineraries(legs, json, currency, originSky))

    if (json.get("Status") != "UpdatesComplete") {
      result.addAll(pollSession(response.getHeaderField("Location"), currency, originSky))
    }
    return result
  }

  private fun processItineraries(legs: Map<String, SkyscannerLeg>,
                                 json: JSONObject,
                                 currency: String,
                                 originSky: String): Collection<Ticket> {
    val itineraries = json.getJSONArray("Itineraries")
    val result = mutableListOf<Ticket>()
    itineraries.forEachObject { itinerary ->
      val inbound = legs[itinerary.getString("InboundLegId")] ?: return@forEachObject
      val outbound = legs[itinerary.getString("OutboundLegId")] ?: return@forEachObject

      val price = itinerary.getJSONArray("PricingOptions")
          .map { i, jsonArray -> jsonArray.getJSONObject(i).getDouble("Price") }
          .average()

      result.add(
          Ticket(
              Price(price, currency),
              originSky,
              createFlights(outbound.segments),
              createFlights(inbound.segments),
              inbound.duration,
              outbound.duration))
    }
    return result
  }

  private fun createFlights(segments: Collection<SkyscannerSegment>): List<Flight> {
    return segments
        .map { Flight(it.flight, it.origin, it.destination, it.departure, it.arrival, it.duration) }
        .sortedBy { it.departureTime }
        .toList()
  }

  private fun getLegs(segments: Map<Int, SkyscannerSegment>, json: JSONObject): Map<String, SkyscannerLeg> {
    val legs = json.getJSONArray("Legs")
    val result = mutableMapOf<String, SkyscannerLeg>()
    legs.forEachObject { leg ->
      val id = leg.getString("Id")
      val legSegments = leg
          .getJSONArray("SegmentIds")
          .map { i, arr -> arr.getInt(i) }
          .map { segments[it] }
      val duration = leg.getInt("Duration")
      if (legSegments.isEmpty() || legSegments.any { it == null }) return@forEachObject
      result.put(id, SkyscannerLeg(legSegments.filterNotNull(), Minutes.minutes(duration).toStandardDuration()))
    }

    return result
  }

  private fun getCarrierCodes(json: JSONObject): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    val carriers = json.getJSONArray("Carriers")
    carriers.forEachObject { carrier ->
      val id = carrier.getInt("Id")
      val code = carrier.getString("Code")
      result.put(id, code)
    }
    return result
  }

  private fun getSegments(airports: Map<Int, String>, carrierCodes: Map<Int, String>, json: JSONObject): Map<Int, SkyscannerSegment> {
    val result = mutableMapOf<Int, SkyscannerSegment>()
    val segments = json.getJSONArray("Segments")
    segments.forEachObject { segment ->
      if (segment.getString("JourneyMode") != "Flight") return@forEachObject
      val id = segment.getInt("Id")
      val start = airports[segment.getInt("OriginStation")] ?: return@forEachObject
      val end = airports[segment.getInt("DestinationStation")] ?: return@forEachObject
      val departure = DateTime.parse(segment.getString("DepartureDateTime")) ?: return@forEachObject
      val arrival = DateTime.parse(segment.getString("ArrivalDateTime")) ?: return@forEachObject
      val flightNumber = segment.getString("FlightNumber") ?: return@forEachObject
      val carrier = carrierCodes[segment.getInt("OperatingCarrier")] ?: return@forEachObject
      val duration = Minutes.minutes(segment.getInt("Duration")).toStandardDuration()

      result.put(id, SkyscannerSegment(start, end, departure, arrival, carrier + flightNumber, duration))
    }

    return result
  }

  private fun getAirports(json: JSONObject): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    val places = json.getJSONArray("Places")
    places.forEachObject { place ->
      if (place.getString("Type") == "Airport") {
        val id = place.getInt("Id")
        val airport = place.getString("Code")
        result.put(id, airport)
      }
    }
    return result
  }

  private fun createSession(origin: String, destination: String, departure: LocalDate, arrival: LocalDate, currency: String): String {
    val webb = Webb.create()
    val dateFormat = "yyyy-MM-dd"
    val result = webb.post(url)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json")
        .param("apiKey", apiKey)
        .param("country", "UK")
        .param("currency", currency)
        .param("locale", "en-GB")
        .param("originplace", origin)
        .param("destinationplace", destination)
        .param("outbounddate", departure.toString(dateFormat))
        .param("inbounddate", arrival.toString(dateFormat))
        .param("locationschema", "Sky")
        .ensureSuccess()
        .asJsonObject()

    return result.getHeaderField("Location")
  }
}