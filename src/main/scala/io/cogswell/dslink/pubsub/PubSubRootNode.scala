package io.cogswell.dslink.pubsub

import scala.collection.mutable.MutableList
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.actions.Parameter
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.Permission
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.util.handler.Handler

import org.slf4j.LoggerFactory

case class PubSubRootNode(
    link: DSLink
) {
  private lazy val manager: NodeManager = link.getNodeManager
  private lazy val rootNode: Node = manager.getNode("/").getNode

  private val connections: MutableList[PubSubConnectionNode] = new MutableList
  private val logger = LoggerFactory.getLogger(getClass)

  private def initUi(): Unit = {
    val HOST_PARAMETER = "host"
    val READKEY_PARAMETER = "read"
    val WRITEKEY_PARAMETER = "write"
    val ADMINKEY_PARAMETER = "admin"
    val NAME_PARAMETER = "name"
    
    val addConnectionHandler = new Handler[ActionResult] { 
      def handle(event: ActionResult) = {
        logger.info(s"Clicked Invoke to Create a Connection")
        logger.info(s"Parameter Values - '${HOST_PARAMETER}': ${event.getParameter(HOST_PARAMETER).getString()}")
        logger.info(s"Parameter Values - '${HOST_PARAMETER}': ${event.getParameter(READKEY_PARAMETER)}")
        
        val blah : Value = event.getParameter(READKEY_PARAMETER)
        
        addConnection(
          Option(event.getParameter(NAME_PARAMETER)).getOrElse(new Value("default_name")).getString(),
          Option(Option(event.getParameter(NAME_PARAMETER)).getOrElse(new Value("read_key")).getString()),
          Option(Option(event.getParameter(NAME_PARAMETER)).getOrElse(new Value("write_key")).getString()),
          Option(Option(event.getParameter(NAME_PARAMETER)).getOrElse(new Value("admin_key")).getString()),
          Option(event.getParameter(NAME_PARAMETER)).getOrElse(new Value("default_url")).getString()
        )
        
        logger.info("Connection node should now exist")
      } 
     }
    
    val action = new Action(Permission.WRITE, addConnectionHandler)
      .addParameter(new Parameter(HOST_PARAMETER, ValueType.STRING, new Value("default_host")))
      .addParameter(new Parameter(READKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(WRITEKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(ADMINKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(NAME_PARAMETER, ValueType.STRING))
    
    rootNode
      .createChild("connection")
      .setDisplayName("Connect")
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