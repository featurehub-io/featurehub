class FHStepper {
  bool group;
  bool serviceAccount;
  bool feature;
  bool groupPermission;
  bool serviceAccountPermission;
  bool environment;
  bool application;

  FHStepper({this.group = true, this.serviceAccount = false, this.feature = false,
    this.groupPermission = false, this.serviceAccountPermission = false, this.environment = false, this.application = false});

  @override
  bool operator ==(Object other) =>
    identical(this, other) ||
      other is FHStepper &&
        runtimeType == other.runtimeType &&
        group == other.group &&
        serviceAccount == other.serviceAccount &&
        feature == other.feature &&
        groupPermission == other.groupPermission &&
        serviceAccountPermission == other.serviceAccountPermission &&
        environment == other.environment &&
        application == other.application;

  @override
  int get hashCode =>
    group.hashCode ^
    serviceAccount.hashCode ^
    feature.hashCode ^
    groupPermission.hashCode ^
    serviceAccountPermission.hashCode ^
    environment.hashCode ^
    application.hashCode;


}
