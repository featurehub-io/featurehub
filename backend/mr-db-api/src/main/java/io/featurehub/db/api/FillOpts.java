package io.featurehub.db.api;

public enum FillOpts {
  Environments,
  People,
  Applications,
  ApplicationIds,
  Portfolios,
  PortfolioIds,
  Members,
  Groups,
  Acls,
  Features,
  Permissions,
  ServiceAccounts,
  SdkURL,
  IgnoreEmptyPermissions,
  Archived, // include archived records
  Details // full details of the current object type (e.g. if people, all details about people)
}
