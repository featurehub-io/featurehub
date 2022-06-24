package io.featurehub.db.api;

public enum FillOpts {
  Environments,
  People,
  SimplePeople,
  PersonLastLoggedIn,
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
  ServiceAccountPermissionFilter,
  RolloutStrategies,
  MetaData, // include meta-data for features and feature values
  Archived, // include archived records
  Details, // full details of the current object type (e.g. if people, all details about people)
  CountGroups, // only count groups don't include them
}
