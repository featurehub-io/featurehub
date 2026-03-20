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

  @override
  String get managePortfolios => 'Manage portfolios';

  @override
  String get managePortfoliosDocumentation => 'Manage Portfolios Documentation';

  @override
  String get createNewPortfolio => 'Create new portfolio';

  @override
  String get searchPortfolios => 'Search portfolios';

  @override
  String get republishSystemCache => 'Republish system cache';

  @override
  String get republishEntireCacheWarningContent =>
      'Are you sure you want to republish the entire cache?';

  @override
  String get manageGroupMembers => 'Manage group members';

  @override
  String get userGroupsDocumentation => 'User Groups Documentation';

  @override
  String get fetchingGroups => 'Fetching Groups...';

  @override
  String get createNewGroup => 'Create new group';

  @override
  String get addMembers => 'Add members';

  @override
  String get columnMemberType => 'Type (User or Admin Service Account)';

  @override
  String get memberTypeUser => 'User';

  @override
  String get memberTypeServiceAccount => 'Service Account';

  @override
  String get removeFromGroup => 'Remove from group';

  @override
  String memberRemovedFromGroup(String name, String groupName) {
    return '\'$name\' removed from group \'$groupName\'';
  }

  @override
  String get noGroupsFound => 'No groups found in the portfolio';

  @override
  String get portfolioGroups => 'Portfolio groups';

  @override
  String get selectGroup => 'Select group';

  @override
  String addMembersToGroupTitle(String groupName) {
    return 'Add members to group $groupName';
  }

  @override
  String get enterMembersToAdd => 'Enter members to add to group...';

  @override
  String get addToGroup => 'Add to group';

  @override
  String groupUpdated(String name) {
    return 'Group \'$name\' updated!';
  }

  @override
  String get manageServiceAccounts => 'Manage service accounts';

  @override
  String get serviceAccountsDocumentation => 'Service Accounts Documentation';

  @override
  String get createNewServiceAccount => 'Create new service account';

  @override
  String get saHasPermissions =>
      'The service account has permissions to one or more environments in this application';

  @override
  String get saHasNoPermissions =>
      'The service account has no permissions to any environments in this application';

  @override
  String get changeAccess => 'Change access';

  @override
  String get addAccess => 'Add access';

  @override
  String get saDeleteContent =>
      'All applications using this service account will no longer have access to features!\n\nThis cannot be undone!';

  @override
  String saDeleted(String name) {
    return 'Service account \'$name\' deleted!';
  }

  @override
  String saDeleteError(String name) {
    return 'Couldn\'t delete service account $name';
  }

  @override
  String get editServiceAccount => 'Edit service account';

  @override
  String get saNameLabel => 'Service account name';

  @override
  String get saNameRequired => 'Please enter a service account name';

  @override
  String get saNameTooShort =>
      'Service account name needs to be at least 4 characters long';

  @override
  String get saDescriptionLabel => 'Service account description';

  @override
  String get saDescriptionRequired =>
      'Please enter service account description';

  @override
  String get saDescriptionTooShort =>
      'Service account description needs to be at least 4 characters long';

  @override
  String get update => 'Update';

  @override
  String get create => 'Create';

  @override
  String saUpdated(String name) {
    return 'Service account \'$name\' updated!';
  }

  @override
  String saCreated(String name) {
    return 'Service account \'$name\' created!';
  }

  @override
  String saAlreadyExists(String name) {
    return 'Service account \'$name\' already exists';
  }

  @override
  String get resetClientApiKeys => 'Reset client eval API keys';

  @override
  String get resetServerApiKeys => 'Reset server eval API keys';

  @override
  String get featuresConsole => 'Features console';

  @override
  String get featuresDocumentation => 'Features Documentation';

  @override
  String get createNewFeature => 'Create New Feature';

  @override
  String get noApplicationsInPortfolio =>
      'There are no applications in this portfolio';

  @override
  String get noApplicationsAccessMessage =>
      'Either there are no applications in this portfolio or you don\'t have access to any of the applications.\nPlease contact your administrator.';

  @override
  String get apiKeysDocumentation => 'API Keys Documentation';

  @override
  String get goToServiceAccountsSettings => 'Go to service accounts settings';

  @override
  String get noServiceAccountsAvailable => 'No service accounts available';

  @override
  String get permissions => 'Permissions';

  @override
  String get clientServerApiKeys => 'Client & Server API Keys';

  @override
  String get clientEvalApiKey => 'Client eval API Key';

  @override
  String get serverEvalApiKey => 'Server eval API Key';

  @override
  String get noPermissionsDefined => 'No permissions defined';

  @override
  String get apiKeyUnavailable =>
      'API Key is unavailable because your current permissions for this environment are lower level';

  @override
  String get manageAdminSdkServiceAccounts =>
      'Manage admin SDK service accounts';

  @override
  String get adminServiceAccountsDocumentation =>
      'Admin Service Accounts Documentation';

  @override
  String get createAdminServiceAccount => 'Create Admin Service Account';

  @override
  String get createUserInstructions =>
      'To create a new user please first provide their email address';

  @override
  String get invalidEmailAddress => 'Please enter a valid email address';

  @override
  String get addUserToGroupsHint =>
      'Add user to some portfolio groups or leave it blank to add them later';

  @override
  String get userCreated => 'User created!';

  @override
  String get sendRegistrationUrlInstructions =>
      'You will need to email this URL to the new user, so they can complete their registration and set their password.';

  @override
  String get userCanSignIn =>
      'The user can now sign in and they will be able to access the system.';

  @override
  String get close => 'Close';

  @override
  String get createAnotherUser => 'Create another user';

  @override
  String userEmailAlreadyExists(String email) {
    return 'User with email \'$email\' already exists';
  }

  @override
  String get appSettingsTitle => 'Application settings';

  @override
  String get tabEnvironments => 'Environments';

  @override
  String get tabGroupPermissions => 'Group Permissions';

  @override
  String get tabServiceAccountPermissions => 'Service Account Permissions';

  @override
  String get tabIntegrations => 'Integrations';

  @override
  String get oauth2NotAuthorized =>
      'You are not authorised to access FeatureHub';

  @override
  String get oauth2ContactAdmin =>
      'Please contact your administrator and ask them nicely to add your email to your organization\'s user list';

  @override
  String get registerUrlUnexpectedError =>
      'Unexpected error occured\n.Please contact your FeatureHub administrator.';

  @override
  String get registerUrlExpiredOrInvalid =>
      'This Register URL is either expired or invalid.\n\nCheck your URL is correct or contact your FeatureHub administrator.';

  @override
  String get validatingInvitationUrl => 'Validating your invitation URL';

  @override
  String get welcomeToFeatureHub => 'Welcome to FeatureHub';

  @override
  String get registerCompleteDetails =>
      'To register please complete the following details';

  @override
  String get nameLabel => 'Name';

  @override
  String get nameRequired => 'Please enter your name';

  @override
  String get passwordMustBe7Chars => 'Password must be at least 7 characters!';

  @override
  String get passwordsDoNotMatch => 'Passwords don\'t match';

  @override
  String get confirmPasswordLabel => 'Confirm Password';

  @override
  String get confirmPasswordRequired => 'Please confirm your password';

  @override
  String get registerButton => 'Register';

  @override
  String get passwordStrengthWeak => 'Weak';

  @override
  String get passwordStrengthBelowAverage => 'Below average';

  @override
  String get passwordStrengthGood => 'Good';

  @override
  String get passwordStrengthStrong => 'Strong';

  @override
  String get notFoundMessage => 'Sorry, we couldn\'t find the page!';

  @override
  String get pageNotFoundMessage =>
      'Looks like we couldn\'t find any relevant information to display!';

  @override
  String get featureGroupsDocumentation => 'Feature Groups Documentation';

  @override
  String get createFeatureGroup => 'Create feature group';

  @override
  String get applicationStrategiesDocumentation =>
      'Application Strategies Documentation';

  @override
  String get editUser => 'Edit user';

  @override
  String get resetPassword => 'Reset password';

  @override
  String get editEmailAddress => 'Edit email address';

  @override
  String get editNames => 'Edit names';

  @override
  String get removeOrAddUserToGroup =>
      'Remove user from a group or add a new one';

  @override
  String get saveAndClose => 'Save and close';

  @override
  String userUpdated(String name) {
    return 'User $name has been updated';
  }

  @override
  String get resetPasswordInstructions =>
      'After you reset the password below, make sure you email the new password to the user.';

  @override
  String get newPasswordLabel => 'New password';

  @override
  String get newPasswordRequired => 'Please enter new password';

  @override
  String get confirmNewPasswordLabel => 'Confirm new password';

  @override
  String get confirmNewPasswordRequired => 'Please confirm new password';

  @override
  String get save => 'Save';

  @override
  String get groupPrefix => 'Group: ';

  @override
  String get applicationPrefix => 'Application: ';

  @override
  String get environmentPrefix => 'Environment: ';

  @override
  String get applyAllChanges => 'Apply all changes';

  @override
  String featureGroupSettingsUpdated(String name) {
    return 'Settings for group \'$name\' have been updated';
  }

  @override
  String get noPermissions => 'No permissions';

  @override
  String get editSplitTargetingRules => 'Edit split targeting rules';

  @override
  String get viewSplitTargetingRules => 'View split targeting rules';

  @override
  String get removeStrategy => 'Remove strategy';

  @override
  String get addRolloutStrategy => 'Add rollout strategy';

  @override
  String get featuresList => 'Features List';

  @override
  String get addFeature => 'Add Feature';

  @override
  String get featureValueLocked =>
      'Feature value is locked. Unlock from the main Features dashboard to enable editing';

  @override
  String get adminSaNameRequired =>
      'Please provide a name for the Admin Service Account';

  @override
  String get adminSaGroupsHint =>
      'Assign to some portfolio groups or leave it blank to add them later';

  @override
  String adminSaCreated(String name) {
    return 'Admin Service Account \'$name\' created!';
  }

  @override
  String get createAnotherServiceAccount => 'Create another Service Account';

  @override
  String adminSaAlreadyExists(String name) {
    return 'Service Account with name \'$name\' already exists';
  }

  @override
  String get editAdminSdkServiceAccount => 'Edit admin SDK service account';

  @override
  String get editName => 'Edit name';

  @override
  String get removeOrAddAdminSaToGroup =>
      'Remove Admin Service Account from a group or add a new one';

  @override
  String adminSaUpdated(String name) {
    return 'Admin Service Account $name has been updated';
  }

  @override
  String createApplicationStrategyTitle(String name) {
    return 'Create Application Strategy for $name';
  }

  @override
  String editApplicationStrategyTitle(String name) {
    return 'Edit Application Strategy for $name';
  }

  @override
  String get editApplication => 'Edit application';

  @override
  String get appNameLabel => 'Application name';

  @override
  String get appNameRequired => 'Please enter an application name';

  @override
  String get appNameTooShort =>
      'Application name needs to be at least 4 characters long';

  @override
  String get appDescriptionLabel => 'Application description';

  @override
  String get appDescriptionRequired => 'Please enter app description';

  @override
  String get appDescriptionTooShort =>
      'Application description needs to be at least 4 characters long';

  @override
  String appUpdated(String name) {
    return 'Application $name updated!';
  }

  @override
  String appCreated(String name) {
    return 'Application $name created!';
  }

  @override
  String appAlreadyExists(String name) {
    return 'Application \'$name\' already exists';
  }

  @override
  String get createNewFeatureGroup => 'Create new Feature Group';

  @override
  String get editFeatureGroup => 'Edit Feature Group';

  @override
  String get featureGroupNameLabel => 'Feature group name';

  @override
  String get featureGroupNameRequired => 'Please enter feature group name';

  @override
  String get featureGroupNameTooShort =>
      'Group name needs to be at least 4 characters long';

  @override
  String get featureGroupDescriptionLabel => 'Feature group description';

  @override
  String get featureGroupDescriptionRequired =>
      'Please enter feature group description';

  @override
  String get featureGroupDescriptionTooShort =>
      'Description needs to be at least 4 characters long';

  @override
  String featureGroupUpdated(String name) {
    return 'Feature group $name updated!';
  }

  @override
  String featureGroupCreated(String name) {
    return 'Feature group $name created!';
  }

  @override
  String featureGroupAlreadyExists(String name) {
    return 'Feature group \'$name\' already exists';
  }

  @override
  String get editGroup => 'Edit group';

  @override
  String get groupNameLabel => 'Group name';

  @override
  String get groupNameRequired => 'Please enter a group name';

  @override
  String get groupNameTooShort =>
      'Group name needs to be at least 4 characters long';

  @override
  String groupCreated(String name) {
    return 'Group \'$name\' created!';
  }

  @override
  String groupAlreadyExists(String name) {
    return 'Group \'$name\' already exists';
  }

  @override
  String get groupDeleteContent =>
      'All permissions belonging to this group will be deleted \n\nThis cannot be undone!';

  @override
  String groupDeleted(String name) {
    return 'Group \'$name\' deleted!';
  }

  @override
  String couldNotDeleteGroup(String name) {
    return 'Could not delete group $name';
  }

  @override
  String get viewFeature => 'View feature';

  @override
  String get editFeature => 'Edit feature';

  @override
  String get featureNameLabel => 'Feature name';

  @override
  String get featureNameRequired => 'Please enter feature name';

  @override
  String get featureNameTooShort =>
      'Feature name needs to be at least 4 characters long';

  @override
  String get featureKeyLabel => 'Feature key';

  @override
  String get featureKeyHint => 'To be used in the code with FeatureHub SDK';

  @override
  String get featureKeyRequired => 'Please enter feature key';

  @override
  String get featureKeyNoWhitespace => 'Cannot contain whitespace';

  @override
  String get featureDescriptionLabel => 'Description (optional)';

  @override
  String get featureDescriptionHint => 'Some information about feature';

  @override
  String get featureLinkLabel => 'Reference link (optional)';

  @override
  String get featureLinkHint =>
      'Optional link to external tracking system, e.g. Jira';

  @override
  String get selectFeatureType => 'Select feature type';

  @override
  String featureUpdated(String name) {
    return 'Feature $name updated!';
  }

  @override
  String featureCreated(String name) {
    return 'Feature $name created!';
  }

  @override
  String featureKeyAlreadyExists(String key) {
    return 'Feature with key \'$key\' already exists';
  }

  @override
  String get featureTypeString => 'String';

  @override
  String get featureTypeNumber => 'Number';

  @override
  String get featureTypeBoolean => 'Standard flag (boolean)';

  @override
  String get featureTypeJson => 'Remote configuration (JSON)';

  @override
  String get adminSaResetTokenWarning =>
      'Are you sure you want to reset the access token for this service account?\nThis will invalidate the current token!';

  @override
  String get adminSdkTokenReset => 'Admin SDK access token has been reset';

  @override
  String get adminSdkTokenResetSnackbar =>
      'Admin SDK access token has been reset!';

  @override
  String get unableToResetToken => 'Unable to reset access token';

  @override
  String get resetClientApiKeysWarning =>
      'Are you sure you want to reset ALL client eval API keys for this service account?\nThis will affect the keys across all environments and all applications that this service account has access to!';

  @override
  String get resetServerApiKeysWarning =>
      'Are you sure you want to reset ALL server eval API keys for this service account?\nThis will affect the keys across all environments and all applications that this service account has access to!';

  @override
  String get clientApiKeysReset => '\'Client\' eval API Keys have been reset!';

  @override
  String get serverApiKeysReset => '\'Server\' eval API Keys have been reset!';

  @override
  String get unableToResetApiKey => 'Unable to reset API Key';

  @override
  String viewMetadataFor(String name) {
    return 'View metadata for \'$name\'';
  }

  @override
  String editMetadataFor(String name) {
    return 'Edit metadata for \'$name\'';
  }

  @override
  String get setValue => 'Set value';

  @override
  String featureMetadataUpdated(String name) {
    return 'Feature $name metadata has been updated!';
  }

  @override
  String get setFeatureValue => 'Set feature value';

  @override
  String get addRolloutStrategyTargetingRules =>
      'Add rollout strategy targeting rules';

  @override
  String get splitStrategyName => 'Split strategy name';

  @override
  String get splitStrategyNameExample => 'E.g. 20% rollout';

  @override
  String get strategyNameRequired => 'Strategy name required';

  @override
  String get percentageValue => 'Percentage value';

  @override
  String get percentageValueHelperText =>
      'You can enter a value with up to 4 decimal points, e.g. 0.0005 %';

  @override
  String get percentageValueRequired => 'Percentage value required';

  @override
  String get addPercentageRolloutRule => 'Add percentage rollout rule';

  @override
  String get addPercentage => '+ Percentage';

  @override
  String get percentageTotalOver100Error =>
      'Your percentage total across all rollout values cannot be over 100%. Please enter different value.';

  @override
  String get add => 'Add';
}
