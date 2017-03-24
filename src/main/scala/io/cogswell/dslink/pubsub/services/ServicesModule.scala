package io.cogswell.dslink.pubsub.services

import scaldi.Module
import scaldi.Condition
import scala.util.Properties

object ServicesModule extends Module {
  private lazy val mode: String = Properties.envOrElse("DSLINK_RUN_MODE", "production")

  def inTest(): Condition = Condition("dev".equals(mode))
  def inProd(): Condition = !inTest()

  // Environment-specific bindings

  bind [PubSubService] when (inTest) to TestPubSubService
  bind [PubSubService] when (inProd) to CogsPubSubService
}