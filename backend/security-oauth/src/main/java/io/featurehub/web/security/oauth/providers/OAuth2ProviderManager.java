package io.featurehub.web.security.oauth.providers;

import org.glassfish.hk2.api.IterableProvider;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OAuth2ProviderManager implements OAuth2ProviderDiscovery {
  protected Map<String, OAuth2Provider> providerMap = new HashMap<>();

  @Inject
  public OAuth2ProviderManager(IterableProvider<OAuth2Provider> oAuth2Providers) {
    oAuth2Providers.forEach(p -> {
      providerMap.put(p.providerName(), p);
    });
  }

  public OAuth2Provider getProvider(String id) {
    return providerMap.get(id);
  }

  @Override
  public OAuth2Provider getProviderFromState(String state) {
    final int semi = state.indexOf(";");

    if (semi > 0) {
      return getProvider(state.substring(0, semi));
    }

    return null;
  }

  @Override
  public Collection<String> getProviders() {
    return providerMap.keySet();
  }

  @Override
  public String requestRedirectUrl(String provider) {
    // TODO: store state to ensure valid callback and XSRF attacks
    String state = URLEncoder.encode(provider + ";" + UUID.randomUUID().toString(), StandardCharsets.UTF_8);
    return providerMap.get(provider).requestAuthorizationUrl() + "&state=" + state;
  }
}
