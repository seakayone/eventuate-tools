package com.rbmhtechnology.eventuate.tools.test

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.ExtendedActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object AkkaSystems {

  private val akkaSystemCounter = new AtomicInteger(0)

  def akkaRemotingConfig: Config = ConfigFactory.parseString(
    """
      |akka.actor.provider = akka.remote.RemoteActorRefProvider
      |akka.remote.netty.tcp.port = 0
    """.stripMargin
  )

  def akkaTestTimeoutConfig: Config = ConfigFactory.parseString(
    s"akka.test.single-expect-default=${TestTimings.timeout.duration.toMillis}ms"
  )

  def withActorSystem[A](overrideConfig: Config = ConfigFactory.empty())(f: ActorSystem => A): A = {
    import Futures.AwaitHelper
    val config = overrideConfig
      .withFallback(akkaTestTimeoutConfig)
      .withFallback(ConfigFactory.parseResourcesAnySyntax("application.conf"))
      .withFallback(ConfigFactory.load("test-core.conf"))
    val system = ActorSystem(newUniqueSystemName, config)
    try {
      f(system)
    } finally {
      system.terminate().await
    }
  }

  def newUniqueSystemName: String = s"default${akkaSystemCounter.getAndIncrement()}"

  def akkaAddress(system: ActorSystem): Address = system match {
    case sys: ExtendedActorSystem => sys.provider.getDefaultAddress
  }
}
