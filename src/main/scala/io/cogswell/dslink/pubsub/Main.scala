package io.cogswell.dslink.pubsub

import org.dsa.iot.dslink.DSLinkHandler
import org.slf4j.LoggerFactory
import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.DSLinkFactory

object Main extends DSLinkHandler {
  private val logger = LoggerFactory.getLogger(getClass)

  override val isResponder = true
  override def isRequester() = true

// General handlers

  // Handle a failed set operation
  override def onSetFail(path: String, value: String): Unit = {
    logger.info(s"Set failed of path ${path} to value ${value}")
  }
  
  // Handle a failed subscription operation
  override def onSubscriptionFail(path: String): Unit = {
    logger.info(s"Subscription failed to path ${path}")
  }
  
  // Handle a failed invocation operation
  override def onInvocationFail(path: String): Unit = {
    logger.info(s"Invocation failed for path ${path}")
  }
  
// Responder handlers
  
  // Handle initialization of the Responder
  override def onResponderInitialized(link: DSLink): Unit = {
    logger.info(s"Responder ${link.getPath} has been initialized")
  }
  
  // Handle connection of the Responder (happens after initialization)
  override def onResponderConnected(link: DSLink): Unit = {
    logger.info(s"Responder ${link.getPath} connected")
  }
  
  // Handle disconnection of the Responder
  override def onResponderDisconnected(link: DSLink): Unit = {
    logger.info(s"Responder ${link.getPath} disconnected")
  }
  
// Requestor handlers
  
  // Handle initialization of the Requestor
  override def onRequestorInitialized(link: DSLink): Unit = {
    logger.info(s"Requestor ${link.getPath} has been initialized")
  }
  
  // Handle connection of the Requestor (happens after initialization)
  override def onRequestorConnected(link: DSLink): Unit = {
    logger.info(s"Requestor ${link.getPath} connected")
  }
  
  // Handle disconnection of the Requestor
  override def onRequestorDisconnected(link: DSLink): Unit = {
    logger.info(s"Requestor ${link.getPath} disconnected")
  }
  
  // Bootstrap the DSLink
  def main(args: Array[String]): Unit = {
    DSLinkFactory.start(args, Main)
  }
}