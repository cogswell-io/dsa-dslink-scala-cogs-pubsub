package io.cogswell.dslink.pubsub.model

import play.api.libs.json._
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import io.cogswell.dslink.pubsub.execptions.JsonValidationException
import io.cogswell.dslink.pubsub.execptions.JsonParseException
import io.cogswell.dslink.pubsub.util.JsonUtils

/**
 * Metadata stashed with a pub/sub connection node.
 * 
 * @param readKey optional read (subscribe) key for pub/sub
 * @param writeKey optional write (publish) key for pub/sub
 * @param url optional URL, which overrides the default URL
 */
case class PubSubConnectionMetadata(
    readKey: Option[String],
    writeKey: Option[String],
    url: Option[String]
) {
  def toJson: JsValue = PubSubConnectionMetadata.toJson(this)
}

object PubSubConnectionMetadata {
  implicit val writer = Json.writes[PubSubConnectionMetadata]
  implicit val reader = Json.reads[PubSubConnectionMetadata]
  
  def toJson(metadata: PubSubConnectionMetadata): JsValue = Json toJson metadata
  
  def fromJson(json: JsValue): Try[PubSubConnectionMetadata] = {
    Json.fromJson[PubSubConnectionMetadata](json) match {
      case JsSuccess(metadata, _) => Success(metadata)
      case e: JsError => Failure(new JsonValidationException(e))
    }
  }
  
  def parse(jsonText: String): Try[PubSubConnectionMetadata] = {
    JsonUtils.parse(jsonText) flatMap (fromJson(_))
  }
}