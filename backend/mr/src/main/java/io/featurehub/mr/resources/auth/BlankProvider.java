package io.featurehub.mr.resources.auth;

import javax.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.Collections;

// ensures we have at least one of these as injection requires it
public class BlankProvider implements AuthProvider {
  @Override
  public Collection<String> getProviders() {
    return Collections.emptyList();
  }

  @Override
  public String requestRedirectUrl(String provider) {
    // if we get picked there is something seriously wrong
    throw new NotFoundException();
  }
}
