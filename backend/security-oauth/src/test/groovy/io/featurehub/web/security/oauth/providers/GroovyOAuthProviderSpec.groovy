package io.featurehub.web.security.oauth.providers

import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.core.Form
import spock.lang.Specification

class GroovyOAuthProviderSpec extends Specification {
  def data = []

  def sp(String key, String val) {
    System.setProperty(key, val)
    data.add(key)
  }

  def cleanup() {
    data.each { System.clearProperty(it) }
    data.clear()
  }
  def clearProperties() {

  }
  def "i can configure and augment the request"() {
    given:
      sp("oauth2.providers.generic.secret", "client-secret")
      sp("oauth2.providers.generic.auth-url", "http://blah.com?name=value")
      sp("oauth2.providers.generic.id", "client-secret")
      sp("oauth2.providers.generic.secret-in-header", "true")
      sp("oauth2.providers.generic.scope", "scope+more+scope")
      sp("oauth2.providers.generic.name-fields", "name")
      sp("oauth2.providers.generic.token-url", "http://token.com?name=value")
      sp("oauth2.providers.generic.email-field", "email")
      sp("oauth2.providers.generic.icon.url", "http://icon.com")
      sp("oauth2.providers.generic.icon.background-color", "black")
      sp("oauth2.providers.generic.icon.text", "icon-text")
      sp("oauth2.redirectUrl", "redirect-url")
      sp("oauth2.providers.generic.token-header-pairs", "fred=flintstone,child=bambam")
      sp("oauth2.providers.generic.token-form-pairs", "wilma=boss,betty=rubble")
    and:
      def req = Mock(Invocation.Builder)
      def form = new Form()
      def gen = new GenericOAuthProvider()
    when:
      gen.enhanceTokenRequest(req, form)
    then:
      1 * req.header('fred', 'flintstone')
      1 * req.header('child', 'bambam')
      form.asMap()['wilma'] == ['boss']
      form.asMap()['betty'] == ['rubble']

      gen.requestTokenUrl() == "http://token.com?name=value"
  }
}
