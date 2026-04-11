package io.featurehub.mr.rest

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.resources.MaintenanceBannerResource
import io.featurehub.systemcfg.MaintenanceConfig
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import spock.lang.Specification

class MaintenanceBannerResourceSpec extends Specification {
  SystemConfigApi systemConfigApi
  MaintenanceBannerResource resource

  def setup() {
    systemConfigApi = Mock(SystemConfigApi)
    resource = new MaintenanceBannerResource(systemConfigApi)
  }

  def "returns 204 when maintenance is not active"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(false)
      ]
    when:
      resource.getMaintenanceBanner()
    then:
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.NO_CONTENT.statusCode
  }

  def "returns 204 when no maintenance config is stored"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> []
    when:
      resource.getMaintenanceBanner()
    then:
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.NO_CONTENT.statusCode
  }

  def "returns 200 with active flag and message when maintenance is active"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(true),
        new SystemConfig().key(MaintenanceConfig.cfg_message).value("Emergency maintenance in progress")
      ]
    when:
      def result = resource.getMaintenanceBanner()
    then:
      result.active == true
      result.message == "Emergency maintenance in progress"
  }

  def "returns 200 with active flag and no message when maintenance is active but message is absent"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> [
        new SystemConfig().key(MaintenanceConfig.cfg_active).value(true)
      ]
    when:
      def result = resource.getMaintenanceBanner()
    then:
      result.active == true
      result.message == null
  }

  def "returns 204 when maintenance transitions from active to inactive"() {
    given: "maintenance was active"
      systemConfigApi.findConfigs(["maintenance"]) >>> [
        [
          new SystemConfig().key(MaintenanceConfig.cfg_active).value(true),
          new SystemConfig().key(MaintenanceConfig.cfg_message).value("Maintenance window")
        ],
        [
          new SystemConfig().key(MaintenanceConfig.cfg_active).value(false)
        ]
      ]
    when: "first call — maintenance is active"
      def result = resource.getMaintenanceBanner()
    then:
      result.active == true

    when: "second call — maintenance has been disabled"
      resource.getMaintenanceBanner()
    then:
      WebApplicationException ex = thrown()
      ex.response.status == Response.Status.NO_CONTENT.statusCode
  }
}
