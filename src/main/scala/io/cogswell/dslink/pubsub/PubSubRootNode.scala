package io.cogswell.dslink.pubsub

import scala.collection.mutable.MutableList
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.util.handler.Handler
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.actions.Parameter
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.Permission
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Node

case class PubSubRootNode(
    link: DSLink
) {
  private lazy val manager: NodeManager = link.getNodeManager
  private lazy val rootNode: Node = manager.getNode("/").getNode

  private val connections: MutableList[PubSubConnectionNode] = new MutableList

  private def initUi(): Unit = {
    // logger.info(s"Responder for path '${link.getPath}' has been initialized")
    val CHILD_NAME = "Connect"
    val CHILD_TITLE = "Connect"
    
    val action = new Action(Permission.WRITE, new Handler[ActionResult] { def handle(event: ActionResult) = {}})
      .addParameter(new Parameter("host", ValueType.STRING))
      .addParameter(new Parameter("read", ValueType.STRING))
      .addParameter(new Parameter("write", ValueType.STRING))
      .addParameter(new Parameter("admin", ValueType.STRING))
      .addParameter(new Parameter("name", ValueType.STRING))
    
    val builder = rootNode
      .createChild(CHILD_NAME)
      .setDisplayName(CHILD_TITLE)
      .setAction(action)
      .build()
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