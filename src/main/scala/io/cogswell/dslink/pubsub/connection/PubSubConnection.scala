package io.cogswell.dslink.pubsub.connection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.UUID
import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.subscription.PubSubSubscription

trait PubSubConnection {
  /**
   * Close this connection.
   * 
   * @return a Future indicating success
   */
  def disconnect()(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Subscribe to a pub/sub channel.
   * 
   * @param channel the channel to which we are subscribing
   * @param messageListener an optional handler for messages received from this channel
   * 
   * @return a Future which, if successful, will contain the subscription
   */
  def subscribe(
      channel: String, 
      messageListener: Option[(PubSubMessage) => Unit] = None
  )(implicit ec: ExecutionContext): Future[PubSubSubscription]

  /**
   * Unsubscribe from a pub/sub channel.
   * 
   * @param channel the channel from which to unsubscribe
   * 
   * @param a Future indicating success
   */
  def unsubscribe(channel: String)(implicit ec: ExecutionContext): Future[Unit]

  /**
   * Publish a message to a pub/sub channel.
   * 
   * @param channel the channel to which the message should be published
   * @param message the message to publish
   * 
   * @return a Future which, if successful, will contain the message ID
   */
  def publish(
      channel: String, message: String
  )(implicit ec: ExecutionContext): Future[UUID]
}