package io.cogswell.dslink.pubsub

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.DSLinkFactory
import org.dsa.iot.dslink.DSLinkHandler
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValueType
import org.slf4j.LoggerFactory
import scala.util.Random
import java.util.Objects
import java.util.TimerTask
import java.util.Timer
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import io.cogswell.dslink.pubsub.util.Scheduler



object Main extends DSLinkHandler {
  private val logger = LoggerFactory.getLogger(getClass)

  private var rootNode: Node = _

  override val isResponder = true
  override val isRequester = false

// General handlers
  
  // Handle a failed set operation
  override def onSetFail(path: String, value: Value): Unit = {
    logger.info(s"Failed to set path '${path}' to value ${value}")
  }
  
  // Handle a failed subscription operation
  /*
  override def onSubscriptionFail(path: String): Node = {
    logger.info(s"Subscription failed to path '${path}'")
    rootNode
  }
  // */
  
  // Handle a failed invocation operation
  /*
  override def onInvocationFail(path: String): Node = {
    logger.info(s"Invocation failed for path '${path}'")
    rootNode
  }
  // */
  
// Responder handlers
  
  /*
  private def cogsResponderInitialized(link: DSLink): Unit = {
    logger.info(s"Responder ${link.getPath} has been initialized")
    val childName = "CogswellPubsub"
    val childTitle = "Cogswell Pub/Sub"
    
    var value: Option[String] = None
    
    link.getNodeManager.getSuperRoot
      .createChild(childName)
      .setDisplayName(childTitle)
      .setValueType(ValueType.STRING)
      .setValue(new Value("") {
        override def getType(): ValueType = ValueType.STRING
        override def getString(): String = value.getOrElse(null)
      })
  }
  */
  
  // Handle initialization of the Responder
  override def onResponderInitialized(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' has been initialized")
    val VALUE_TYPE = ValueType.NUMBER
    val CHILD_NAME = "RandomNumbers"
    val CHILD_TITLE = "Random Numbers"
    
    rootNode = link.getNodeManager.getSuperRoot
      .createChild(CHILD_NAME)
      .setDisplayName(CHILD_TITLE)
      .setValueType(VALUE_TYPE)
      .setValue(new Value(Random.nextDouble()))
      .build()
    
    Scheduler.repeat(Duration(500, TimeUnit.MILLISECONDS)) {
      rootNode.setValue(new Value(Random.nextDouble()))
    }
  }
  
  // Handle connection of the Responder (happens after initialization)
  override def onResponderConnected(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' connected")
  }
  
  // Handle disconnection of the Responder
  override def onResponderDisconnected(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' disconnected")
  }
  
// Requester handlers
  
  // Handle initialization of the Requester
  override def onRequesterInitialized(link: DSLink): Unit = {
    logger.info(s"Requester for path '${link.getPath}' has been initialized")
  }
  
  // Handle connection of the Requester (happens after initialization)
  override def onRequesterConnected(link: DSLink): Unit = {
    logger.info(s"Requester for path '${link.getPath}' connected")
  }
  
  // Handle disconnection of the Requester
  override def onRequesterDisconnected(link: DSLink): Unit = {
    logger.info(s"Requester for path '${link.getPath}' disconnected")
  }
  
  // Bootstrap the DSLink
  def main(args: Array[String]): Unit = {
    DSLinkFactory.start(args, Main)
  }
}