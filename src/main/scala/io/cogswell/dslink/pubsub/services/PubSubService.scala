package io.cogswell.dslink.pubsub.services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.model.PubSubOptions

trait PubSubService {
  /**
   * Connect to the pub/sub provider.
   * 
   * @param keys the auth keys for the pub/sub provider
   * @param the options for the new pub/sub connection
   * 
   * @return a Future which, if successful, contains the connection
   */
  def connect(
      keys: Seq[String],
      options: Option[PubSubOptions] = None
  )(implicit ec: ExecutionContext): Future[PubSubConnection]
}