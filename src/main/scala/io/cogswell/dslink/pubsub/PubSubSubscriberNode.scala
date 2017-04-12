package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Writable
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.model.PubSubMessage
import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import org.dsa.iot.dslink.DSLink
import io.cogswell.dslink.pubsub.model.SubscriberNodeName
import io.cogswell.dslink.pubsub.model.ActionNodeName
import io.cogswell.dslink.pubsub.model.InfoNodeName

case class PubSubSubscriberNode(
    parentNode: Node,
    connection: PubSubConnection,
    subscriberName: SubscriberNodeName
)(implicit ec: ExecutionContext) extends PubSubNode {
  private val logger = LoggerFactory.getLogger(getClass)
  private var messageSource: Option[(PubSubMessage) => Unit] = None
  private var subscriberNode: Option[Node] = None
  private lazy val channel = subscriberName.channel
  
  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Initializing subscriber $subscriberName")
    
    val MESSAGE_PARAM = "message"
    
    subscriberNode = Some(LinkUtils.getOrMakeNode(
        parentNode, subscriberName,
        Some { _
          .setWritable(Writable.NEVER)
          .setValueType(ValueType.STRING)
        }
    ))
    
    messageSource = Some({ msg =>
      subscriberNode foreach (_.setValue(new Value(msg.message), true))
    })
    
    subscriberNode foreach { sNode =>
      logger.info(s"Configuring actions for subscriber $subscriberName")
      
      LinkUtils.getOrMakeNode(
          sNode, InfoNodeName("channel-name", "Channel Name"),
          Some { builder =>
            builder.setValueType(ValueType.STRING)
            builder.setValue(new Value(subscriberName.channel))
          }
      )
      
      // Disconnect action node
      LinkUtils.getOrMakeNode(sNode, ActionNodeName("remove-subscriber", "Remove Subscriber"))
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Removing subscriber $subscriberName")
        // TODO: unsubscribe from pub/sub service
        parentNode.removeChild(sNode)
      })
      
      // Subscriber action node
      LinkUtils.getOrMakeNode(sNode, ActionNodeName("publish-message", "Publish Message"))
      .setAction(LinkUtils.action(Seq(
          ActionParam(MESSAGE_PARAM, ValueType.STRING, Some(new Value("")))
      )) { actionData =>
        val map = actionData.dataMap
        val message = map(MESSAGE_PARAM).value.map(_.getString).getOrElse("")
        connection.publish(channel, message)
      })
    }
    
    Future.successful()
  }
  
  def destroy(): Unit = {
    subscriberNode.foreach(parentNode.removeChild(_))
    connection.unsubscribe(channel)
  }

  def subscribe()(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    connection.subscribe(channel, Some({ msg =>
      messageSource.foreach(_(msg))
    }))
  }
}