package io.cogswell.dslink.pubsub.publisher

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.cogswell.dslink.pubsub.connection.PubSubConnection

case class TestPubSubPublisher(
    connection: PubSubConnection,
    channelName: String
) extends PubSubPublisher {
  override def channel: String = channelName

  override def publish(
      message: String
  )(implicit ec: ExecutionContext): Future[UUID] = {
    connection.publish(channel, message)
  }
}