package flight_finder.skyscanner

import org.joda.time.DateTime
import org.joda.time.Duration

class SkyscannerSegment(val origin: String,
                        val destination: String,
                        val departure: DateTime,
                        val arrival: DateTime,
                        val flight: String,
                        val duration: Duration)