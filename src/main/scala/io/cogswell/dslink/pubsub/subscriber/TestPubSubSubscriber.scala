package io.cogswell.dslink.pubsub.subscriber

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.cogswell.dslink.pubsub.connection.PubSubConnection

case class TestPubSubSubscriber(
    connection: PubSubConnection,
    channelName: String
) extends PubSubSubscriber {
  override def channel: String = channelName

  override def unsubscribe()(implicit ec: ExecutionContext): Future[Unit] = {
    Future.successful(Unit)
  }

  override def publish(
      message: String
  )(implicit ec: ExecutionContext): Future[UUID] = {
    connection.publish(channel, message)
  }
}