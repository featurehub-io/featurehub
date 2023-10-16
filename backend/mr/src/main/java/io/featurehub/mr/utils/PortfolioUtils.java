package io.featurehub.mr.utils;

import cd.connect.app.config.ConfigKey;
import io.featurehub.mr.model.Portfolio;
import jakarta.inject.Singleton;

@Singleton
public class PortfolioUtils {

  @ConfigKey("portfolio.admin.group.suffix")
  private String portfolioAdminGroupSuffix = "Administrators";

  public String formatPortfolioAdminGroupName(String pfName) {
    return String.format("%s %s", pfName, portfolioAdminGroupSuffix);
  }

}
