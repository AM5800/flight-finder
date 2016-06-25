package flight_finder.skyscanner

import org.joda.time.Duration

data class SkyscannerLeg(val segments: List<SkyscannerSegment>, val duration: Duration)