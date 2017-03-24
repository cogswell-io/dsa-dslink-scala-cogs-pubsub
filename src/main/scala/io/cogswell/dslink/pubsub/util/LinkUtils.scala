package io.cogswell.dslink.pubsub.util

import org.dsa.iot.dslink.util.handler.Handler
import org.slf4j.LoggerFactory
import scala.util.Try
import scala.util.Failure
import scala.util.Success

object LinkUtils {
  /**
   * Creates a new Handler wrapped around the supplied action,
   * and logs any un-handled Exceptions thrown by the action.
   * 
   * @param action the action which will be invoked by the Handler
   * 
   * @return the new Handler
   */
  def handler[T](action: (T) => Unit): Handler[T] = new Handler {
    private val logger = LoggerFactory.getLogger(action.getClass)
    
    def handle(value: T): Unit = {
      Try {
        action(value)
      } match {
        case Success(_) =>
        case Failure(error) => {
          logger.error("Error in Handler:", error)
          throw error
        }
      }
    }
  }
}