package io.cogswell.dslink.pubsub

import java.util.concurrent.TimeUnit

import scala.collection.mutable.{ Map => MutableMap }
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Random
import scala.util.Success

import org.dsa.iot.dslink.DSLink
import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.Writable
import org.dsa.iot.dslink.node.value.Value
import org.dsa.iot.dslink.node.value.ValuePair
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.util.handler.Handler
import org.slf4j.LoggerFactory

import com.google.common.base.Throwables

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.util.ActionParam
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.Scheduler
import io.cogswell.dslink.pubsub.services.CogsPubSubService

case class PubSubRootNode(
    link: DSLink
)(implicit ec: ExecutionContext) {
  private lazy val manager: NodeManager = link.getNodeManager
  private lazy val rootNode: Node = manager.getSuperRoot
  
  private val logger = LoggerFactory.getLogger(getClass)
  private val connections = MutableMap[String, PubSubConnectionNode]()
  
  private var numberSink: Option[(Number) => Unit] = None
  private var connectNode: Node = _
  
  private def initNode(): Unit = {
    // Random number generator
    val randNode = rootNode
      .createChild("RandomNumbers")
      .setDisplayName("Random Numbers")
      .setValueType(ValueType.NUMBER)
      .setValue(new Value(Random.nextDouble()))
      .build()
    
    Scheduler.repeat(Duration(500, TimeUnit.MILLISECONDS)) {
      // Only update data if there are subscribers.
      if (link.getSubscriptionManager.hasValueSub(randNode)) {
        randNode.setValue(new Value(Random.nextDouble()))
      }
    }
    
    // Number logger
    val loggerNode = rootNode
      .createChild("NumberLogger")
      .setDisplayName("Number Logger")
      .setValueType(ValueType.NUMBER)
      .setWritable(Writable.WRITE)
      .build()

    // Handle updates to the 
    loggerNode.getListener.setValueHandler(new Handler[ValuePair]{
      override def handle(pair: ValuePair): Unit = {
        Option(pair.getCurrent).map(_.getNumber) match {
          case Some(number) => numberSink.foreach(_(number))
        }
      }
    })
      
    logger.info(s"link = $link")
    logger.info(s"loggerNode = $loggerNode")

    numberSink = Some({ number =>
      logger.info(s"Received number: $number")
    })
    
    // Connect action
    val NAME_PARAM = "name"
    val URL_PARAM = "url"
    val READ_KEY_PARAM = "read"
    val WRITE_KEY_PARAM = "write"
    
    val connectAction = LinkUtils.action(Seq(
        ActionParam(NAME_PARAM, ValueType.STRING),
        ActionParam(URL_PARAM, ValueType.STRING, Some(new Value(CogsPubSubService.defaultUrl))),
        ActionParam(READ_KEY_PARAM, ValueType.STRING, Some(new Value(""))),
        ActionParam(WRITE_KEY_PARAM, ValueType.STRING, Some(new Value("")))
    )) { actionData =>
      val map = actionData.dataMap
      
      val name = map(NAME_PARAM).value.map(_.getString).getOrElse("")
      val url = map(URL_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val readKey = map(READ_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      val writeKey = map(WRITE_KEY_PARAM).value.map(_.getString).filter(!_.isEmpty)
      
      if(connections.keys.toSeq.contains(name)){
        throw new IllegalArgumentException(s"Name $name already in use.")
      }
      
      if(url.isEmpty){
        throw new IllegalArgumentException("Url must be provided.")
      }
      
      if(readKey.isEmpty && writeKey.isEmpty){
        throw new IllegalArgumentException("At least one key must be provided.");
      }
      
      logger.info(s"Clicked Invoke to Create a Connection")
      logger.info(s"'${URL_PARAM}' : ${url}")
      logger.info(s"'${READ_KEY_PARAM}' : ${readKey}")
      logger.info(s"'${WRITE_KEY_PARAM}' : ${writeKey}")
      logger.info(s"'${NAME_PARAM}' : ${name}")
      
      class StringyException(cause: Throwable = null) extends Exception(cause) {
        override def getMessage(): String = {
          Throwables.getStackTraceAsString(cause)
        }
      }
      
      Await.result(
        addConnection(name, readKey, writeKey, url) transform({v => v}, {e => new StringyException(e)}),
        Duration(30, TimeUnit.SECONDS)
      )
      
      logger.info("Connection node should now exist")
    }
    
    connectNode = rootNode
      .createChild("connect")
      .setDisplayName("Connect")
      .setAction(connectAction)
      .build()
  }
  
  initNode()
  
  def destroy(): Unit = {
    connections.foreach(_._2.destroy())
    rootNode.removeChild(connectNode)
  }
  
  private def addConnection(
      name: String,
      readKey: Option[String],
      writeKey: Option[String],
      url: Option[String]
  ): Future[PubSubConnection] = {
    val connection = PubSubConnectionNode(manager, rootNode, name, readKey, writeKey, url)
    connections(name) = connection
    connection.connect() andThen {
      case Success(_) => logger.info("Connected to the Pub/Sub server.")
      case Failure(error) => {
        logger.error("Error connecting to the Pub/Sub service:", error)
        connection.destroy()
      }
    }
  }
}