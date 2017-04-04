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
import org.dsa.iot.dslink.node.value.Value
import scala.util.Failure
import scala.util.Success
import org.dsa.iot.dslink.DSLink
import scala.concurrent.Future
import io.cogswell.dslink.pubsub.model.PublisherNodeName
import io.cogswell.dslink.pubsub.model.ActionNodeName

case class PubSubPublisherNode(
    parentNode: Node,
    connection: PubSubConnection,
    name: PublisherNodeName
)(implicit ec: ExecutionContext) extends PubSubNode {
  private val logger = LoggerFactory.getLogger(getClass)
  private val messageSink: (String) => Unit = { message =>
    connection.publish(channel, message)
  }
  
  private lazy val channel = name.channel
  private var publisherNode: Option[Node] = None
  
  override def linkReady(link: DSLink)(implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Initializing publisher node for '$channel'")
    
    val MESSAGE_PARAM = "message"
    val nodeName = s"publisher:$channel"
    val nodeAlias = s"Publisher [$channel]"
    
    // Connection node
    publisherNode = Some(LinkUtils.getOrMakeNode(
        parentNode, name,
        Some { _
          .setWritable(Writable.WRITE)
          .setValueType(ValueType.STRING)
        }
    ))
    
    publisherNode foreach { pNode =>
      // Handle updates to the 
      pNode.getListener.setValueHandler(new Handler[ValuePair]{
        override def handle(pair: ValuePair): Unit = {
          Option(pair.getCurrent).map(_.getString) match {
            case Some(msg) => connection.publish(channel, msg)
            case _ =>
          }
        }
      })
      
      // Disconnect action node
      LinkUtils.getOrMakeNode(pNode, ActionNodeName("remove-publisher", "Remove Publisher"))
      .setAction(LinkUtils.action(Seq()) { actionData =>
        logger.info(s"Removing subscriber for channel '$channel'")
        parentNode.removeChild(pNode)
      })
      
      // Publisher action node
      LinkUtils.getOrMakeNode(pNode, ActionNodeName("publish-messsage", "Publish Message"))
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
    publisherNode.foreach(parentNode.removeChild(_))
    connection.unsubscribe(channel)
  }
}