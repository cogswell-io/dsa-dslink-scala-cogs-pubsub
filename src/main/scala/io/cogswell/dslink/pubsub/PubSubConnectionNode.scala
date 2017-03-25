package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.actions.Parameter
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.Permission
import org.dsa.iot.dslink.util.handler.Handler

import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.services.Services
import io.cogswell.dslink.pubsub.model.PubSubOptions

case class PubSubConnectionNode(
    manager: NodeManager,
    parentNode: Node,
    name: String,
    readKey: Option[String] = None,
    writeKey: Option[String] = None,
    adminKey: Option[String] = None,
    url: Option[String] = None
) {
  private def options: Option[PubSubOptions] = url.map(u => PubSubOptions(url = u))
  private val keys: Seq[String] = Seq(readKey, writeKey, writeKey).filter(_.isDefined).map(_.get)
  private var connection: Option[PubSubConnection] = None
  
  private val logger = LoggerFactory.getLogger(getClass)

  private def initUi(): Unit = {
    def handle(event: ActionResult) = {
      logger.info(s"Clicked Invoke to Create a Do Something")
    }
    
    val action = new Action(Permission.WRITE, new Handler[ActionResult] { def handle(event: ActionResult) {}})
    /*  .addParameter(new Parameter(HOST_PARAMETER, ValueType.STRING, new Value("default_host")))
      .addParameter(new Parameter(READKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(WRITEKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(ADMINKEY_PARAMETER, ValueType.STRING))
      .addParameter(new Parameter(NAME_PARAMETER, ValueType.STRING))*/
    
    val connectNode = parentNode
      .createChild(name)
      .setDisplayName(name)
      .setAction(action)
      .build()

  }

  initUi()
  
  def connect()(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    Services.pubSubService.connect(keys, options) map { conn =>
      connection = Some(conn)
      conn
    }
  }
}