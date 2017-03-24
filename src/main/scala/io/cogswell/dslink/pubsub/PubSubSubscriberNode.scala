package io.cogswell.dslink.pubsub

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager

import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import io.cogswell.dslink.pubsub.connection.PubSubConnection
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class PubSubSubscriberNode(
    manager: NodeManager,
    parentNode: Node,
    connection: PubSubConnection,
    channel: String
) {
  private def initUi(): Unit = {
    // TODO: setup UI
  }
  
  initUi()
  
  def subscribe()(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    connection.subscribe(channel, None)
  }
}