package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.subscriber.PubSubSubscriber
import org.slf4j.LoggerFactory
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.ActionParam
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.Writable
import io.cogswell.dslink.pubsub.model.PubSubMessage
import org.dsa.iot.dslink.node.value.Value

case class PubSubSubscriberNode(
    manager: NodeManager,
    parentNode: Node,
    connection: PubSubConnection,
    channel: String
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  private var messageSource: Option[(PubSubMessage) => Unit] = None
  
  private def initNode(): Unit = {
    logger.info(s"Initializing subscriber node for '$channel'")
    
    val MESSAGE_PARAM = "channel"
    val nodeName = s"subscriber:$channel"
    val nodeAlias = s"Subscriber [$channel]"
    
    // Connection node
    val subscriberNode = parentNode
      .createChild(nodeName)
      .setDisplayName(nodeAlias)
      .setWritable(Writable.NEVER)
      .setValueType(ValueType.STRING)
      .build()
      
    messageSource = Some({ msg =>
      subscriberNode.setValue(new Value(msg.message), true)
    })
    
    // Disconnect action node
    val removeNode = subscriberNode.createChild("Remove")
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Removing subscriber for channel '$channel'")
        // TODO: unsubscribe from pub/sub service
        parentNode.removeChild(subscriberNode)
      })
      .build()
    
    // Subscriber action node
    val publishNode = subscriberNode.createChild("Publish")
      .setAction(LinkUtils.action(Seq(
          ActionParam(MESSAGE_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        
        map(MESSAGE_PARAM).value.map(_.getString) match {
          case None => {
            logger.warn("Cannot publish because no message was supplied!")
            // TODO: handle missing message
          }
          case Some(message) => connection.publish(channel, message)
        }
      })
      .build()
  }
  
  initNode()
  
  def subscribe()(implicit ec: ExecutionContext): Future[PubSubSubscriber] = {
    connection.subscribe(channel, Some({ msg =>
      messageSource.foreach(_(msg))
    }))
  }
}