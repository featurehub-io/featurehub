package io.featurehub.mr.utils;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.mr.model.Portfolio;
import jakarta.inject.Singleton;

@Singleton
public class PortfolioUtils {

  @ConfigKey("portfolio.admin.group.suffix")
  private String portfolioAdminGroupSuffix = "Administrators";

  public PortfolioUtils() {
    DeclaredConfigResolver.resolve(this);
  }

  public String formatPortfolioAdminGroupName(Portfolio pf) {
    return String.format("%s %s", pf.getName(), portfolioAdminGroupSuffix);
  }

}
