package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager
import org.dsa.iot.dslink.node.value.ValueType
import org.dsa.iot.dslink.node.actions.Action
import org.dsa.iot.dslink.node.actions.Parameter
import org.dsa.iot.dslink.node.actions.ActionResult
import org.dsa.iot.dslink.node.Permission
import org.dsa.iot.dslink.util.handler.Handler

import org.slf4j.LoggerFactory

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.services.Services
import io.cogswell.dslink.pubsub.model.PubSubOptions
import io.cogswell.dslink.pubsub.util.LinkUtils
import io.cogswell.dslink.pubsub.util.ActionParam
import scala.collection.mutable.{Map => MutableMap}

case class PubSubConnectionNode(
    manager: NodeManager,
    parentNode: Node,
    name: String,
    readKey: Option[String] = None,
    writeKey: Option[String] = None,
    adminKey: Option[String] = None,
    url: Option[String] = None
)(implicit ec: ExecutionContext) {
  private val subscribers = MutableMap[String, PubSubSubscriberNode]()
  private val publishers = MutableMap[String, PubSubPublisherNode]()
  
  private def options: Option[PubSubOptions] = url.map(u => PubSubOptions(url = u))
  private val keys: Seq[String] = Seq(readKey, writeKey, writeKey).filter(_.isDefined).map(_.get)
  private var connection: Option[PubSubConnection] = None
  
  private val logger = LoggerFactory.getLogger(getClass)

  private def initUi(): Unit = {
    val CHANNEL_PARAM = "channel"
    
    // Connection node
    val connectionNode = parentNode.createChild(name).build()
    
    // Disconnect action node
    val disconnectNode = connectionNode.createChild("Disconnect")
      .setAction(LinkUtils.action(Seq()) { actionData =>
        connection.foreach(_.disconnect())
      })
      .build()
    
    // Subscribe action node
    val subscribeNode = connectionNode.createChild("Subscribe")
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        map(CHANNEL_PARAM).value.map(_.getString) match {
          case None => // TODO: handle missing channel
          case Some(channel) => addSubscriber(connectionNode, channel)
        }
      })
      .build()
      
    // Publisher action node
    val publisherNode = connectionNode.createChild("Publish")
      .setAction(LinkUtils.action(Seq(
          ActionParam(CHANNEL_PARAM, ValueType.STRING)
      )) { actionData =>
        val map = actionData.dataMap
        map(CHANNEL_PARAM).value.map(_.getString) match {
          case None => // TODO: handle missing channel
          case Some(channel) => addPublisher(connectionNode, channel)
        }
      })
      .build()
  }

  initUi()
  
  def addSubscriber(parentNode: Node, channel: String): Unit = {
    connection match {
      case None => // TODO: handle no connection
      case Some(c) => {
        val subscriber = PubSubSubscriberNode(manager, parentNode, c, channel)
        subscribers(channel) = subscriber
      }
    }
  }
  
  def addPublisher(parentNode: Node, channel: String): Unit = {
    connection match {
      case None => // TODO: handle no connection
      case Some(c) => {
        val publisher = PubSubPublisherNode(manager, parentNode, c, channel)
        publishers(channel) = publisher
      }
    }
  }
  
  def connect()(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    Services.pubSubService.connect(keys, options) map { conn =>
      connection = Some(conn)
      conn
    }
  }
}