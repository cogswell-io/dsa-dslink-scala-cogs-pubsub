package io.cogswell.dslink.pubsub

import java.net.ConnectException
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
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.model.PubSubOptions
import io.cogswell.dslink.pubsub.model.PubSubConnectionMetadata
import io.cogswell.dslink.pubsub.model.NameKey
import io.cogswell.dslink.pubsub.services.Services
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.StringyException
import io.cogswell.dslink.pubsub.model.LinkNodeName
import io.cogswell.dslink.pubsub.model.ConnectionNodeName
import io.cogswell.dslink.pubsub.model.InfoNodeName
import io.cogswell.dslink.pubsub.model.ActionNodeName
import io.cogswell.dslink.pubsub.model.PublisherNodeName
import io.cogswell.dslink.pubsub.model.SubscriberNodeName
import io.cogswell.dslink.pubsub.util.StringUtils

case class PubSubConnectionNode(
    parentNode: Node,
    name: LinkNodeName,
    metadata: Option[PubSubConnectionMetadata] = None
)(implicit ec: ExecutionContext) extends PubSubNode {
  private val subscribers = MutableMap[NameKey, PubSubSubscriberNode]()
  private val publishers = MutableMap[NameKey, PubSubPublisherNode]()
  
  private var connection: Option[PubSubConnection] = None
  private var connectionNode: Option[Node] = None
  
  private val logger = LoggerFactory.getLogger(getClass)
  private var statusNode: Option[Node] = None

  private def setStatus(status: String): Unit = {
    logger.info(s"Setting status to '$status' for connection '${name.alias}'")
    statusNode.foreach(_.setValue(new Value(status)))
  }
  
  private def closeHandler: (Option[Throwable]) => Unit = { cause =>
    setStatus("Disconnected")
  }

  private def reconnectHandler: () => Unit = { () =>
    setStatus("Connected")
    validateSubscriptions()
  }
  
  /**
   * Confirm that the subscriptions in the pub/sub service match those
   * which we have stored in this connection. If there are subscriptions 
   * missing, re-subscribe to those channels. If there are extraneous
   * subscriptions, un-subscribe from those channels.
   */
  private def validateSubscriptions(): Unit = {
    connection.foreach { conn =>
      conn.subscriptions() map {
        _.map(SubscriberNodeName(_))
      } map { _.toSet } map { subs =>
        // Combine all channels both the pub/sub service and locally
        val keys = subs.map(_.key) ++ subscribers.keys
        
        keys.map { key =>
          // Identify to which a channel belongs (service, local, or both)
           (key, subscribers.contains(key), subs.contains(key))
        } foreach {
          case (key, true, false) => {
            // If a channel is local only, re-subscribe to it. 
            subscribers.get(key).foreach { node =>
              node.subscribe() andThen {
                case Success(_) =>
                  logger.info(s"Re-subscribed to channel ${key.name.alias}")
                case Failure(error) =>
                  logger.error(s"Error re-subscribing to channel ${key.name.alias}:", error)
              }
            }
          }
          case (key, false, true) => {
            // If a channel is on the service only, un-subscribe from it.
            key.name match {
              case name: SubscriberNodeName => {
                connection.foreach { conn =>
                  conn.unsubscribe(name.channel) andThen {
                    case Success(_) =>
                      logger.info(s"Successfully un-subsbscribed from channel ${name.channel}")
                    case Failure(error) =>
                      logger.error(s"Error un-subsbscribed from channel ${name.channel}:", error)
                  }
                }
              }
              case _ => logger.warn(s"Node name is of the wrong type: ${key}")
            }
          }
          case _ => // Already in sync; no update needed.
        }
      }
    }
  }
  
  private def restoreSubscribers(link: DSLink): Unit = {
    // Synchronize connection nodes with map of nodes.
    {
      val nodeKeys = parentNode.getChildren.toMap.keySet filter {
        _.startsWith("subscriber:")
      } map { key => 
        val channel = StringUtils trim key through ':'
        SubscriberNodeName(channel).key
      }
      
      (nodeKeys ++ subscribers.keySet) map { key =>
        (key.name, nodeKeys.contains(key), subscribers.containsKey(key))
      } foreach {
        case (name: SubscriberNodeName, false, true) =>
          subscribers.remove(name.key) foreach { _.destroy() }
        case (name: SubscriberNodeName, true, false) =>
          addSubscriber(link, parentNode, name)
        case (name: SubscriberNodeName, true, true) =>
          subscribers(name.key).linkReady(link)
        case t =>
          logger.warn(s"Bad state for subscriber: $t")
      }
    }
  }
  
  private def restorePublishers(): Unit = {
    
  }
  
  private def options: PubSubOptions = {
    PubSubOptions(
      closeListener = Some(closeHandler),
      reconnectListener = Some(reconnectHandler),
      url = metadata.flatMap(_.url)
    )
  }

  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Initializing connection '${name.alias}'")
    
    val CHANNEL_PARAM = "channel"
    val MESSAGE_PARAM = "message"
    
    // Connection node
    connectionNode = Some(LinkUtils.getOrMakeNode(
        parentNode, name,
        Some { builder =>
          metadata.foreach { builder setMetaData _.toJson.toString }
        }
    ))
    
    connectionNode foreach { cNode =>
      // Status indicator node
      statusNode = Some(LinkUtils.getOrMakeNode(
          cNode, InfoNodeName("status", "Status"),
          Some { _
            .setValueType(ValueType.STRING)
            .setValue(new Value("Unknown"))
          }
      ))
      
      // Disconnect action node
      LinkUtils.getOrMakeNode(cNode, ActionNodeName("remove-connection", "Remove Connection"))
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Closing connection '$name'")
        destroy()
      })
      
      // Subscribe action node
      LinkUtils.getOrMakeNode(cNode, ActionNodeName("add-subscriber", "Add Subscriber"))
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        
        map(CHANNEL_PARAM).value.map(_.getString) match {
          case Some(channel) => {
            Await.result(
              addSubscriber(link, cNode, SubscriberNodeName(channel)) transform (
                {v => v}, {e => new StringyException(e)}
              ), 
              Duration(30, TimeUnit.SECONDS)
            )
          }
          case None => {
            val message = "No channel supplied for new subscriber."
            logger.warn(message)
            throw new IllegalArgumentException(message)
          }
        }
      })
        
      // Publisher action node
      LinkUtils.getOrMakeNode(cNode, ActionNodeName("add-publisher", "Add Publisher"))
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        
        map(CHANNEL_PARAM).value.map(_.getString) match {
          case Some(channel) => addPublisher(link, cNode, PublisherNodeName(channel))
          case None => {
            val message = "No channel supplied for new publisher."
            logger.warn(message)
            throw new IllegalArgumentException(message)
          }
        }
      })
      
      // Publish action
      LinkUtils.getOrMakeNode(cNode, ActionNodeName("publish", "Publish"))
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING),
          ActionParam(MESSAGE_PARAM, ValueType.STRING, Some(new Value("")))
      )) { actionData =>
        val map = actionData.dataMap
        val message = map(MESSAGE_PARAM).value.map(_.getString).getOrElse("")

        map(CHANNEL_PARAM).value.map(_.getString) match {
          case Some(channel) => connection.foreach(_.publish(channel, message))
          case _ => {
            val message = "Missing channel and/or message for publish action."
            logger.warn(message)
            throw new IllegalArgumentException(message)
          }
        }
      })
    }
    
    connection.fold(connect()) { _ => Future.successful() }
  }
  
  private def addSubscriber(
      link: DSLink, parentNode: Node, subscriberName: SubscriberNodeName
  ): Future[Unit] = {
    val channel = subscriberName.channel
    logger.info(s"Adding subscriber to channel '${channel}'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to subscribe!")
        Future.failed(new ConnectException("Pub/Sub connection does not exist."))
      }
      case Some(conn) => {
        val subscriber = PubSubSubscriberNode(parentNode, conn, subscriberName)
        
        subscriber.subscribe() flatMap { _ =>
          subscriber.linkReady(link)
        } andThen {
          case Success(_) => {
            logger.info(s"[$name] Succesfully added subscriber to channel '${channel}'")
            subscribers.put(subscriberName.key, subscriber)
          }
          case Failure(error) => {
            logger.error(s"[$name] Error adding subscriber to channel '${channel}':", error)
            subscriber.destroy()
          }
        } map { _ => Unit }
      }
    }
  }
  
  private def addPublisher(
      link: DSLink, parentNode: Node, publisherName: PublisherNodeName
  ): Unit = {
    val channel = publisherName.channel
    logger.info(s"Adding publisher for channel '$channel'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to setup publisher!")
        throw new ConnectException("Pub/Sub connection is offline.")
      }
      case Some(conn) => {
        val publisher = PubSubPublisherNode(parentNode, conn, publisherName)
        publisher.linkReady(link) andThen {
          case Success(_) => {
            logger.info(s"[$name] Succesfully added publisher for channel '$channel'")
            publishers.put(publisherName.key, publisher)
          }
          case Failure(error) => {
            logger.error(s"[$name] Error adding publisher for channel '$channel':", error)
            publisher.destroy()
          }
        }
      }
    }
  }
  
  def destroy(): Unit = {
    subscribers.foreach(_._2.destroy())
    publishers.foreach(_._2.destroy())
    connection.foreach(_.disconnect())
    connectionNode.foreach(parentNode.removeChild(_))
  }
  
  def connect()(implicit ec: ExecutionContext): Future[Unit] = {
    val keys = metadata.fold(Seq.empty[String]) { m =>
      Seq(m.readKey, m.writeKey) filter (_.isDefined) map (_.get)
    }
    
    Services.pubSubService.connect(keys, Some(options)) map { conn =>
      logger.info("Connected to the pub/sub service.")
      
      setStatus("Connected")
      connection = Some(conn)
      ()
    }
  }
}