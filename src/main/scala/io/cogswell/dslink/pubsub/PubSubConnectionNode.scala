package io.cogswell.dslink.pubsub

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.dsa.iot.dslink.node.Node
import org.dsa.iot.dslink.node.NodeManager

import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.services.Services
import io.cogswell.dslink.pubsub.model.PubSubOptions

case class PubSubConnectionNode(
    manager: NodeManager,
    parentNode: Node,
    name: String,
    readKey: Option[String] = None,
    writeKey: Option[String] = None,
    adminKey: Option[String] = None,
    url: Option[String] = None
) {
  private def options: Option[PubSubOptions] = url.map(u => PubSubOptions(url = u))
  private val keys: Seq[String] = Seq(readKey, writeKey, writeKey).filter(_.isDefined).map(_.get)
  private var connection: Option[PubSubConnection] = None

  private def initUi(): Unit = {
    // TODO: setup UI
  }
  
  initUi()
  
  def connect()(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    Services.pubSubService.connect(keys, options) map { conn =>
      connection = Some(conn)
      conn
    }
  }
}