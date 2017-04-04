package io.cogswell.dslink.pubsub

import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions._
import scala.collection.mutable.{ Map => MutableMap }
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.model.PubSubConnectionMetadata
import io.cogswell.dslink.pubsub.services.CogsPubSubService
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.StringyException
import io.cogswell.dslink.pubsub.util.StringUtils
import io.cogswell.dslink.pubsub.model.NameKey
import io.cogswell.dslink.pubsub.model.ConnectionNodeName
import io.cogswell.dslink.pubsub.model.LinkNodeName
import io.cogswell.dslink.pubsub.model.ActionNodeName
import io.cogswell.dslink.pubsub.model.NameKey
import io.cogswell.dslink.pubsub.model.ActionNodeName
import io.cogswell.dslink.pubsub.model.ConnectionNodeName

case class PubSubRootNode() extends PubSubNode {
  private val logger = LoggerFactory.getLogger(getClass)
  
  private val connections = MutableMap[NameKey, PubSubConnectionNode]()
  
  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    val manager: NodeManager = link.getNodeManager
    val rootNode = manager.getSuperRoot
    
    def addConnection(
        name: LinkNodeName,
        metadata: Option[PubSubConnectionMetadata]
    ): Future[Unit] = {
      val connection = PubSubConnectionNode(rootNode, name, metadata)
      connections(name.key) = connection
      connection.linkReady(link) andThen {
        case Success(_) => logger.info("Connected to the Pub/Sub service.")
        case Failure(error) => {
          logger.error("Error connecting to the Pub/Sub service:", error)
          connection.destroy()
        }
      }
    }
    
    // Synchronize connection nodes with map of nodes.
    {
      val nodeKeys = Option(rootNode.getChildren)
      .fold(Map.empty[String, Node])(_.toMap)
      .keySet filter {
        _.startsWith("connection:")
      } map { key => 
        val name = StringUtils trim key through ':'
        ConnectionNodeName(name).key
      }
      
      (nodeKeys ++ connections.keySet) map { key =>
        (key, nodeKeys.contains(key), connections.containsKey(key))
      } foreach {
        case (key, false, true) => connections.remove(key) foreach { _.destroy() }
        case (key, true, false) => addConnection(key.name, None)
        case (key, true, true) => connections(key).linkReady(link)
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
      
      val alias = map(NAME_PARAM).value.map(_.getString).getOrElse("")
      val url = map(URL_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val readKey = map(READ_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val writeKey = map(WRITE_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val name = ConnectionNodeName(alias)
      
      logger.info(s"Clicked Invoke to Create a Connection")
      logger.debug(s"'${URL_PARAM}' : ${url}")
      logger.debug(s"'${READ_KEY_PARAM}' : ${readKey}")
      logger.debug(s"'${WRITE_KEY_PARAM}' : ${writeKey}")
      logger.debug(s"'${NAME_PARAM}' : ${alias}")
      
      if (connections.keys.toSeq.contains(name)) {
        throw new IllegalArgumentException(s"Name $alias already in use.")
      }
      
      if (url.isEmpty) {
        throw new IllegalArgumentException("Url must be provided.")
      }
      
      if (readKey.isEmpty && writeKey.isEmpty) {
        throw new IllegalArgumentException("At least one key must be provided.");
      }
      
      Await.result(
        addConnection(
            name, Some(PubSubConnectionMetadata(readKey, writeKey, url))
        ) transform({v => v}, {e => new StringyException(e)}),
        Duration(30, TimeUnit.SECONDS)
      )
      
      logger.info("Connection node should now exist")
    }
    
    LinkUtils.getOrMakeNode(
        rootNode, ActionNodeName("add-connection", "Add Connection")
    ) setAction connectAction
    
    Future.successful()
  }
}