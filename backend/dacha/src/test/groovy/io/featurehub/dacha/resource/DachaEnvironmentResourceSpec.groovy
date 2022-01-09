package io.featurehub.dacha.resource

import io.featurehub.dacha.InternalCache
import io.featurehub.dacha.model.PublishEnvironment
import jakarta.ws.rs.NotFoundException
import spock.lang.Specification

class DachaEnvironmentResourceSpec extends Specification {
  InternalCache cache
  DachaEnvironmentResource resource

  def setup() {
    cache = Mock(InternalCache)
    resource = new DachaEnvironmentResource(cache)
  }

  def "asking for an environment that doesn't exist leads to NFE"() {
    given: "the cache does not have the environment"
      cache.findEnvironment(_) >> null
    when: "we ask for the environment"
      resource.getEnvironmentStructure(UUID.randomUUID())
    then:
      thrown NotFoundException
  }

  def "asking for an environment that does exist returns the environment"() {
    given: "the cache does have the environment"
      def orgId = UUID.randomUUID()
      def portId = UUID.randomUUID()
      def appId = UUID.randomUUID()

      cache.findEnvironment(_) >> new PublishEnvironment()
          .organizationId(orgId).applicationId(appId).portfolioId(portId)
    when: "we ask for the environment"
      def details = resource.getEnvironmentStructure(UUID.randomUUID())
    then:
      details.organizationId == orgId
      details.portfolioId == portId
      details.applicationId == appId
  }
}
