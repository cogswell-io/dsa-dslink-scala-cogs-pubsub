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
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.ActionParam
import scala.collection.mutable.{Map => MutableMap}
import scala.util.Failure
import scala.util.Success

case class PubSubConnectionNode(
    manager: NodeManager,
    parentNode: Node,
    name: String,
    readKey: Option[String] = None,
    writeKey: Option[String] = None,
    url: Option[String] = None
)(implicit ec: ExecutionContext) {
  private val subscribers = MutableMap[String, PubSubSubscriberNode]()
  private val publishers = MutableMap[String, PubSubPublisherNode]()
  
  private def options: Option[PubSubOptions] = url.map(u => PubSubOptions(url = u))
  private val keys: Seq[String] = Seq(readKey, writeKey).filter(_.isDefined).map(_.get)
  private var connection: Option[PubSubConnection] = None
  
  private val logger = LoggerFactory.getLogger(getClass)

  private def initNode(): Unit = {
    logger.info(s"Initializing connection '$name'")
    
    val CHANNEL_PARAM = "channel"
    val MESSAGE_PARAM = "message"
    
    // Connection node
    val connectionNode = parentNode.createChild(name).build()
    
    // Disconnect action node
    val disconnectNode = connectionNode.createChild("Disconnect")
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Closing connection '$name'")
        parentNode.removeChild(connectionNode)
        connection.foreach(_.disconnect())
      })
      .build()
    
    // Subscribe action node
    val subscribeNode = connectionNode.createChild("Add Subscriber")
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        
        map(CHANNEL_PARAM).value.map(_.getString) match {
          case Some(channel) => addSubscriber(connectionNode, channel)
          case None => {
            logger.warn(s"No channel supplied for new subscriber.")
            // TODO: handle missing channel
          }
        }
      })
      .build()
      
    // Publisher action node
    val publisherNode = connectionNode.createChild("Add Publisher")
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap

        map(CHANNEL_PARAM).value.map(_.getString) match {
          case Some(channel) => addPublisher(connectionNode, channel)
          case None => {
            logger.warn(s"No channel supplied for new publisher.")
            // TODO: handle missing channel
          }
        }
      })
      .build()

    // Publish action
    val publishNode = connectionNode.createChild("Publish")
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING),
          ActionParam(MESSAGE_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        val channel = map(CHANNEL_PARAM).value.map(_.getString)
        val message = map(MESSAGE_PARAM).value.map(_.getString)

        (channel, message) match {
          case (Some(channel), Some(message)) => {
            connection.foreach(_.publish(channel, message))
          }
          case _ => logger.warn(s"Missing channel and/or message for publish action.")
        }
      })
      .build()
  }
  
  initNode()
  
  private def addSubscriber(parentNode: Node, channel: String): Unit = {
    logger.info(s"Adding subscriber to channel '$channel'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to subscribe!")
        // TODO: handle no connection
      }
      case Some(conn) => {
        val subscriber = PubSubSubscriberNode(manager, parentNode, conn, channel)
        subscribers(channel) = subscriber
        
        subscriber.subscribe() andThen {
          case Success(_) => logger.info(s"[$name] Succesfully subscribed to channel '$channel'")
          case Failure(error) => logger.error(s"[$name] Error subscribing to channel '$channel':", error)
        }
      }
    }
  }
  
  private def addPublisher(parentNode: Node, channel: String): Unit = {
    logger.info(s"Adding publisher for channel '$channel'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to setup publisher!")
        // TODO: handle no connection
      }
      case Some(conn) => {
        val publisher = PubSubPublisherNode(manager, parentNode, conn, channel)
        publishers(channel) = publisher
      }
    }
  }
  
  def connect()(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    // TODO: add close and reconnect handlers to options, and update status based on these
    
    Services.pubSubService.connect(keys, options) andThen {
      case Failure(error) => {
        logger.error("Error connecting to the pub/sub service:", error)
        // TODO: handle connection failure: remove node, alert user
      }
    } map { conn =>
      logger.info("Connected to the pub/sub service.")
      connection = Some(conn)
      conn
    }
  }
}