package io.cogswell.dslink.pubsub.services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import io.cogswell.dslink.pubsub.connection.TestPubSubConnection
import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.model.PubSubOptions

object TestPubSubService extends PubSubService {
  override def connect(
      keys: Seq[String], options: Option[PubSubOptions] = None
  )(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    Future.successful(TestPubSubConnection(options))
  }
}