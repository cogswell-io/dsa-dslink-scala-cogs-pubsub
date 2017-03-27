package io.cogswell.dslink.pubsub

import java.util.concurrent.TimeUnit

import scala.collection.mutable.{Map => MutableMap}
import scala.collection.mutable.MutableList
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Random

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.Scheduler
import scala.util.Failure
import scala.util.Success

case class PubSubRootNode(
    link: DSLink
)(implicit ec: ExecutionContext) {
  private lazy val manager: NodeManager = link.getNodeManager
  private lazy val rootNode: Node = manager.getSuperRoot

  private val logger = LoggerFactory.getLogger(getClass)
  private val connections = MutableMap[String, PubSubConnectionNode]()

  private def initUi(): Unit = {
    // Random number generator
    val randNode = rootNode
      .createChild("RandomNumbers")
      .setDisplayName("Random Numbers")
      .setValueType(ValueType.NUMBER)
      .setValue(new Value(Random.nextDouble()))
      .build()
    
    Scheduler.repeat(Duration(500, TimeUnit.MILLISECONDS)) {
      randNode.setValue(new Value(Random.nextDouble()))
    }
    
    // Connect action
    val NAME_PARAM = "name"
    val URL_PARAM = "url"
    val READ_KEY_PARAM = "read"
    val WRITE_KEY_PARAM = "write"
    val ADMIN_KEY_PARAM = "admin"
    
    val connectAction = LinkUtils.action(Seq(
        ActionParam(NAME_PARAM, ValueType.STRING),
        ActionParam(URL_PARAM, ValueType.STRING, Some(new Value("wss://api.cogswell.io/pubsub"))),
        ActionParam(READ_KEY_PARAM, ValueType.STRING),
        ActionParam(WRITE_KEY_PARAM, ValueType.STRING),
        ActionParam(ADMIN_KEY_PARAM, ValueType.STRING)
    )) { actionData =>
      val map = actionData.dataMap
      
      val name = map(NAME_PARAM).value.map(_.getString).getOrElse("")
      val url = map(URL_PARAM).value.map(_.getString)
      val readKey = map(READ_KEY_PARAM).value.map(_.getString)
      val writeKey = map(WRITE_KEY_PARAM).value.map(_.getString)
      val adminKey = map(ADMIN_KEY_PARAM).value.map(_.getString)
      
      // TODO: ensure that the name is not empty, nor a duplicate
      // TODO: ensure that at least one key is supplied
      
      logger.info(s"Clicked Invoke to Create a Connection")
      logger.info(s"'${URL_PARAM}' : ${url}")
      logger.info(s"'${READ_KEY_PARAM}' : ${readKey}")
      logger.info(s"'${WRITE_KEY_PARAM}' : ${writeKey}")
      logger.info(s"'${ADMIN_KEY_PARAM}' : ${adminKey}")
      logger.info(s"'${NAME_PARAM}' : ${name}")
      
      addConnection(name, readKey, writeKey, adminKey, url)
      
      logger.info("Connection node should now exist")
    }
    
    rootNode
      .createChild("connection")
      .setDisplayName("Connect")
      .setAction(connectAction)
      .build()
  }
  
  initUi()
  
  private def addConnection(
      name: String,
      readKey: Option[String],
      writeKey: Option[String],
      adminKey: Option[String],
      url: Option[String]
  ): Unit = {
    val connection = PubSubConnectionNode(manager, rootNode, name, readKey, writeKey, adminKey, url)
    connections(name) = connection
    connection.connect() andThen {
      case Success(_) => logger.info("Connected to the Pub/Sub server.")
      case Failure(error) => logger.error("Error connecting to the Pub/Sub server:", error)
    }
    
    // TODO: handle outcome of the connection, indicating failure to the UI
    // TODO: consider making the addition of the connection contingent upon successful connection,
    //       unless the child is the correct way to indicate connection failure.
  }
}