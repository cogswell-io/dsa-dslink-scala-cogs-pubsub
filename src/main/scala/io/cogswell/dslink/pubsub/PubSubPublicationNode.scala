package io.cogswell.dslink.pubsub

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Node

class PubSubPublicationNode(
    manager: NodeManager,
    parentNode: Node,
    connection: PubSubConnection,
    channel: String
) {
  private def initUi(): Unit = {
    // TODO: setup UI
  }
  
  initUi()
}