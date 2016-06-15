package com.rbmhtechnology.eventuate.tools.healthcheck.dropwizard

import akka.actor.PoisonPill
import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import com.rbmhtechnology.eventuate.log.CircuitBreaker
import com.rbmhtechnology.eventuate.log.EventLog
import com.rbmhtechnology.eventuate.log.EventLog.EventLogAvailable
import com.rbmhtechnology.eventuate.log.EventLog.EventLogUnavailable
import com.rbmhtechnology.eventuate.ReplicationEndpoint
import com.rbmhtechnology.eventuate.tools.healthcheck.dropwizard.AvailabilityMonitor.HealthRegistryName
import com.rbmhtechnology.eventuate.tools.healthcheck.dropwizard.AvailabilityMonitor.UnhealthyCause
import com.rbmhtechnology.eventuate.tools.healthcheck.dropwizard.AvailabilityMonitor.monitorHealth

/**
 * Monitors the connections of the [[ReplicationEndpoint]]
 * [[EventLog]]s to its persistence backend. The [[EventLog]] has to use a [[CircuitBreaker]] to
 * enable monitoring.
 *
 * The registry name for an [[EventLog]] is: `persistence-of.<log-id>`.
 * and it is optionally prefixed with `namePrefix.` if that is non-empty.
 */
class PersistenceHealthMonitor(endpoint: ReplicationEndpoint, healthRegistry: HealthCheckRegistry, namePrefix: Option[String] = None) {

  import PersistenceHealthMonitor._

  private implicit val availableHealthOps = new HealthRegistryName[EventLogAvailable] {
    override def healthRegistryName(available: EventLogAvailable): String = healthName(available.id)
  }

  private implicit val unavailableHealthOps = new HealthRegistryName[EventLogUnavailable] with UnhealthyCause[EventLogUnavailable] {
    override def healthRegistryName(unavailable: EventLogUnavailable): String = healthName(unavailable.id)
    override def unhealthyCause(unavailable: EventLogUnavailable): Throwable = unavailable.initialCause
  }

  private val monitorActor =
    monitorHealth[EventLogAvailable, EventLogUnavailable](endpoint.system, healthRegistry, namePrefix)

  /**
   * Stop monitoring persistence health and de-register health checks (asynchronously).
   */
  def stopMonitoring(): Unit = monitorActor ! PoisonPill
}

object PersistenceHealthMonitor {
  /**
   * Returns the registry name (without prefix) under which a [[HealthCheck.Result]]
   * for the connection to an [[EventLog]]'s persistence backend is registered.
   */
  def healthName(logId: String): String = s"persistence-of.$logId"
}
