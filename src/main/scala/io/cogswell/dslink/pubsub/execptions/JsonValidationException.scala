package io.cogswell.dslink.pubsub.execptions

import play.api.libs.json.JsError
import play.api.libs.json.Json

/**
 * Indicates a failure validating a JsValue as a defined type.
 * 
 * @param error the JsError indicating what was wrong with the structure
 */
case class JsonValidationException(
  error: JsError
) extends Exception("") {
  override def getMessage(): String = {
    JsError toJson error toString
  }
}