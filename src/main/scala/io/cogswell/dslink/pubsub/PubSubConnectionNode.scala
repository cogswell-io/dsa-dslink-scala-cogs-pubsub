package io.cogswell.dslink.pubsub

import java.net.ConnectException
import java.util.concurrent.TimeUnit

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
import io.cogswell.dslink.pubsub.services.Services
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.StringyException

case class PubSubConnectionNode(
    parentNode: Node,
    name: String,
    metadata: Option[PubSubConnectionMetadata] = None
)(implicit ec: ExecutionContext) extends PubSubNode {
  private val subscribers = MutableMap[String, PubSubSubscriberNode]()
  private val publishers = MutableMap[String, PubSubPublisherNode]()
  
  private var connection: Option[PubSubConnection] = None
  private var connectionNode: Option[Node] = None
  
  private val logger = LoggerFactory.getLogger(getClass)
  private var statusNode: Option[Node] = None

  private def setStatus(status: String): Unit = {
    logger.info(s"Setting status to '$status' for connection '$name'")
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
      conn.subscriptions() map { _.toSet } map { subs =>
        // Combine all channels both the pub/sub service and locally
        val channels = subs ++ subscribers.keys
        
        channels.map { channel =>
          // Identify to which a channel belongs (service, local, or both)
          (channel, subscribers.contains(channel), subs.contains(channel))
        } foreach {
          case (channel, true, false) => {
            // If a channel is local only, re-subscribe to it. 
            subscribers.get(channel).foreach { node =>
              node.subscribe() andThen {
                case Success(_) => logger.info(s"Re-subscribed to channel ${channel}")
                case Failure(error) => logger.error(s"Error re-subscribing to channel ${channel}:", error)
              }
            }
          }
          case (channel, false, true) => {
            // If a channel is on the service only, un-subscribe from it.
            connection.foreach { conn =>
              conn.unsubscribe(channel) andThen {
                case Success(_) => logger.info(s"Successfully un-subsbscribed from channel ${channel}")
                case Failure(error) => logger.error(s"Error un-subsbscribed from channel ${channel}:", error)
              }
            }
          }
          case _ => // Already in sync; no update needed.
        }
      }
    }
  }
  
  private def options: PubSubOptions = {
    PubSubOptions(
      closeListener = Some(closeHandler),
      reconnectListener = Some(reconnectHandler),
      url = metadata.flatMap(_.url)
    )
  }

  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Initializing connection '$name'")
    
    val CHANNEL_PARAM = "channel"
    val MESSAGE_PARAM = "message"
    
    // Connection node
    connectionNode = Option(parentNode getChild name) orElse {
      Some {
        val builder = parentNode createChild name
        
        metadata.foreach { md =>
          builder.setMetaData(md.toJson.toString)
        }
        
        builder build
      }
    }
    
    connectionNode foreach { cNode =>
      // Status indicator node
      statusNode = Option(cNode getChild "Status") orElse { 
        Some(
          cNode.createChild("Status")
          .setValueType(ValueType.STRING)
          .setValue(new Value("Unknown"))
          .build()
        )
      }
      
      // Disconnect action node
      Option(cNode getChild "Disconnect") orElse {
        Some(cNode createChild "Disconnect" build)
      } foreach {
        _.setAction(LinkUtils.action(Seq()) { actionData =>
          logger.info(s"Closing connection '$name'")
          destroy()
        })
      }
      
      // Subscribe action node
      Option(cNode getChild "Add Subscriber") orElse {
        Some(cNode createChild "Add Subscriber" build)
      } foreach {
        _.setAction(LinkUtils.action(Seq(
            ActionParam(CHANNEL_PARAM, ValueType.STRING)
        )) { actionData =>
          val map = actionData.dataMap
          
          map(CHANNEL_PARAM).value.map(_.getString) match {
            case Some(channel) => {
              Await.result(
                addSubscriber(link, cNode, channel) transform (
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
      }
        
      // Publisher action node
      Option(cNode getChild "Add Publisher") orElse {
        Some(cNode createChild "Add Publisher" build)
      } foreach {
        _.setAction(LinkUtils.action(Seq(
            ActionParam(CHANNEL_PARAM, ValueType.STRING)
        )) { actionData =>
          val map = actionData.dataMap

          map(CHANNEL_PARAM).value.map(_.getString) match {
            case Some(channel) => addPublisher(link, cNode, channel)
            case None => {
              val message = "No channel supplied for new publisher."
              logger.warn(message)
              throw new IllegalArgumentException(message)
            }
          }
        })
      }
      
      // Publish action
      Option(cNode getChild "Publish") orElse {
        Some(cNode createChild "Publish" build)
      } foreach {
        _.setAction(LinkUtils.action(Seq(
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
    }
    
    connection.fold(connect()) { _ => Future.successful() }
  }
  
  private def addSubscriber(link: DSLink, parentNode: Node, channel: String): Future[Unit] = {
    logger.info(s"Adding subscriber to channel '$channel'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to subscribe!")
        Future.failed(new ConnectException("Pub/Sub connection does not exist."))
      }
      case Some(conn) => {
        val subscriber = PubSubSubscriberNode(parentNode, conn, channel)
        
        subscriber.subscribe() flatMap { _ =>
          subscriber.linkReady(link)
        } andThen {
          case Success(_) => {
            logger.info(s"[$name] Succesfully added subscriber to channel '$channel'")
            subscribers.put(channel, subscriber)
          }
          case Failure(error) => {
            logger.error(s"[$name] Error adding subscriber to channel '$channel':", error)
            subscriber.destroy()
          }
        } map { _ => Unit }
      }
    }
  }
  
  private def addPublisher(link: DSLink, parentNode: Node, channel: String): Unit = {
    logger.info(s"Adding publisher for channel '$channel'")
    
    connection match {
      case None => {
        logger.warn("No connection found when attempting to setup publisher!")
        throw new ConnectException("Pub/Sub connection does not exist.")
      }
      case Some(conn) => {
        val publisher = PubSubPublisherNode(parentNode, conn, channel)
        publisher.linkReady(link) andThen {
          case Success(_) => {
            logger.info(s"[$name] Succesfully added publisher for channel '$channel'")
            publishers.put(channel, publisher)
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