package io.featurehub.rest

import cd.connect.app.config.ThreadLocalConfigurationSource
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.MultivaluedHashMap
import spock.lang.Specification

import java.time.Instant

class MaintenanceNotificationFilterSpec extends Specification {
  ContainerRequestContext requestCtx
  ContainerResponseContext responseCtx
  MultivaluedHashMap<String, Object> headers

  def setup() {
    requestCtx = Mock(ContainerRequestContext)
    responseCtx = Mock(ContainerResponseContext)
    headers = new MultivaluedHashMap<>()
    responseCtx.headers >> headers
  }

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  private MaintenanceNotificationFilter filterWith(Map<String, String> config) {
    ThreadLocalConfigurationSource.createContext(config)
    def filter = new MaintenanceNotificationFilter()
    filter.@Companion.wireFilterCheck()
    return filter
  }

  def "filter adds no headers when no maintenance config is set"() {
    given:
      def filter = filterWith([:])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when only start is configured"() {
    given:
      def filter = filterWith(["maintenance.start": "2020-01-01T00:00:00Z"])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when only end is configured"() {
    given:
      def filter = filterWith(["maintenance.end": "2099-12-31T23:59:59Z"])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when start timestamp is malformed"() {
    given:
      def filter = filterWith([
        "maintenance.start": "not-a-timestamp",
        "maintenance.end"  : "2099-12-31T23:59:59Z"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when end timestamp is malformed"() {
    given:
      def filter = filterWith([
        "maintenance.start": "2020-01-01T00:00:00Z",
        "maintenance.end"  : "not-a-timestamp"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when current time is before the maintenance window"() {
    given:
      def filter = filterWith([
        "maintenance.start": "2090-01-01T00:00:00Z",
        "maintenance.end"  : "2099-12-31T23:59:59Z"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds no headers when current time is after the maintenance window"() {
    given:
      def filter = filterWith([
        "maintenance.start": "2020-01-01T00:00:00Z",
        "maintenance.end"  : "2020-06-01T00:00:00Z"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.isEmpty()
  }

  def "filter adds maintenance headers when within the window"() {
    given:
      def filter = filterWith([
        "maintenance.start"  : "2020-01-01T00:00:00Z",
        "maintenance.end"    : "2099-12-31T23:59:59Z",
        "maintenance.message": "Planned downtime"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.containsKey("X-Maintenance-Start")
      headers.containsKey("X-Maintenance-End")
      headers.containsKey("X-Maintenance-Message")
  }

  def "X-Maintenance-Message header matches configured message"() {
    given:
      def filter = filterWith([
        "maintenance.start"  : "2020-01-01T00:00:00Z",
        "maintenance.end"    : "2099-12-31T23:59:59Z",
        "maintenance.message": "We'll be back soon"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.getFirst("X-Maintenance-Message") == "We'll be back soon"
  }

  def "X-Maintenance-Start header matches the configured start timestamp"() {
    given:
      def startStr = "2020-01-01T00:00:00Z"
      def filter = filterWith([
        "maintenance.start": startStr,
        "maintenance.end"  : "2099-12-31T23:59:59Z",
        "maintenance.message" : "omuandco"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.getFirst("X-Maintenance-Start") == Instant.parse(startStr).toString()
  }

  def "X-Maintenance-End header matches the configured end timestamp"() {
    given:
      def endStr = "2099-12-31T23:59:59Z"
      def filter = filterWith([
        "maintenance.start": "2020-01-01T00:00:00Z",
        "maintenance.end"  : endStr,
        "maintenance.message" : "omuandco"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      headers.getFirst("X-Maintenance-End") == Instant.parse(endStr).toString()
  }

  def "original response is not otherwise modified (status unchanged)"() {
    given:
      def filter = filterWith([
        "maintenance.start": "2020-01-01T00:00:00Z",
        "maintenance.end"  : "2099-12-31T23:59:59Z"
      ])
    when:
      filter.filter(requestCtx, responseCtx)
    then:
      0 * responseCtx.setStatus(_)
      0 * responseCtx.setEntity(_)
  }
}
