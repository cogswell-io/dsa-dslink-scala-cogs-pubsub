package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext.Implicits.global

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.DSLinkFactory
import org.dsa.iot.dslink.DSLinkHandler
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.value.Value
import org.slf4j.LoggerFactory

object Main extends DSLinkHandler {
  private val logger = LoggerFactory.getLogger(getClass)

  private var pubSubNode = PubSubRootNode()

  override val isResponder = true

// General handlers
  
  // Handle a failed set operation
  override def onSetFail(path: String, value: Value): Unit = {
    logger.warn(s"Failed to set path '${path}' to value ${value}")
  }
  
  // Handle a failed subscription operation
  override def onSubscriptionFail(path: String): Node = {
    logger.warn(s"Subscription failed to path '${path}'")
    null
  }
  
  // Handle a failed invocation operation
  override def onInvocationFail(path: String): Node = {
    logger.warn(s"Invocation failed for path '${path}'")
    null
  }
  
// Responder handlers
  
  /**
   * Responder nodes:
   * 
   * - cogswell-pubsub
   *   - [A] Add Connection (action:add-connection)
   *   - [S] Connection[<connection-name>] connection:<connection-name>
   *     - {Metadata}
   *       - keys (at least one must be defined, and identities must match)
   *       - URL (optional)
   *     - [A] Disconnect (action:disconnect)
   *     - [A] Add Subscriber (action:add-subscriber) | Only with R- key
   *     - [A] Add Publisher (action:add-publisher) | Only with W- key
   *     - [A] Publish (action:publish) | Only with W- key
   *     - [I] Status (info:status)
   *     - [S] Subscriber[<channel-name>] (subscriber:<channel-name>)
   *       - [A] Unsubscribe (action:remove-subscriber)
   *       - [A] Publish (action:publish) | Only with W- key
   *     - Publisher[<channel-name>] (publisher:<channel-name>)
   *       - [A] Remove Publisher (action:remove-publisher)
   *       - [A] Publish (action:publish) | Only with W- key
   * 
   */
  
  // Handle connection of the Responder (happens after initialization)
  override def onResponderConnected(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' connected")
    pubSubNode.linkConnected(link)
  }
  
  // Handle initialization of the Responder. This is called after onResponderConnected()!!
  override def onResponderInitialized(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' has been initialized")
    pubSubNode.linkReady(link)
  }
  
  // Handle disconnection of the Responder
  override def onResponderDisconnected(link: DSLink): Unit = {
    logger.info(s"Responder for path '${link.getPath}' disconnected")
    pubSubNode.linkDisconnected(link)
  }
  
  // Bootstrap the DSLink
  def main(args: Array[String]): Unit = {
    DSLinkFactory.start(args, Main)
  }
}