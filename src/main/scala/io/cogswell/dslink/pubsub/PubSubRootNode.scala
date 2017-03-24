package io.cogswell.dslink.pubsub

import scala.collection.mutable.MutableList
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Node

case class PubSubRootNode(
    link: DSLink
) {
  private lazy val manager: NodeManager = link.getNodeManager
  private lazy val rootNode: Node = manager.getNode("/").getNode

  private val connections: MutableList[PubSubConnectionNode] = new MutableList

  private def initUi(): Unit = {
    // TODO: setup UI
  }
  
  initUi()
  
  private def addConnection(
      name: String,
      readKey: Option[String],
      writeKey: Option[String],
      adminKey: Option[String],
      url: String
  ): Unit = {
    val connection = PubSubConnectionNode(manager, rootNode, name, readKey, writeKey, adminKey)
    connections += connection
  }
}