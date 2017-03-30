package io.cogswell.dslink.pubsub.connection

import java.util.UUID

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.gambit.sdk.pubsub.PubSubHandle

import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.subscriber.CogsPubSubSubscriber
import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import io.cogswell.dslink.pubsub.util.Futures
import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler
import com.gambit.sdk.pubsub.PubSubMessageRecord
import org.joda.time.DateTime
import java.time.ZoneId
import io.cogswell.dslink.pubsub.util.TimeUtils
import org.slf4j.LoggerFactory
import scala.util.Failure
import scala.util.Success

case class CogsPubSubConnection(
    pubSubHandle: PubSubHandle
) extends PubSubConnection {
  private val logger = LoggerFactory.getLogger(getClass)

  override def disconnect()(implicit ec: ExecutionContext): Future[Unit] = {
    Futures.convert(pubSubHandle.close()).map(_ => Unit)
  }

  override def subscribe(
      channel: String,
      messageListener: Option[(PubSubMessage) => Unit]
  )(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    val messageHandler = new PubSubMessageHandler {
      override def onMessage(msg: PubSubMessageRecord): Unit = {
        messageListener.foreach { listener =>
          val timestamp = TimeUtils.instantToDateTime(msg.getTimestamp)
          val message = PubSubMessage(msg.getId, timestamp, msg.getChannel, msg.getMessage)
          listener(message)
        }
      }
    }

    Futures.convert(pubSubHandle.subscribe(channel, messageHandler)) map { _ =>
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
    Futures.convert(pubSubHandle.publishWithAck(channel, message)) andThen {
      case Failure(error) => logger.error(s"Failed to publish to channel '$channel'", error)
      case Success(messageId) => {
        logger.debug(s"Successfully published message '$messageId' to channel '$channel'")
      }
    }
  }
  
  override def subscriptions()(implicit ec: ExecutionContext): Future[Set[String]] = {
    Futures.convert(pubSubHandle.listSubscriptions).map(_.toList.toSet)
  }
}