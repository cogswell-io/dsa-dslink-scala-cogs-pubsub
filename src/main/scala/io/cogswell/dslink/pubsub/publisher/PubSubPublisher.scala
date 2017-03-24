package io.cogswell.dslink.pubsub.publisher

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.UUID

trait PubSubPublisher {
  /**
   * Supplies the channel of this publisher.
   * 
   * @return the channel
   */
  def channel: String

  /**
   * Publishes a message to this publisher's channel.
   * 
   * @param message the message to publish
   * 
   * @return a Future which, if successful, contains the message ID
   */
  def publish(message: String)(implicit ec: ExecutionContext): Future[UUID]
}