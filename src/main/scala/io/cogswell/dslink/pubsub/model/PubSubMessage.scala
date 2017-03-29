package io.cogswell.dslink.pubsub.model

import java.util.UUID
import org.joda.time.DateTime
import com.gambit.sdk.pubsub.PubSubMessageRecord
import io.cogswell.dslink.pubsub.util.TimeUtils

case class PubSubMessage(
  id: UUID,
  timestamp: DateTime,
  channel: String,
  message: String
)

object PubSubMessage {
  /**
   * Converts a PubSubMessageRecord to a PubSubMessage.
   * 
   * @param record the PubSubMessageRecord
   * 
   * @return the new PubSubMessage
   */
  def fromRecord(record: PubSubMessageRecord): PubSubMessage = {
    PubSubMessage(
      record.getId,
      TimeUtils.instantToDateTime(record.getTimestamp),
      record.getChannel,
      record.getMessage
    )
  }
}