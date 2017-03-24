package io.cogswell.dslink.pubsub.services

import scaldi.Injectable
import scaldi.Injector

object Services {
  private lazy val injector = new AppInjector{}.injector
  private lazy val serviceProvider: ServiceProvider = new ServiceProvider()(injector)
  
  private var customInjector: Option[Injector] = None
  private var customServiceProvider: Option[ServiceProvider] = None
  
  def useCustomInjector(injector: Injector): Unit = {
    customInjector = Some(injector)
    customServiceProvider = Some(new ServiceProvider()(injector))
  }
  
  def useDefaultInjector(): Unit = {
    customInjector = None
    customServiceProvider = None
  }
  
  def pubSubService = serviceProvider.pubSubService
}

private class ServiceProvider(implicit inj: Injector) extends Injectable {
  val pubSubService = inject [PubSubService]
}