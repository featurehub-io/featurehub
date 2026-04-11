package io.featurehub.mr.rest

import io.featurehub.db.api.AuthenticationApi
import io.featurehub.db.api.GroupApi
import io.featurehub.db.api.OrganizationApi
import io.featurehub.db.api.PersonApi
import io.featurehub.db.api.PortfolioApi
import io.featurehub.db.api.SetupApi
import io.featurehub.db.api.SystemConfigApi
import io.featurehub.mr.auth.AuthenticationRepository
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.resources.SetupResource
import io.featurehub.mr.utils.PortfolioUtils
import io.featurehub.systemcfg.MaintenanceConfig
import io.featurehub.web.security.oauth.AuthProviderCollection
import spock.lang.Specification

class SetupResourceSpec extends Specification {
  SetupApi setupApi
  AuthenticationApi authenticationApi
  OrganizationApi organizationApi
  PortfolioApi portfolioApi
  GroupApi groupApi
  AuthenticationRepository authRepository
  PersonApi personApi
  PortfolioUtils portfolioUtils
  AuthProviderCollection authProviderCollection
  SystemConfigApi systemConfigApi
  SetupResource resource

  def setup() {
    setupApi = Mock(SetupApi)
    authenticationApi = Mock(AuthenticationApi)
    organizationApi = Mock(OrganizationApi)
    portfolioApi = Mock(PortfolioApi)
    groupApi = Mock(GroupApi)
    authRepository = Mock(AuthenticationRepository)
    personApi = Mock(PersonApi)
    portfolioUtils = Mock(PortfolioUtils)
    authProviderCollection = Mock(AuthProviderCollection)
    systemConfigApi = Mock(SystemConfigApi)

    authProviderCollection.codes >> []
    authProviderCollection.providers >> []
    organizationApi.get() >> new Organization().name("test-org")
    setupApi.initialized() >> true

    resource = new SetupResource(
      setupApi, authenticationApi, organizationApi, portfolioApi, groupApi,
      authRepository, personApi, portfolioUtils, authProviderCollection, systemConfigApi
    )
  }

  def "when maintenance is not active, maintenanceInfo is absent from the setup response"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(false)
      ]
    when:
      def response = resource.isInstalled()
    then:
      response.maintenanceInfo == null
  }

  def "when no maintenance configs are stored, maintenanceInfo is absent"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> []
    when:
      def response = resource.isInstalled()
    then:
      response.maintenanceInfo == null
  }

  def "when maintenance is active with a message, maintenanceInfo is present with the message"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(true),
        new SystemConfig().key(MaintenanceConfig.cfg_message).value("Scheduled maintenance at midnight")
      ]
    when:
      def response = resource.isInstalled()
    then:
      response.maintenanceInfo != null
      response.maintenanceInfo.active == true
      response.maintenanceInfo.message == "Scheduled maintenance at midnight"
  }

  def "when maintenance is active but no message is set, maintenanceInfo is present with a null message"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(true)
      ]
    when:
      def response = resource.isInstalled()
    then:
      response.maintenanceInfo != null
      response.maintenanceInfo.active == true
      response.maintenanceInfo.message == null
  }

  def "the setup response always includes the standard capability info"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> []
    when:
      def response = resource.isInstalled()
    then:
      response.capabilityInfo != null
      response.capabilityInfo.containsKey("system.config")
      response.capabilityInfo.containsKey("webhook.features")
  }
}
