package io.cogswell.dslink.pubsub.util

import com.google.common.base.Throwables

class StringyException(cause: Throwable = null) extends Exception(cause) {
  override def getMessage(): String = {
    Throwables.getStackTraceAsString(cause)
  }
}