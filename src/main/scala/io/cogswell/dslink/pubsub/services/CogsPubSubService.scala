package io.cogswell.dslink.pubsub.services

import scala.collection.JavaConverters._

import scala.concurrent.Future
import com.gambit.sdk.pubsub.PubSubSDK
import io.cogswell.dslink.pubsub.util.Futures
import com.gambit.sdk.pubsub.{PubSubOptions => CogsPubSubOptions}
import scala.concurrent.ExecutionContext
import java.time.Duration
import io.cogswell.dslink.pubsub.connection.CogsPubSubConnection
import io.cogswell.dslink.pubsub.connection.PubSubConnection
import io.cogswell.dslink.pubsub.model.PubSubOptions
import com.gambit.sdk.pubsub.handlers.PubSubCloseHandler
import com.gambit.sdk.pubsub.handlers.PubSubReconnectHandler
import com.gambit.sdk.pubsub.PubSubMessageRecord
import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler
import io.cogswell.dslink.pubsub.model.PubSubMessage
import com.gambit.sdk.pubsub.handlers.PubSubErrorHandler
import com.gambit.sdk.pubsub.responses.errors.PubSubErrorResponse
import com.gambit.sdk.pubsub.handlers.PubSubErrorResponseHandler
import com.gambit.sdk.pubsub.handlers.PubSubRawRecordHandler

object CogsPubSubService extends PubSubService {
  private def translateOptions(options: PubSubOptions): CogsPubSubOptions = {
    new CogsPubSubOptions(options.url, true, Duration.ofSeconds(30), null)
  }
  
  override def connect(
      keys: Seq[String],
      options: Option[PubSubOptions]
  )(implicit ec: ExecutionContext): Future[PubSubConnection] = {
    val cogsOptions = translateOptions(options.getOrElse(PubSubOptions.default))

    Futures.convert(
        PubSubSDK.getInstance.connect(keys.toList.asJava, cogsOptions)
    ) map { handle =>
      // Add handlers to the handle
      options.foreach { opts =>
        opts.closeHandler.foreach(handle.onClose(_))
        opts.reconnectHandler.foreach(handle.onReconnect(_))
        opts.messageHandler.foreach(handle.onMessage(_))
        opts.errorHandler.foreach(handle.onError(_))
        opts.errorResponseHandler.foreach(handle.onErrorResponse(_))
        opts.rawRecordHandler.foreach(handle.onRawRecord(_))
      }
      
      CogsPubSubConnection(handle)
    }
  }
}