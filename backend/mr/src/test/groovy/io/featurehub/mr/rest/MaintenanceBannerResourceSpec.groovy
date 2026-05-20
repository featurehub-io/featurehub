package io.featurehub.mr.rest

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.resources.MaintenanceBannerResource
import io.featurehub.systemcfg.MaintenanceConfig
import jakarta.ws.rs.WebApplicationException
import spock.lang.Specification

import java.time.OffsetDateTime

class MaintenanceBannerResourceSpec extends Specification {
  SystemConfigApi systemConfigApi
  MaintenanceBannerResource resource

  def setup() {
    systemConfigApi = Mock(SystemConfigApi)
    resource = new MaintenanceBannerResource(systemConfigApi)
  }

  private static List<SystemConfig> configs(Map<String, Object> values) {
    values.collect { k, v -> new SystemConfig().key(k).value(v) }
  }

  def "returns 204 when maintenance is not active"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> configs([(MaintenanceConfig.cfg_active): false])
    when:
      resource.getMaintenanceBanner()
    then:
      def ex = thrown(WebApplicationException)
      ex.response.status == 204
  }

  def "returns 204 when no maintenance configs are present"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> []
    when:
      resource.getMaintenanceBanner()
    then:
      def ex = thrown(WebApplicationException)
      ex.response.status == 204
  }

  def "returns active MaintenanceInfo when maintenance is active with no times"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> configs([
        (MaintenanceConfig.cfg_active) : true,
        (MaintenanceConfig.cfg_message): "Deploying new version"
      ])
    when:
      def info = resource.getMaintenanceBanner()
    then:
      info.active == true
      info.message == "Deploying new version"
  }

  def "returns 204 when endTime is in the past (maintenance window over)"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> configs([
        (MaintenanceConfig.cfg_active) : true,
        (MaintenanceConfig.cfg_endTime): OffsetDateTime.now().minusMinutes(5).toString()
      ])
    when:
      resource.getMaintenanceBanner()
    then:
      def ex = thrown(WebApplicationException)
      ex.response.status == 204
  }

  def "returns non-active MaintenanceInfo when startTime is in the future (pre-maintenance warning)"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): OffsetDateTime.now().plusHours(2).toString(),
        (MaintenanceConfig.cfg_endTime)  : OffsetDateTime.now().plusHours(4).toString()
      ])
    when:
      def info = resource.getMaintenanceBanner()
    then:
      info.active == false
      info.startTime != null
      info.endTime != null
  }

  def "returns active MaintenanceInfo when startTime is in the past and endTime is in the future"() {
    given:
      systemConfigApi.findConfigs(["maintenance"]) >> configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): OffsetDateTime.now().minusMinutes(10).toString(),
        (MaintenanceConfig.cfg_endTime)  : OffsetDateTime.now().plusMinutes(50).toString()
      ])
    when:
      def info = resource.getMaintenanceBanner()
    then:
      info.active == true
      info.startTime != null
      info.endTime != null
  }
}
