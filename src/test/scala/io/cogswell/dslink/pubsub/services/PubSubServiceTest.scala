package io.cogswell.dslink.pubsub.services

import io.cogswell.dslink.pubsub.DslinkTest
import scaldi.Injectable
import io.cogswell.dslink.pubsub.connection.LocalPubSubConnection
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import io.cogswell.dslink.pubsub.model.PubSubOptions

/**
 * This test serves primarily as an example for writing new DSLink tests.
 * 
 * General timeouts are tunable via the requisite parent class: DslinkTest.
 * 
 * This test confirms that injection is working correctly.
 */
class PubSubServiceTest extends DslinkTest(
    timeout = Duration(5, TimeUnit.SECONDS),
    interval = Duration(50, TimeUnit.MILLISECONDS)
) with Injectable {
  private def noKeys: Seq[String] = Seq.empty

  "PubSubService" should "run in test mode within unit tests" in {
    ServicesModule.runMode should be (TestMode)
    
    val pubsub: PubSubService = inject [PubSubService]
    
    pubsub should be (LocalPubSubService)
  }
  
  it should "connect and supply a PubSubConnection" in {
    whenReady(Services.pubSubService.connect(noKeys, None)) { connection =>
      connection should be (LocalPubSubConnection(None))
    }

    whenReady(Services.pubSubService.connect(noKeys, Some(PubSubOptions()))) { connection =>
      connection should be (LocalPubSubConnection(Some(PubSubOptions())))
    }

    whenReady(
        Services.pubSubService.connect(noKeys, None) flatMap { _.disconnect }
    ) { result =>
      result should be (())
    }
  }
}