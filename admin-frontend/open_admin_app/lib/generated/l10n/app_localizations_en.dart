// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get signInTitle => 'Sign in to FeatureHub';

  @override
  String get signInWithCredentials => 'or sign in with a username and password';

  @override
  String get emailLabel => 'Email address';

  @override
  String get emailRequired => 'Please enter your email';

  @override
  String get passwordLabel => 'Password';

  @override
  String get passwordRequired => 'Please enter your password';

  @override
  String get incorrectCredentials => 'Incorrect email address or password';

  @override
  String get signInButton => 'Sign in';

  @override
  String get darkMode => 'Dark mode';

  @override
  String get lightMode => 'Light mode';

  @override
  String get signOut => 'Sign out';

  @override
  String get applicationSettings => 'Application Settings';

  @override
  String get portfolioSettings => 'Portfolio Settings';

  @override
  String get organizationSettings => 'Organization Settings';

  @override
  String get portfolios => 'Portfolios';

  @override
  String get users => 'Users';

  @override
  String get adminServiceAccounts => 'Admin Service Accounts';

  @override
  String get systemConfig => 'System Config';

  @override
  String get groups => 'Groups';

  @override
  String get serviceAccounts => 'Service Accounts';

  @override
  String get environments => 'Environments';

  @override
  String get groupPermissions => 'Group permissions';

  @override
  String get serviceAccountPermissions => 'Service account permissions';

  @override
  String get integrations => 'Integrations';

  @override
  String get applications => 'Applications';

  @override
  String get features => 'Features';

  @override
  String get featureGroups => 'Feature Groups';

  @override
  String get applicationStrategies => 'Application Strategies';

  @override
  String get apiKeys => 'API Keys';

  @override
  String deleteConfirmTitle(String thing) {
    return 'Are you sure you want to delete the $thing?';
  }

  @override
  String get cannotBeUndone => 'This cannot be undone!';

  @override
  String get cancel => 'Cancel';

  @override
  String get delete => 'Delete';

  @override
  String get reset => 'Reset';

  @override
  String get ok => 'OK';

  @override
  String get edit => 'Edit';

  @override
  String get viewDocumentation => 'View documentation';

  @override
  String get createNewApplication => 'Create new application';

  @override
  String get applicationsDocumentation => 'Applications Documentation';

  @override
  String get republishPortfolioCache => 'Republish portfolio cache';

  @override
  String get republishPortfolioCacheWarningTitle =>
      'Warning: Intensive system operation';

  @override
  String get republishPortfolioCacheWarningContent =>
      'Are you sure you want to republish this entire portfolio\'s cache?';

  @override
  String get featureFlags => 'Feature flags';

  @override
  String get showMore => 'Show more';

  @override
  String get republishCacheForApp => 'Republish cache for this app';

  @override
  String get manageUsers => 'Manage users';

  @override
  String get manageUsersDocumentation => 'Manage Users Documentation';

  @override
  String get createNewUser => 'Create new user';

  @override
  String get searchUsers => 'Search users';

  @override
  String get columnName => 'Name';

  @override
  String get columnStatus => 'Status';

  @override
  String get columnEmail => 'Email';

  @override
  String get columnLastSignIn => 'Last sign in (UTC)';

  @override
  String get columnActions => 'Actions';

  @override
  String get notYetRegistered => 'Not yet registered';

  @override
  String get statusActive => 'active';

  @override
  String get statusDeactivated => 'deactivated';

  @override
  String get activateUserTooltip => 'Activate user';

  @override
  String activateUserTitle(String name) {
    return 'Activate user \'$name\'';
  }

  @override
  String activateUserConfirm(String email) {
    return 'Are you sure you want to activate user with email address $email?';
  }

  @override
  String get activate => 'Activate';

  @override
  String userActivated(String name) {
    return 'User \'$name\' activated!';
  }

  @override
  String userDeactivated(String name) {
    return 'User \'$name\' deactivated!';
  }

  @override
  String get userInformation => 'User information';

  @override
  String get registrationUrl => 'Registration URL';

  @override
  String get copyUrlToClipboard => 'Copy URL to Clipboard';

  @override
  String get registrationExpired => 'Registration expired';

  @override
  String get renewRegistrationUrl =>
      'Renew registration URL and copy to clipboard';

  @override
  String get registrationUrlRenewed =>
      'Registration URL renewed and copied to clipboard';

  @override
  String get cantDeleteYourself => 'You can\'t delete yourself!';

  @override
  String get cantDeleteYourselfContent =>
      'To delete yourself from the organization, you\'ll need to contact a site administrator.';

  @override
  String get deleteUserContent =>
      'This user will be removed from all groups and deactivated in this organization.';
}
