package io.cogswell.dslink.pubsub

import java.util.concurrent.TimeUnit

import scala.collection.mutable.{ Map => MutableMap }
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._
import scala.util.Failure
import scala.util.Random
import scala.util.Success

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Writable
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValuePair
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.util.handler.Handler
import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.Scheduler
import io.cogswell.dslink.pubsub.services.CogsPubSubService
import io.cogswell.dslink.pubsub.util.StringyException

case class PubSubRootNode() extends PubSubNode {
  private val logger = LoggerFactory.getLogger(getClass)
  
  private val connections = MutableMap[String, PubSubConnectionNode]()
  
  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    val manager: NodeManager = link.getNodeManager
    val rootNode = manager.getSuperRoot
    
    def addConnection(
        name: String,
        metadata: Option[PubSubConnectionMetadata]
    ): Future[Unit] = {
      val connection = PubSubConnectionNode(rootNode, name, metadata)
      connections(name) = connection
      connection.linkReady(link) andThen {
        case Success(_) => logger.info("Connected to the Pub/Sub service.")
        case Failure(error) => {
          logger.error("Error connecting to the Pub/Sub service:", error)
          connection.destroy()
        }
      }
    }
    
    // Synchronize connection nodes
    {
      val (childNodes, childKeys) = Option(rootNode.getChildren).fold(
        (Map.empty[String, Node], Set.empty[String])
      ) { nodeMap =>
        (nodeMap.toMap, nodeMap.keySet.toSet)
      }
      
      (childKeys ++ connections.keySet) map { name =>
        (name, childNodes.containsKey(name), connections.contains(name))
      } foreach {
        case (name, false, true) => connections.remove(name) foreach { _.destroy() }
        case (name, true, false) => addConnection(name, None)
        case (name, true, true) => connections(name).linkReady(link)
        case _ =>
      }
    }
    
    // Connect action
    val NAME_PARAM = "name"
    val URL_PARAM = "url"
    val READ_KEY_PARAM = "read"
    val WRITE_KEY_PARAM = "write"
    
    val connectAction = LinkUtils.action(Seq(
        ActionParam(NAME_PARAM, ValueType.STRING),
        ActionParam(URL_PARAM, ValueType.STRING, Some(new Value(CogsPubSubService.defaultUrl))),
        ActionParam(READ_KEY_PARAM, ValueType.STRING, Some(new Value(""))),
        ActionParam(WRITE_KEY_PARAM, ValueType.STRING, Some(new Value("")))
    )) { actionData =>
      val map = actionData.dataMap
      
      val name = map(NAME_PARAM).value.map(_.getString).getOrElse("")
      val url = map(URL_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val readKey = map(READ_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val writeKey = map(WRITE_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      
      if (connections.keys.toSeq.contains(name)) {
        throw new IllegalArgumentException(s"Name $name already in use.")
      }
      
      if (url.isEmpty) {
        throw new IllegalArgumentException("Url must be provided.")
      }
      
      if (readKey.isEmpty && writeKey.isEmpty) {
        throw new IllegalArgumentException("At least one key must be provided.");
      }
      
      logger.info(s"Clicked Invoke to Create a Connection")
      logger.info(s"'${URL_PARAM}' : ${url}")
      logger.info(s"'${READ_KEY_PARAM}' : ${readKey}")
      logger.info(s"'${WRITE_KEY_PARAM}' : ${writeKey}")
      logger.info(s"'${NAME_PARAM}' : ${name}")
      
      Await.result(
        addConnection(
            name, Some(PubSubConnectionMetadata(readKey, writeKey, url))
        ) transform({v => v}, {e => new StringyException(e)}),
        Duration(30, TimeUnit.SECONDS)
      )
      
      logger.info("Connection node should now exist")
    }
    
    val CONNECT_NODE_NAME = "connect"
    val CONNECT_NODE_ALIAS = "Connect"
    
    Option(rootNode.getChild(CONNECT_NODE_NAME)) orElse {
      Some(
        rootNode
        .createChild(CONNECT_NODE_NAME)
        .setDisplayName(CONNECT_NODE_ALIAS)
        .build()
      )
    } foreach { _ setAction connectAction }
    
    Future.successful()
  }
}