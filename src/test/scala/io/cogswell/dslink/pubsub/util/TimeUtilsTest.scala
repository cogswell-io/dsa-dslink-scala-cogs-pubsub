package io.cogswell.dslink.pubsub.util

import io.cogswell.dslink.pubsub.DslinkTest
import scaldi.Injectable
import java.time.Instant
import org.joda.time.DateTime

class TimeUtilsTest extends DslinkTest() with Injectable {
  "TimeUtils" should "correctly convert the epoch to a DateTime" in {
    val epochInstant = Instant.EPOCH
    val convertedDateTime = TimeUtils.instantToDateTime(epochInstant)
    val parsedDateTime = DateTime.parse("1970-01-01T00:00:00Z")
    
    convertedDateTime.getMillis should be (parsedDateTime.getMillis)
  }

  it should "correctly convert the current time to a DateTime within a second of the current time" in {
    val nowInstant = Instant.now()
    val convertedDateTime = TimeUtils.instantToDateTime(nowInstant)
    val nowDateTime = DateTime.now()
    
    val diff = nowDateTime.getMillis - convertedDateTime.getMillis
    diff should be < 10L
    diff should be > -10L
  }
}