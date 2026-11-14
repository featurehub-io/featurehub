package io.featurehub.db.services

import io.featurehub.db.api.SystemConfigApi
import io.featurehub.db.model.DbSystemConfig
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.systemcfg.KnownSystemConfigSource
import io.featurehub.systemcfg.ReadOnlySystemConfig
import io.featurehub.systemcfg.ValidSystemConfig
import org.glassfish.hk2.api.IterableProvider

class SystemConfigSqlApiSpec extends Base2Spec {
  static final String TOKEN = "test.secretToken"
  static final String CHANNEL = "test.channel"

  SystemConfigSqlApi api
  KnownSystemConfigSource source

  def setup() {
    // Base2Spec mocks the encryption service; make it behave like a simple reversible encryption
    encryptionService.encryptSingle(_ as String) >> { String s -> "salt\t" + s }

    source = Stub(KnownSystemConfigSource) {
      getName() >> "test"
      getKnownConfig() >> [
        new ValidSystemConfig(TOKEN, "a secret", true, KnownSystemConfigSource.@Companion.stringRef, true, null, false),
        new ValidSystemConfig(CHANNEL, "a channel", false, KnownSystemConfigSource.@Companion.stringRef, true, null, false),
      ]
      getReadOnlyConfg() >> ([] as List<ReadOnlySystemConfig>)
      presaveUpdateCheck(_, _) >> null
    }

    IterableProvider<KnownSystemConfigSource> provider = Mock()
    provider.iterator() >> { [source].iterator() }

    api = new SystemConfigSqlApi(convertUtils, provider, encryptionService)

    db.find(DbSystemConfig).where().in("id.key", [TOKEN, CHANNEL]).delete()
  }

  SystemConfig stored(String key) {
    return api.findConfigs([key]).find { it.key == key }
  }

  String rawStoredValue(String key) {
    return db.find(DbSystemConfig).where().eq("id.key", key).findOne()?.value
  }

  SystemConfig saveToken(String value) {
    def current = stored(TOKEN)
    return api.updateConfigs([new UpdatedSystemConfig().key(TOKEN).version(current.version).value(value)],
      superuser, false).first()
  }

  def "a stored secret is returned only as the encrypted placeholder"() {
    when:
      saveToken("xoxb-123")
    then:
      stored(TOKEN).value == WebhookEncryptionService.ENCRYPTEDTEXT
      rawStoredValue(TOKEN).contains("xoxb-123")
  }

  def "sending null clears a stored secret"() {
    given:
      saveToken("xoxb-123")
    when:
      saveToken(null)
    then:
      stored(TOKEN).value == null
      rawStoredValue(TOKEN) == null
  }

  def "sending a blank value clears a stored secret rather than encrypting it"() {
    given:
      saveToken("xoxb-123")
    when:
      saveToken(blank)
    then:
      stored(TOKEN).value == null
      rawStoredValue(TOKEN) == null
      0 * encryptionService.encryptSingle(blank)
    where:
      blank << ["", "   "]
  }

  def "sending the encrypted placeholder back leaves the secret unchanged"() {
    given:
      saveToken("xoxb-123")
      def before = rawStoredValue(TOKEN)
    when:
      def result = api.updateConfigs([new UpdatedSystemConfig().key(TOKEN).version(stored(TOKEN).version)
        .value(WebhookEncryptionService.ENCRYPTEDTEXT)], superuser, false)
    then:
      result.isEmpty()
      rawStoredValue(TOKEN) == before
  }

  def "a cleared secret can be set again"() {
    given:
      saveToken("xoxb-123")
      saveToken(null)
    when:
      saveToken("xoxb-456")
    then:
      stored(TOKEN).value == WebhookEncryptionService.ENCRYPTEDTEXT
      rawStoredValue(TOKEN).contains("xoxb-456")
  }

  def "an update using a stale version is rejected and nothing is written"() {
    given: "the token and channel are saved"
      saveToken("xoxb-123")
      api.updateConfigs([new UpdatedSystemConfig().key(CHANNEL).version(-1).value("C123")], superuser, false)
      def staleTokenVersion = stored(TOKEN).version
      def channelVersion = stored(CHANNEL).version
    and: "someone else changes the token"
      saveToken("xoxb-other")
    when: "the stale version is used to clear the token, along with a valid channel change"
      api.updateConfigs([
        new UpdatedSystemConfig().key(CHANNEL).version(channelVersion).value("C999"),
        new UpdatedSystemConfig().key(TOKEN).version(staleTokenVersion).value(null),
      ], superuser, false)
    then:
      def e = thrown(SystemConfigApi.UpdateSystemConfigFailedException)
      e.message.contains(TOKEN)
      rawStoredValue(TOKEN).contains("xoxb-other")
      stored(CHANNEL).value == "C123"
  }

  def "unknown keys are ignored rather than tripping the version check"() {
    when:
      def result = api.updateConfigs([new UpdatedSystemConfig().key("not.a.real.key").version(99).value("x")],
        superuser, false)
    then:
      result.isEmpty()
  }
}
