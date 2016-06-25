package flight_finder

import org.joda.time.DateTime
import org.joda.time.Duration

data class Flight(val flightId: String,
                  val origin: String,
                  val destination: String,
                  val departureTime: DateTime,
                  val arrival: DateTime,
                  val duration: Duration)