package io.cogswell.dslink.pubsub

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.ActionParam
import org.dsa.iot.dslink.node.value.ValueType
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory
import org.dsa.iot.dslink.node.Writable
import io.cogswell.dslink.pubsub.model.PubSubMessage
import org.dsa.iot.dslink.util.handler.Handler
import org.dsa.iot.dslink.node.value.ValuePair

case class PubSubPublisherNode(
    manager: NodeManager,
    parentNode: Node,
    connection: PubSubConnection,
    channel: String
)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val messageSink: (String) => Unit = { message =>
    connection.publish(channel, message)
  }
  
  private def initNode(): Unit = {
    logger.info(s"Initializing publisher node for '$channel'")
    
    val MESSAGE_PARAM = "message"
    val nodeName = s"publisher:$channel"
    val nodeAlias = s"Publisher [$channel]"
    
    // Connection node
    val publisherNode = parentNode
      .createChild(nodeName)
      .setDisplayName(nodeAlias)
      .setWritable(Writable.WRITE)
      .setValueType(ValueType.STRING)
      .build()
    
    // Handle updates to the 
    publisherNode.getListener.setValueHandler(new Handler[ValuePair]{
      override def handle(pair: ValuePair): Unit = {
        Option(pair.getCurrent).map(_.getString) match {
          case Some(msg) => connection.publish(channel, msg)
        }
      }
    })
    
    // Disconnect action node
    val removeNode = publisherNode.createChild("Remove Publisher")
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Removing subscriber for channel '$channel'")
        parentNode.removeChild(publisherNode)
      })
      .build()
    
    // Publisher action node
    val publishNode = publisherNode.createChild("Publish Message")
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
}