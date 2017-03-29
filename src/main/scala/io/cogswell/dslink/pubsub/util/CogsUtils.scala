package io.cogswell.dslink.pubsub.util

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler
import com.gambit.sdk.pubsub.handlers.PubSubReconnectHandler
import com.gambit.sdk.pubsub.handlers.PubSubErrorHandler
import com.gambit.sdk.pubsub.handlers.PubSubRawRecordHandler
import com.gambit.sdk.pubsub.responses.errors.PubSubErrorResponse
import com.gambit.sdk.pubsub.PubSubMessageRecord
import com.gambit.sdk.pubsub.handlers.PubSubCloseHandler
import com.gambit.sdk.pubsub.handlers.PubSubErrorResponseHandler
import io.cogswell.dslink.pubsub.model.PubSubMessage

/**
 * Utilties for working with the Cogswell SDK.
 */
object CogsUtils {
  /**
   * Creates a new PubSubCloseHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubCloseHandler
   */
  def closeHandler(listener: (Option[Throwable]) => Unit): PubSubCloseHandler = {
     new PubSubCloseHandler {
        override def onClose(cause: Throwable): Unit = listener(Option(cause))
     }
  }
  
  /**
   * Creates a new PubSubReconnectHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubReconnectHandler
   */
  def reconnectHandler(listener: () => Unit): PubSubReconnectHandler = {
    new PubSubReconnectHandler {
      override def onReconnect(): Unit = listener()
    }
  }
  
  /**
   * Creates a new PubSubMessageHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubMessageHandler
   */
  def messageHandler(listener: (PubSubMessage) => Unit): PubSubMessageHandler = {
    new PubSubMessageHandler {
      override def onMessage(message: PubSubMessageRecord): Unit = {
        listener(PubSubMessage.fromRecord(message))
      }
    }
  }
  
  /**
   * Creates a new PubSubErrorHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubErrorHandler
   */
  def errorHandler(listener: (Throwable) => Unit): PubSubErrorHandler = {
    new PubSubErrorHandler {
      override def onError(error: Throwable): Unit = listener(error)
    }
  }
  
  /**
   * Creates a new PubSubErrorResponseHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubErrorResponseHandler
   */
  def errorResponseHandler(listener: (PubSubErrorResponse) => Unit): PubSubErrorResponseHandler = {
    new PubSubErrorResponseHandler {
      override def onErrorResponse(response: PubSubErrorResponse): Unit = {
        listener(response)
      }
    }
  }
  
  /**
   * Creates a new PubSubRawRecordHandler wrapping the supplied listener function.
   * 
   * @param the listener function
   * 
   * @return the new PubSubRawRecordHandler
   */
  def rawRecordHandler(listener: (String) => Unit): PubSubRawRecordHandler = {
    new PubSubRawRecordHandler {
      override def onRawRecord(record: String): Unit = listener(record)
    }
  }
}