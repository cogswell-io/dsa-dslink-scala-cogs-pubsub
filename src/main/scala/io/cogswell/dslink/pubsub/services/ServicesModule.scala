package io.cogswell.dslink.pubsub.services

import scaldi.Module
import scaldi.Condition
import scala.util.Properties

object ServicesModule extends Module {
  val RUN_MODE_KEY = "io.cogswell.dslink.pubsub.RunMode"
  
  def setTestMode(): Unit = Properties.setProp(RUN_MODE_KEY, TestMode.modeName)
  def setProductionMode(): Unit = Properties.setProp(RUN_MODE_KEY, ProductionMode.modeName)
  
  def runMode: RunMode = {
    RunMode.forName(
      Properties.propOrElse(RUN_MODE_KEY, ProductionMode.modeName)
    ) getOrElse ProductionMode
  }
  
  def inTest(): Condition = Condition(TestMode == runMode)
  def inProd(): Condition = Condition(ProductionMode == runMode)

  // Environment-specific bindings

  bind [PubSubService] when (inTest) to LocalPubSubService
  bind [PubSubService] when (inProd) to CogsPubSubService
}