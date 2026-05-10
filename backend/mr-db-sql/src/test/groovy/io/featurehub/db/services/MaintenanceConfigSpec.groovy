package io.featurehub.db.services

import io.featurehub.mr.model.SystemConfig
import io.featurehub.systemcfg.MaintenanceConfig
import spock.lang.Specification

import java.time.OffsetDateTime

class MaintenanceConfigSpec extends Specification {

  // ── helpers ──────────────────────────────────────────────────────────────

  private static List<SystemConfig> configs(Map<String, Object> values) {
    values.collect { k, v -> new SystemConfig().key(k).value(v) }
  }

  private static String inFuture(int minutes) {
    OffsetDateTime.now().plusMinutes(minutes).toString()
  }

  private static String inPast(int minutes) {
    OffsetDateTime.now().minusMinutes(minutes).toString()
  }

  // ── active=false ─────────────────────────────────────────────────────────

  def "returns null when active flag is false"() {
    given:
      def cfgs = configs([(MaintenanceConfig.cfg_active): false])
    expect:
      MaintenanceConfig.computeMaintenanceInfo(cfgs) == null
  }

  def "returns null when no configs are present"() {
    expect:
      MaintenanceConfig.computeMaintenanceInfo([]) == null
  }

  // ── active=true, no times ────────────────────────────────────────────────

  def "active=true with no times returns active info"() {
    given:
      def cfgs = configs([(MaintenanceConfig.cfg_active): true])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info != null
      info.active == true
      info.startTime == null
      info.endTime == null
  }

  def "active=true with message and no times returns active info with message"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active) : true,
        (MaintenanceConfig.cfg_message): "DB upgrade in progress"
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info != null
      info.active == true
      info.message == "DB upgrade in progress"
  }

  // ── startTime in future → pre-maintenance warning (active=false) ─────────

  def "active=true with startTime in future returns non-active info (pre-maintenance warning)"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): inFuture(60)
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info != null
      info.active == false
      info.startTime != null
  }

  def "pre-maintenance info includes endTime when set"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)  : true,
        (MaintenanceConfig.cfg_startTime): inFuture(60),
        (MaintenanceConfig.cfg_endTime) : inFuture(120)
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info.active == false
      info.startTime != null
      info.endTime != null
  }

  // ── startTime in past → window active ────────────────────────────────────

  def "active=true with startTime in past returns active info (window started)"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): inPast(30)
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info != null
      info.active == true
  }

  def "active=true with startTime in past and endTime in future returns active info"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): inPast(30),
        (MaintenanceConfig.cfg_endTime)  : inFuture(30)
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info.active == true
      info.startTime != null
      info.endTime != null
  }

  // ── endTime in past → window over ────────────────────────────────────────

  def "returns null when endTime is in the past (maintenance over)"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)  : true,
        (MaintenanceConfig.cfg_endTime) : inPast(10)
      ])
    expect:
      MaintenanceConfig.computeMaintenanceInfo(cfgs) == null
  }

  def "past endTime takes precedence even when startTime is also in the past"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): inPast(120),
        (MaintenanceConfig.cfg_endTime)  : inPast(10)
      ])
    expect:
      MaintenanceConfig.computeMaintenanceInfo(cfgs) == null
  }

  // ── malformed datetime strings ────────────────────────────────────────────

  def "malformed startTime string is treated as absent (no start gate)"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)   : true,
        (MaintenanceConfig.cfg_startTime): "not-a-date"
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      // startTime unparseable → treated as null → maintenance is active immediately
      info != null
      info.active == true
      info.startTime == null
  }

  def "malformed endTime string is treated as absent (no expiry)"() {
    given:
      def cfgs = configs([
        (MaintenanceConfig.cfg_active)  : true,
        (MaintenanceConfig.cfg_endTime) : "not-a-date"
      ])
    when:
      def info = MaintenanceConfig.computeMaintenanceInfo(cfgs)
    then:
      info != null
      info.active == true
      info.endTime == null
  }
}
