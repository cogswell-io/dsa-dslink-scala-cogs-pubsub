package io.cogswell.dslink.pubsub.model

/**
 * Options for a pub/sub connection.
 * 
 * @param messageListener optional listener for messages received from any channel
 * @param errorListener optional listener for errors
 * @param url overrides the default URL to the pub/sub service
 */
case class PubSubOptions(
    messageListener: Option[(PubSubMessage) => Unit] = None,
    errorListener: Option[(Throwable) => Unit] = None,
    url: String = "wss://api.cogswell.io/pubsub"
)

object PubSubOptions {
  /**
   * Supplies the default options object.
   * 
   * @return default options
   */
  def default = PubSubOptions()
}