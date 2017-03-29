package io.cogswell.dslink.pubsub.connection

import java.util.UUID

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.collection.mutable.{ Set => MutableSet }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.model.PubSubOptions
import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import io.cogswell.dslink.pubsub.subscriber.LocalPubSubSubscriber
import io.cogswell.dslink.pubsub.util.Uuid

case class LocalPubSubConnection(
    options: Option[PubSubOptions]
) extends PubSubConnection {
  type MessageListener = (PubSubMessage) => Unit
  private val subscribers = new HashMap[String, MutableSet[MessageListener]] with MultiMap[String, MessageListener]

  override def disconnect()(implicit ec: ExecutionContext): Future[Unit] = {
    subscribers.clear()
    Future.successful(Unit)
  }

  override def subscribe(
      channel: String, 
      messageListener: Option[MessageListener]
  )(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    messageListener.foreach(subscribers.addBinding(channel, _))

    Future.successful(LocalPubSubSubscriber(this, channel))
  }

  override def unsubscribe(
      channel: String
  )(implicit ec: ExecutionContext): Future[Unit] = {
    subscribers.remove(channel)

    Future.successful(Unit)
  }

  override def publish(
      channel: String, message: String
  )(implicit ec: ExecutionContext): Future[UUID] = {
    val messageRecord = PubSubMessage(Uuid.now(), DateTime.now(), channel, message)

    subscribers.get(channel).foreach(_.foreach(_(messageRecord)))

    Future.successful(messageRecord.id)
  }

  override def subscriptions()(implicit ec: ExecutionContext): Future[Set[String]] = {
    Future.successful(Set(subscribers.keySet.toSeq: _*))
  }
}