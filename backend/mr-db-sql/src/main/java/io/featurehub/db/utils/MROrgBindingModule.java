package io.featurehub.db.utils;

import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.services.OrganizationSqlApi;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.inject.Singleton;

public class MROrgBindingModule extends AbstractBinder {
  @Override
  protected void configure() {
    bind(OrganizationSqlApi.class).to(OrganizationApi.class).in(Singleton.class);
  }
}
