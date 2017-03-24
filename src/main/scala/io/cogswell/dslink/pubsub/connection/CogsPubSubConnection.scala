package io.cogswell.dslink.pubsub.connection

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.gambit.sdk.pubsub.PubSubHandle
import io.cogswell.dslink.pubsub.util.Futures
import java.util.UUID
import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.subscription.PubSubSubscription
import io.cogswell.dslink.pubsub.subscription.CogsPubSubSubscription

case class CogsPubSubConnection(
    pubSubHandle: PubSubHandle
) extends PubSubConnection {
  override def disconnect()(implicit ec: ExecutionContext): Future[Unit] = {
    Futures.convert(pubSubHandle.close()).map(_ => Unit)
  }

  override def subscribe(
      channel: String,
      messageListener: Option[(PubSubMessage) => Unit]
  )(implicit ec: ExecutionContext): Future[PubSubSubscription] = {
    Futures.convert(pubSubHandle.subscribe(channel, null)) map { _ =>
      CogsPubSubSubscription(this, channel)
    }
  }

  override def unsubscribe(
      channel: String
  )(implicit ec: ExecutionContext): Future[Unit] = {
    Futures.convert(pubSubHandle.unsubscribe(channel)).map(_ => Unit)
  }
  
  override def publish(
      channel: String,
      message: String
  )(implicit ec: ExecutionContext): Future[UUID] = {
    Futures.convert(pubSubHandle.publishWithAck(channel, message))
  }
}