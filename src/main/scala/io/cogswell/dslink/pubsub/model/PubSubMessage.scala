package io.cogswell.dslink.pubsub.model

import java.util.UUID
import org.joda.time.DateTime

case class PubSubMessage(
  id: UUID,
  timestamp: DateTime,
  channel: String,
  message: String
)