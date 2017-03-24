package io.cogswell.dslink.pubsub.subscriber

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.UUID

trait PubSubSubscriber {
  /**
   * Supplies the channel of this subscriber.
   * 
   * @return the channel
   */
  def channel: String

  /**
   * Unsubscribes from the channel.
   * 
   * @return a Future indicating success
   */
  def unsubscribe()(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Publishes a message to this subscriber's channel.
   * 
   * @param message the message to publish
   * 
   * @return a Future which, if successful, contains the message ID
   */
  def publish(message: String)(implicit ec: ExecutionContext): Future[UUID]
}