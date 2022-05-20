package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.AuthClientResult
import io.featurehub.web.security.oauth.OAuth2JerseyClient
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.Form
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import spock.lang.Specification

class OAuth2JerseyClientSpec extends Specification {
  def cleanup() {
    System.clearProperty("oauth2.redirectUrl")
  }
    def "every oauth2 provider gets asked to enhance the header and form"() {
      given: "we have a basic setup"
        System.setProperty("oauth2.redirectUrl", "http://redirect-url")
        def client = Mock(Client)
        def provider = Mock(OAuth2Provider)
        provider.clientId >> "client-id"
        provider.clientSecret >> "client-secret"
        provider.secretInHeader >> true
        provider.requestTokenUrl() >> { ->"http://token.com" }
      and: "a request"
        def req = Mock(Invocation.Builder)
        def target = Mock(WebTarget)
        def response = Response.status(400).build()
      and: "a oauth2 jersey client"
        def jClient = new OAuth2JerseyClient(client)
      when:
        jClient.requestAccess("code", provider)
      then:
        1 * provider.enhanceTokenRequest(req, { Form f ->
          f.asMap()["client_id"] == ["client-id"]
          !f.asMap().containsKey("client_secret")
        })
        1 * client.target((String)_) >> target
        1 * req.header("Authorization", _) >> req
//        1 * client.target({ String uri ->
//          uri == "http://redirect-uri"
//        }) >> target
        1 * target.request() >> req
        1 * req.accept(MediaType.APPLICATION_JSON) >> req
        1 * req.post(_) >> response
    }
}
