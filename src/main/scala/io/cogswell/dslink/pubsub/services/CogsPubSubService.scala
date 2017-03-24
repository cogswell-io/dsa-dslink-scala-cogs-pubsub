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
      CogsPubSubConnection(handle)
    }
  }
}