package io.featurehub.web.security.oauth

import io.featurehub.web.security.oauth.providers.SSOProviderCustomisation
import jakarta.ws.rs.NotFoundException
import org.glassfish.hk2.api.IterableProvider
import org.jetbrains.annotations.NotNull
import spock.lang.Specification

class AuthProvidersSpec extends Specification {
  IterableProvider<SSOProviderCollection> provider
  AuthProviders authProviders

  class FakeProviderCollection implements SSOProviderCollection {
    final private List<AuthProviderInfo> providers

    FakeProviderCollection(List<AuthProviderInfo> providers) {
      this.providers = providers
    }

    @Override
    List<AuthProviderInfo> getProviders() {
      return providers
    }

    @Override
    String requestRedirectUrl(@NotNull String provider) {
      return provider
    }
  }

  class FakeProvSource implements AuthProviderInfo {
    private final String code

    FakeProvSource(String code) {
      this.code = code
    }

    @Override
    String getCode() {
      return code
    }

    @Override
    boolean getExposeOnLoginPage() {
      return code == 'barney'
    }

    @Override
    SSOProviderCustomisation getIcon() {
      return null
    }
  }

  def setup() {
    provider = Mock(IterableProvider)
  }

  def "no providers works as expected"() {
    when:
      authProviders = new AuthProviders(provider)
    then:
      1 * provider.iterator() >> [].iterator()
      authProviders.codes.isEmpty()
      authProviders.providers.isEmpty()
  }

  def "two providers get flattened into one"() {
    given:
      def provider1 = new FakeProviderCollection([new FakeProvSource("betty"), new FakeProvSource("wilma")])
      def provider2 = new FakeProviderCollection([new FakeProvSource("fred"), new FakeProvSource("barney")])
    when:
      authProviders = new AuthProviders(provider)
    then:
      1 * provider.iterator() >> [provider1, provider2].iterator()
      authProviders.codes == ['barney', 'betty', 'fred', 'wilma']
      authProviders.find('barney').authInfo.exposeOnLoginPage
      !authProviders.find('fred').authInfo.exposeOnLoginPage
  }

  def "blank provider collection returns empty codes and a failure if called for a url"() {
    when:
      def blank = new BlankProviderCollection()
      def codes = blank.providers
      blank.requestRedirectUrl("blah")
    then:
      codes.isEmpty()
      thrown(NotFoundException)
  }

  def "no auth provider returns blank data"() {
    when:
      def noauth = new NoAuthProviders()
    then:
      noauth.providers.isEmpty()
      noauth.codes.isEmpty()
      noauth.find("anything") == null
  }
}
