package io.cogswell.dslink.pubsub.model

import com.gambit.sdk.pubsub.responses.errors.PubSubErrorResponse
import com.gambit.sdk.pubsub.handlers.PubSubRawRecordHandler
import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler
import com.gambit.sdk.pubsub.handlers.PubSubReconnectHandler
import com.gambit.sdk.pubsub.handlers.PubSubErrorHandler
import io.cogswell.dslink.pubsub.util.CogsUtils
import com.gambit.sdk.pubsub.PubSubMessageRecord
import com.gambit.sdk.pubsub.handlers.PubSubCloseHandler
import com.gambit.sdk.pubsub.handlers.PubSubErrorResponseHandler

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
    closeListener: Option[(Option[Throwable]) => Unit] = None,
    reconnectListener: Option[() => Unit] = None,
    errorResponseListener: Option[(PubSubErrorResponse) => Unit] = None,
    rawRecordListener: Option[(String) => Unit] = None,
    url: String = "wss://api.cogswell.io/pubsub"
) {
  /**
   * Supplies a copy the same options with a different URL.
   * 
   * @param altUrl the alternate URL
   * 
   * @return the new options containing the alternate URL
   */
  def withUrl(altUrl: String): PubSubOptions = {
    PubSubOptions(
      messageListener,
      errorListener,
      closeListener,
      reconnectListener,
      errorResponseListener,
      rawRecordListener,
      altUrl
    )
  }

  def messageHandler: Option[PubSubMessageHandler] = messageListener.map(CogsUtils.messageHandler(_))
  def errorHandler: Option[PubSubErrorHandler] = errorListener.map(CogsUtils.errorHandler(_))
  def closeHandler: Option[PubSubCloseHandler] = closeListener.map(CogsUtils.closeHandler(_))
  def reconnectHandler: Option[PubSubReconnectHandler] = reconnectListener.map(CogsUtils.reconnectHandler(_))
  def errorResponseHandler: Option[PubSubErrorResponseHandler] = errorResponseListener.map(CogsUtils.errorResponseHandler(_))
  def rawRecordHandler: Option[PubSubRawRecordHandler] = rawRecordListener.map(CogsUtils.rawRecordHandler(_))
}

object PubSubOptions {
  /**
   * Supplies the default options object.
   * 
   * @return default options
   */
  def default = PubSubOptions()
}