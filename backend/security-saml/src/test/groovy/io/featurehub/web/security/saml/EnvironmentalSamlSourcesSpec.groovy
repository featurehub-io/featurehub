package io.featurehub.web.security.saml

import cd.connect.app.config.ThreadLocalConfigurationSource
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.Response
import spock.lang.Specification

class EnvironmentalSamlSourcesSpec extends Specification {

  def cleanup() {
    ThreadLocalConfigurationSource.clearContext()
  }

  def "no saml means no environmental saml sources"() {
    when: "i create an environmental saml source with no config"
      def source = new EnvironmentSamlSources(Mock(EnvironmentSamlSourceProvider))
    then:
      source.providers.isEmpty()
  }

  def "when i specify two sml sources it asks to create two of them"() {
    given: "i have two sources defined"
      ThreadLocalConfigurationSource.createContext([
        "saml.idp-providers": "sampleX, sampleY,,",])
    and: "i have mocked out the provider"
      def provider = Mock(EnvironmentSamlSourceProvider)
    when: "i ask to create the environmental sources"
      def source = new EnvironmentSamlSources(provider)
    then:
      source.providers.size() == 2
      1 * provider.create("sampleX", _) >> Mock(SamlServiceProviderConfig)
      1 * provider.create("sampleY", _) >> Mock(SamlServiceProviderConfig)
  }

  def testX509() {
    '''MIIDojCCAooCCQCKjhXQRyVBvjANBgkqhkiG9w0BAQsFADCBkjELMAkGA1UEBhMC
a24xDjAMBgNVBAgMBVVzYmtlMRIwEAYDVQQHDAlOdW5jaHVrYXMxEzARBgNVBAoM
ClNwb3JlcyBJbmMxEDAOBgNVBAsMB1NlY3Rpb24xGTAXBgNVBAMMEHNhbWwuZXhh
bXBsZS5jb20xHTAbBgkqhkiG9w0BCQEWDm1lQGV4YW1wbGUuY29tMB4XDTIyMDYx
NzIwMzIzM1oXDTIzMDYxNzIwMzIzM1owgZIxCzAJBgNVBAYTAmtuMQ4wDAYDVQQI
DAVVc2JrZTESMBAGA1UEBwwJTnVuY2h1a2FzMRMwEQYDVQQKDApTcG9yZXMgSW5j
MRAwDgYDVQQLDAdTZWN0aW9uMRkwFwYDVQQDDBBzYW1sLmV4YW1wbGUuY29tMR0w
GwYJKoZIhvcNAQkBFg5tZUBleGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQAD
ggEPADCCAQoCggEBAPA9MhGc+zMz7Z/5IG2HjINFryYmFwD956nPK+Y1xGOfvFIu
bKkfUFiQ+Lz97L2LjltsWbADC2rpgFVTPuw6gd+KQEYPTzhbVm9hW+nGfuionI8o
kjS23QkdLH4xef0AUfhaa5BZq/XKZ+G58+tLAamBv/q8Xsn6WvS5Fx76Wfc5qKcI
p6SsPJO9q8fEpaSpeQtvQfQPqteYkAYUyHbm91MXBZdvVTKYNuoX1p0iwiCjZBWd
B+SivKPiB95KcqzEFGUGgPPfj0qCPmlhqmL8YLEvACxPGNxuOA87COziRCO/O7DU
mlYy3MBCyTkM2vVqR35zEq9p8BxparWIo8vBXQMCAwEAATANBgkqhkiG9w0BAQsF
AAOCAQEA7nj2p3MIg1glltotyW8Via4+eIbX6Qq4oTN0pBETs3Jf/f7SFvyliuKo
7SDatOqi77H9N4gKXVjqbYINCDvVa8ymH5wZA27zKvKmtFbR67ywRnaHdGhEwmHO
k/re3t2AVJVUi7OhrLSGqPpVMsolTaw5MObPbMCvaDT3Cq96Y+B4a3FBpEbVWaQg
g4ikxWPIHvwhsLdfHRr+/gGoHH5TspuTm8miYpbp0n9VnhOvK7VstVGsbyAcxlvq
LElAZkGwM4t54mPu6zLTEzn7we+PTRu/OtIgLZkras/hQjRxNCDMMopgzwi21h9B
sxnr8NZOKBBDWrweiw/ntYuLQ4KpVA==
'''.split("\n").join()
  }

  def testPrivateKey() {
    '''MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDwPTIRnPszM+2f
+SBth4yDRa8mJhcA/eepzyvmNcRjn7xSLmypH1BYkPi8/ey9i45bbFmwAwtq6YBV
Uz7sOoHfikBGD084W1ZvYVvpxn7oqJyPKJI0tt0JHSx+MXn9AFH4WmuQWav1ymfh
ufPrSwGpgb/6vF7J+lr0uRce+ln3OainCKekrDyTvavHxKWkqXkLb0H0D6rXmJAG
FMh25vdTFwWXb1UymDbqF9adIsIgo2QVnQfkoryj4gfeSnKsxBRlBoDz349Kgj5p
Yapi/GCxLwAsTxjcbjgPOwjs4kQjvzuw1JpWMtzAQsk5DNr1akd+cxKvafAcaWq1
iKPLwV0DAgMBAAECggEAJXW1V++9b6d6NwaWmt2W+GUmQXGRBrOQqvbF5pidrdcb
e0kH4nsigRzh3D5P6uJW6USE26At6JbOTv+zAQzCcYPSL9p1RNlhKe7pcRNleIvK
FbyCx7t3zdMBvA+k8OFa5rnaqeCBosrT1euz8A17Dd5QfB1hPmdORXiS9V6eJqFG
o5yblOrWQEnC9b9kGWwqjlDUyzsRIJfcU6uqNq+OF1MZRomfDRpAYw8alYJLgFUy
AXWcGhpLGrsYSVc5Kbrkm8uAOZ2Hb5DyZgh6JJ0NB3M8bxdcll0h0C/44D0vEo5f
pBOD9DZFTjUziXaGLWnsK0nQMilx2pLwnsYfvt3PcQKBgQD8tkH0zVVIBNPdPvub
ByZf08wK2RgE5pSBJPwBSAIESZnV7116q4bUSAHP7+YPct5cFrD76keJgJ6rgVkl
95eZ6e9YehI/XxeivnEnhNwNoFXNlCM0lXO2ASgugtwjp+Xw+7eGu6AsFntCY6m7
EwI8GWPr+7Dl69FFdCsLIr5uiQKBgQDzXWSK3v6x8YpOAgzb/9UEXM3ZlkYTOV1R
c9kTR80bJbeXRzLbSTvI9dILq+/9iqe82a4n5HORzmi8rLfrW/bik7z/aeaJorUv
2Nydsgfu8q4P49tVpEB42mXlm1k9Uw3R0sU+5j9hnTdJio6l1e2naNE8sqEqWvYb
vjP5FUtsKwKBgARGvUT8OIY4drFWwIE2FMMoXVqNo+dpU3f//Te0VTxnVnAVVdqe
jnCHK2iuYZE/W13pkGMi6sT75TN3w99tmiYjnEY+ApMJ8+dwG5AUonikju8ko5ff
M48P4/MtibYcwDpcuBVSlXpJgxpzf0rIHpCPyW4T20F4xRjMx3Gvy9MpAoGBAMXn
ye6Av2CXiyA63Jg/qMv+aEwsv1m8yZ2Gdx4Nwsbz2iPEe6AGIv8eoKxrKyPlENkr
sEuypSUKVPQyBPL5+4BwIj8WsKp2zZ9WXLpE39y6L0CuzFwN0Xw2Jq0csuqC0vcx
zsOC3EIWZrKAYdaMbNI6FAWnQha+l+shtwQ1A/u3AoGBAJsrPFvGPrnZzDY0Lace
m2VXKPR9Mz4XNNU0KxnidRXQaA22tX8eS6JO4e6UsW8j8h9MmmYKUB4wYWqL+rXM
H3ILaBGEfrCAtM9tusR9oF1qFtNeR5q9vbYvrzY05fxNxWSVAsPu/0ViHw8kMAfo
vPAw98CTIIgwcWti8n9Xch0v
'''.split("\n").join()
  }

  def testMetadataText() {
    '''<?xml version="1.0" encoding="UTF-8"?><md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata" entityID="https://nebula.example.com/o/saml2?" validUntil="2025-12-02T07:07:29.000Z">
  <md:IDPSSODescriptor WantAuthnRequestsSigned="false" protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
    <md:KeyDescriptor use="signing">
      <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:X509Data>
          <ds:X509Certificate>MIIDrjCCApYCCQCTlyzvr0plAzANBgkqhkiG9w0BAQsFADCBmDELMAkGA1UEBhMC
            dHcxEDAOBgNVBAgMB1R3aW5rbGUxEjAQBgNVBAcMCU1ldGF2ZXJzZTEPMA0GA1UE
            CgwGR2FsYXh5MQ8wDQYDVQQLDAZOZWJ1bGExGzAZBgNVBAMMEm5lYnVsYS5leGFt
            cGxlLmNvbTEkMCIGCSqGSIb3DQEJARYVbWVAbmVidWxhLmV4YW1wbGUuY29tMB4X
            DTIyMDYxNzIwMzUyNloXDTIzMDYxNzIwMzUyNlowgZgxCzAJBgNVBAYTAnR3MRAw
            DgYDVQQIDAdUd2lua2xlMRIwEAYDVQQHDAlNZXRhdmVyc2UxDzANBgNVBAoMBkdh
            bGF4eTEPMA0GA1UECwwGTmVidWxhMRswGQYDVQQDDBJuZWJ1bGEuZXhhbXBsZS5j
            b20xJDAiBgkqhkiG9w0BCQEWFW1lQG5lYnVsYS5leGFtcGxlLmNvbTCCASIwDQYJ
            KoZIhvcNAQEBBQADggEPADCCAQoCggEBAMuje4/VzP9oRVqoTUhnV1xSXalhjC2f
            ftwwVqdkIniBvD3Gw4/sj4wSFsScfCKnk4cjEmUBvBA4k7ZtTcItOefloA8KGPpH
            /DWw7vCzVdoEYy8PTiJ6YgsaMX1HOEynw93RQ+qSnhQIg8E35j7WI2pIj63ichyO
            E3pDeQEl1KY8ojlOw57WPerRJ8QzboCmnX8AdY/n8gzW3WcrAPg6y9EAOdD7Dhf9
            1xsQsRrZvFWAbAfoRF0P2YRqWKRMR0ti9yPcNN/86NwtX+jTXskmebW5I21ITpys
            7Np5Nq3BfBvm4iy9EkZ6gTPntgne8BgMAo8OIauK9ghyTI/aEImVqkcCAwEAATAN
            BgkqhkiG9w0BAQsFAAOCAQEAGLu+dy73XLnl9lDXIpxRdyLKaQSbq9mYP/dFaqLE
            bQM0Z8joxI0sN1q/JyYryVrfbdOYgVy+D6uqNrHoJex6x5yqEK1MsuCYj9MI7tV5
            6MHsLAu9SX6gEgAqN695RNQjkEOVBwTa/GEbII3DuwdSXUotEHe3ZLk7+A7MazuJ
            UEzOcYp5P9bhPSsW7YnOCfK5cHupsbfxztlNzLfh3NsexPMfm7FJLq+Nycf/7yU4
            EJCFBkcIuTeGtTk8ODmJUzCjohfIHV0qeiim/3YdqoIv9b+ztQhzNZSvkCPTjP5J
            y9Zd9QwYWqXpU6bw2MnnC7Zun8uz4N8k/OBXqlWDK0POyw==</ds:X509Certificate>
        </ds:X509Data>
      </ds:KeyInfo>
    </md:KeyDescriptor>
    <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat>
    <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect" Location="https://nebula.example.com/o/saml2/idp?"/>
    <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" Location="https://nebula.example.com/o/saml2/idp?"/>
  </md:IDPSSODescriptor>
</md:EntityDescriptor>
'''
  }

  def "we can setup directly from the top level"() {
    given: "i have set up a saml source"
      ThreadLocalConfigurationSource.createContext([
        "saml.idp-providers": "sample",
        "saml.sample.idp.entity-id": "http://entityid",
        "saml.sample.sp.base-url": "http://baseurl",
        "saml.sample.sp.x509-cert": testX509(),
        "saml.sample.sp.x509-cert-new": testX509(),
        "saml.sample.sp.private-key": testPrivateKey(),
        "saml.sample.idp.metadata-file": getClass().getResource("/file-metadata.xml").file,
      ])
    when: "i ask to create the environmental sources"
      def source = new EnvironmentSamlSources(new EnvironmentSamlSourceProviderImpl())
    then:
      source.providers.size() == 1
      source.getSourceFromRegistrationId("sample").idpEntityId == "http://entityid"
      source.requestRedirectUrl("sample") != null
  }

  def "we have metadata as a file source"() {
    given: "i have set up a saml source"
      ThreadLocalConfigurationSource.createContext([
        "saml.sample.idp.entity-id": "http://entityid",
        "saml.sample.sp.base-url": "http://baseurl",
        "saml.sample.sp.x509-cert": testX509(),
        "saml.sample.sp.x509-cert-new": testX509(),
        "saml.sample.sp.private-key": testPrivateKey(),
        "saml.sample.idp.metadata-file": getClass().getResource("/file-metadata.xml").file,
      ])
    and:
      def client = Mock(Client)
    when: "i create the environment"
      def source = new EnvironmentalSamlServiceProviderConfig("sample", client)
      def config = new SamlConfigAsAuthProvider(source)
    then:
      with(source) {
        idpEntityId == "http://entityid"
        samlProviderName == "sample"
        saml2Settings().idpx509cert != null
        saml2Settings().idpEntityId == "https://nebula.example.com/o/saml2?"
//        saml2Settings().idpx509certMulti.size() == 2
      }
      !config.exposeOnLoginPage
      config.icon == null
      config.code == "sample"
  }

  def "we have metadata from the idp url"() {
    given: "i have set up a saml source"
      ThreadLocalConfigurationSource.createContext([
        "saml.sample.idp.entity-id": "http://entityid",
        "saml.sample.sp.base-url": "http://baseurl",
        "saml.sample.sp.x509-cert": testX509(),
        "saml.sample.sp.x509-cert-new": testX509(),
        "saml.sample.sp.private-key": testPrivateKey(),
      ])
    and:
      def client = Mock(Client)
      def target = Mock(WebTarget)
      def builder = Mock(Invocation.Builder)
      def response = Mock(Response) // byte-buddy to the rescue
    when: "i create the environment"
      def source = new EnvironmentalSamlServiceProviderConfig("sample", client)
      def config = new SamlConfigAsAuthProvider(source)
    then:
      1 * client.target("http://entityid") >> target
      1 * target.request() >> builder
      1 * builder.get() >> response
      1 * response.readEntity(String) >> testMetadataText()
      with(source) {
        idpEntityId == "http://entityid"
        samlProviderName == "sample"
        saml2Settings().idpx509cert != null
        saml2Settings().idpEntityId == "https://nebula.example.com/o/saml2?"
      }
      !config.exposeOnLoginPage
      config.icon == null
      config.code == "sample"
  }

  def "we can have a saml source with no UI config"() {
    given: "i have set up a saml source"
      ThreadLocalConfigurationSource.createContext([
        "saml.sample.idp.entity-id": "http://entityid",
        "saml.sample.sp.base-url": "http://baseurl",
        "saml.sample.login.icon-url": "http://icon-url",
        "saml.sample.login.button-background-color": "sample-color",
        "saml.sample.login.button-text": "text",
        "saml.sample.sp.x509-cert": testX509(),
        "saml.sample.email-domains": "FrEd.COM,,wilma.com,",
        "saml.sample.sp.x509-cert-new": testX509(),
        "saml.sample.sp.private-key": testPrivateKey(),
        "saml.sample.idp.metadata-text": testMetadataText(),
      ])
    and:
      def client = Mock(Client)
    when: "i create the environment"
      def source = new EnvironmentalSamlServiceProviderConfig("sample", client)
      def config = new SamlConfigAsAuthProvider(source)
    then:
      with(source) {
        buttonBackgroundColor == "sample-color"
        idpEntityId == "http://entityid"
        buttonText == "text"
        icon === "http://icon-url"
        samlProviderName == "sample"
        saml2Settings().idpx509cert != null
        saml2Settings().idpEntityId == "https://nebula.example.com/o/saml2?"
        mustMatchEmailDomains == ['fred.com', 'wilma.com']
      }
      with(config.icon) {
        buttonBackgroundColor == "sample-color"
        buttonText == "text"
        icon === "http://icon-url"
      }
  }


}
