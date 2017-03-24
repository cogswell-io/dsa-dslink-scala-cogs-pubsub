package io.cogswell.dslink.pubsub.connection

import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.gambit.sdk.pubsub.PubSubHandle

import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.subscriber.CogsPubSubSubscriber
import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import io.cogswell.dslink.pubsub.util.Futures

case class CogsPubSubConnection(
    pubSubHandle: PubSubHandle
) extends PubSubConnection {
  override def disconnect()(implicit ec: ExecutionContext): Future[Unit] = {
    Futures.convert(pubSubHandle.close()).map(_ => Unit)
  }

  override def subscribe(
      channel: String,
      messageListener: Option[(PubSubMessage) => Unit]
  )(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    Futures.convert(pubSubHandle.subscribe(channel, null)) map { _ =>
      CogsPubSubSubscriber(this, channel)
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