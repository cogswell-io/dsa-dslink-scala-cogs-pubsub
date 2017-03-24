package io.cogswell.dslink.pubsub.services

import scaldi.Injector

trait AppInjector {
  implicit lazy val injector: Injector = ServicesModule
}