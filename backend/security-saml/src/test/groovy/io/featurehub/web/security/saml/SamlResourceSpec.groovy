package io.featurehub.web.security.saml

import io.featurehub.web.security.oauth.SSOCompletionListener
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.core.Response
import spock.lang.Specification

class SamlResourceSpec extends Specification {
  SamlConfigSources samlSources
  SSOCompletionListener listener
  SamlImplementationProvider samlImplProvider
  SamlResource resource
  SamlServiceProviderConfig providerConfig
  static final String failureUrl = "http://failure"
  static final String successUrl = "http://success"

  def setup() {
    samlSources = Mock(SamlConfigSources)
    listener = Mock(SSOCompletionListener)
    samlImplProvider = Mock(SamlImplementationProvider)
    providerConfig = Mock(SamlServiceProviderConfig)

    resource = new SamlResource(samlSources, listener, samlImplProvider,
      successUrl, failureUrl, false)
  }

  def "invalid parameters cause failure"() {
    when: "i make a request with no saml payload"
      resource.receiveSamlPayload(payload, "sample")
    then:
      thrown(NotAuthorizedException)
      0 * _
    where:
      payload  | _
      null     | _
      ""       | _
      "     "  | _
  }

  def "if a request is made for a unknown provider we get unauthorized"() {
    when: "i make a request for an unknown provider"
      resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> null
      thrown(NotAuthorizedException)
      0 * _
  }

  def response302(Response response, String location = failureUrl) {
    response.status == 302
    response.headers.containsKey("location")
    response.headers.get("location").get(0) == location.toURI()
  }

  def "if the setup is incomplete, expect a failure"() {
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> Mock(SamlServiceProviderConfig)
      1 * listener.initialAppSetupComplete() >> false
      response302(response)
      0 * _
  }

  def "we have an invalid payload"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> Mock(SamlServiceProviderConfig)
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.valid >> false
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      1 * payload.validationException >> new RuntimeException("blah")
      response302(response)
      0 * _
  }

  def "we have a valid payload but are missing name fields"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * providerConfig.mustMatchEmailDomains >> []
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.attributes >> [:]
      1 * payload.nameId >> "fred@fred.com"
      1 * payload.valid >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      response302(response, resource.samlMisconfiguredUrl)
      0 * _
  }

  def "we have a valid payload and a display name so we should get a completion request"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * providerConfig.mustMatchEmailDomains >> ["fred.com"]
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.attributes >> ["urn:oid:2.16.840.1.113730.3.1.241":["fred"]]
      1 * payload.nameId >> "fred@fred.com"
      1 * payload.valid >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      1 * listener.successfulCompletion("fred@fred.com", "fred", false, failureUrl, successUrl) >> Response.ok().build()
      response.status == 200
      0 * _
  }

  def "we have a valid payload but an invalid email address"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * providerConfig.mustMatchEmailDomains >> ["sausage.com"]
      1 * providerConfig.samlProviderName >> "sausage"
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.nameId >> "fred@fred.com"
      1 * payload.valid >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      response302(response)
      0 * _
  }

  def "we have a valid payload and first/last names so we should get a completion request"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * providerConfig.mustMatchEmailDomains >> []
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.attributes >> ["urn:oid:2.5.4.42":["fred"], "urn:oid:2.5.4.4": ["xml"]]
      1 * payload.nameId >> "fred@fred.com"
      1 * payload.valid >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      1 * listener.successfulCompletion("fred@fred.com", "fred xml", false, failureUrl, successUrl) >> Response.ok().build()
      response.status == 200
      0 * _
  }


  def "we have a valid payload and only first name so it will fail"() {
    given: "a mock payload decode"
      def payload = Mock(FeatureHubSamlResponse)
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * providerConfig.mustMatchEmailDomains >> []
      1 * listener.initialAppSetupComplete() >> true
      1 * payload.attributes >> ["urn:oid:2.5.4.42":["fred"]]
      1 * payload.nameId >> "fred@fred.com"
      1 * payload.valid >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> payload
      response302(response, resource.samlMisconfiguredUrl)
      0 * _
  }

  def "an error in decoding the payload causes a redirect to failure"() {
    when:
      def response = resource.receiveSamlPayload("blah", "sample")
    then:
      1 * samlSources.getSourceFromRegistrationId("sample") >> providerConfig
      1 * listener.initialAppSetupComplete() >> true
      1 * samlImplProvider.decodeResponse(_, "blah") >> { throw new RuntimeException("bad") }
      response302(response)
      0 * _
  }

  def "when we request a redirect from a non existent provider, we get a non authorized request"() {
    when:
      def response = resource.authRedirect("blah")
    then:
      1 * samlSources.getSourceFromRegistrationId("blah") >> null
      thrown(NotAuthorizedException)
      0 * _
  }

  def "when we request a redirect from a known provider, we get a proper redirect"() {
    given:
      def config = Mock(SamlServiceProviderConfig)
    when:
      def response = resource.authRedirect("blah")
    then:
      1 * samlSources.getSourceFromRegistrationId("blah") >> config
      1 * samlImplProvider.createRequest(config) >> "http://trouper"
      response302(response, "http://trouper")
      0 * _
  }
}
