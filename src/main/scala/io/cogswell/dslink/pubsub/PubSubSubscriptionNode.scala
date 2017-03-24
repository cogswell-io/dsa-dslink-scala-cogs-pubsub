package io.cogswell.dslink.pubsub

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager

import io.cogswell.dslink.pubsub.subscription.PubSubSubscription
import io.cogswell.dslink.pubsub.connection.PubSubConnection
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class PubSubSubscriptionNode(
    manager: NodeManager,
    parentNode: Node,
    connection: PubSubConnection,
    channel: String
) {
  private def initUi(): Unit = {
    // TODO: setup UI
  }
  
  initUi()
  
  def subscribe()(implicit ec: ExecutionContext): Future[PubSubSubscription] = {
    connection.subscribe(channel, None)
  }
}