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
  String get featureFilters => 'Feature Filters';

  @override
  String get manageFeatureFilters => 'Manage feature filters';

  @override
  String get featureFiltersDocumentation => 'Feature Filters Documentation';

  @override
  String get createNewFeatureFilter => 'Create New Feature Filter';

  @override
  String get editFeatureFilter => 'Edit Feature Filter';

  @override
  String get noFeatureFiltersFound => 'No filters were found in the portfolio.';

  @override
  String get filterNameLabel => 'Filter name';

  @override
  String get filterDescriptionLabel => 'Filter description';

  @override
  String get filterTableDescriptionLabel => 'Description';

  @override
  String get filterNameRequired => 'Please enter a filter name';

  @override
  String get filterNameTooShort =>
      'Filter name needs to be at least 4 characters long';

  @override
  String get filterDescriptionRequired => 'Please enter a filter description';

  @override
  String get filterDescriptionTooShort =>
      'Filter description needs to be at least 4 characters long';

  @override
  String filterCreated(String name) {
    return 'Feature filter \'$name\' created!';
  }

  @override
  String filterUpdated(String name) {
    return 'Feature filter \'$name\' updated!';
  }

  @override
  String filterDeleted(String name) {
    return 'Feature filter \'$name\' deleted!';
  }

  @override
  String appPlusFeatureCountSingular(String appName, int count) {
    return '$appName ($count feature)';
  }

  @override
  String appPlusFeatureCountPlural(String appName, int count) {
    return '$appName ($count features)';
  }

  @override
  String get filterDeleteContent =>
      'Deleting this filter will remove it from all features and service accounts.\n\nThis cannot be undone!';

  @override
  String get appsUsingFilter => 'Applications/Features';

  @override
  String get saUsingFilter => 'Service Accounts';

  @override
  String get selectFiltersToApply => 'Select filters to apply';

  @override
  String get matchingServiceAccounts => 'Service Accounts using filter';

  @override
  String get matchingFeatures => 'Applications using filter';

  @override
  String get filterByFeatureFilters => 'Filter by feature filters';

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

  @override
  String get filterByFeatureType => 'Filter by feature type';

  @override
  String get searchFeatures => 'Search features';

  @override
  String get createNewStrategy => 'Create new strategy';

  @override
  String get searchStrategy => 'Search strategy';

  @override
  String get columnStrategyName => 'Name';

  @override
  String get columnDateCreated => 'Date created (UTC)';

  @override
  String get columnDateUpdated => 'Date updated (UTC)';

  @override
  String get columnCreatedBy => 'Created by';

  @override
  String get columnUsedIn => 'Used in';

  @override
  String get cannotCreateStrategyNoApps =>
      'Cannot create application strategy as there are no applications in this portfolio';

  @override
  String get appStrategyDeleteContent =>
      'This application strategy will be deleted and unassigned from all the flags. \n\nThis cannot be undone!';

  @override
  String appStrategyDeleted(String name) {
    return 'Application strategy \'$name\' deleted!';
  }

  @override
  String strategyUsage(int envCount, int featureCount) {
    return 'environments: $envCount, feature values: $featureCount';
  }

  @override
  String get addRule => 'Add rule';

  @override
  String get addCustomRule => 'Add custom rule';

  @override
  String get addCustomButton => '+ Custom';

  @override
  String get selectCondition => 'Select condition';

  @override
  String get selectValueType => 'Select value type';

  @override
  String get selectValue => 'Select value';

  @override
  String get selectCountry => 'Select Country';

  @override
  String get selectDevice => 'Select Device';

  @override
  String get selectPlatform => 'Select Platform';

  @override
  String get customKey => 'Custom key';

  @override
  String get customKeyExample => 'e.g. \"warehouse-id\"';

  @override
  String get ruleNameRequired => 'Rule name required';

  @override
  String get deleteRule => 'Delete rule';

  @override
  String get userKeys => 'User key(s)';

  @override
  String get userKeyExample => 'e.g. bob@xyz.com';

  @override
  String get versions => 'Version(s)';

  @override
  String get versionExample => 'e.g. 1.3.4, 7.8.1-SNAPSHOT';

  @override
  String get customValues => 'Custom value(s)';

  @override
  String get customValuesExample => 'e.g. WarehouseA, WarehouseB';

  @override
  String get numbers => 'Number(s)';

  @override
  String get numberExample => 'e.g. 6, 7.87543';

  @override
  String get dates => 'Date(s) - YYYY-MM-DD';

  @override
  String get dateExample => 'e.g. 2017-04-16';

  @override
  String get dateTimes => 'Date/Time(s) - UTC/ISO8601 format';

  @override
  String get dateTimeExample => 'e.g. 2007-03-01T13:00:00Z';

  @override
  String get ipAddresses => 'IP Address(es) with or without CIDR';

  @override
  String get ipAddressExample => 'e.g. 168.192.54.3 or 192.168.86.1/8';

  @override
  String get addValue => 'Add value';

  @override
  String get envOrderUpdated => 'Environment order updated!';

  @override
  String get productionEnvironment => 'Production environment';

  @override
  String deleteProductionEnvWarning(String name) {
    return 'The environment \'$name\' is your production environment, are you sure you wish to remove it?';
  }

  @override
  String envDeleted(String name) {
    return 'Environment \'$name\' deleted!';
  }

  @override
  String envDeleteError(String name) {
    return 'Couldn\'t delete environment $name';
  }

  @override
  String get createNewEnvironment => 'Create new environment';

  @override
  String get editEnvironment => 'Edit environment';

  @override
  String get environmentName => 'Environment name';

  @override
  String get envNameRequired => 'Please enter an environment name';

  @override
  String get envNameTooShort =>
      'Environment name needs to be at least 2 characters long';

  @override
  String get markAsProductionEnvironment => 'Mark as production environment';

  @override
  String envUpdated(String name) {
    return 'Environment $name updated!';
  }

  @override
  String envCreated(String name) {
    return 'Environment $name created!';
  }

  @override
  String envAlreadyExists(String name) {
    return 'Environment with name $name already exists';
  }

  @override
  String get environmentsInfoMessage =>
      'Environments can be ordered by dragging the cards below, showing the deployment promotion order to production (top to bottom). This order will be reflected on the \'Features\' dashboard. It helps your teams see their feature status per environment in the correct order.';

  @override
  String get environmentsDocumentation => 'Environments Documentation';

  @override
  String get group => 'Group';

  @override
  String get goToManageGroupMembers => 'Go to manage group members';

  @override
  String get groupPermissionsDocumentation => 'Group Permissions Documentation';

  @override
  String get selectGroupToEditPermissions =>
      'You need to select a group to edit the permissions for.';

  @override
  String get needToCreateEnvironmentsFirst =>
      'You need to first create some \'Environments\' for this application.';

  @override
  String get setFeatureLevelPermissions => 'Set feature level permissions';

  @override
  String get setAppStrategyPermissions =>
      'Set application strategy permissions';

  @override
  String get setFeatureValuePermissions =>
      'Set feature value level permissions per environment';

  @override
  String get permRead => 'Read';

  @override
  String get permLock => 'Lock';

  @override
  String get permUnlock => 'Unlock';

  @override
  String get permChangeValue => 'Change value / Retire';

  @override
  String get permReadExtendedData => 'Read Extended Feature Data';

  @override
  String noServiceAccountsInPortfolio(String name) {
    return 'There are no service accounts in the \"$name\" portfolio.';
  }

  @override
  String get goToServiceAccountSettings => 'Go to service accounts settings';

  @override
  String get serviceAccount => 'Service account';

  @override
  String get serviceAccountInfoMessage =>
      'We strongly recommend setting production environments with only \'Read\' permission for service accounts. The \'Lock/Unlock\' and \'Change value\' permissions typically given to service accounts for testing purposes, e.g. changing feature values states through the SDK when running tests.';

  @override
  String get selectServiceAccount => 'Select service account';

  @override
  String get setServiceAccountPermissions =>
      'Set the service account access to features for each environment';

  @override
  String serviceAccountUpdated(String name) {
    return 'Service account \'$name\' updated!';
  }

  @override
  String get environmentLabel => 'Environment';

  @override
  String get noEnvironments => 'no environments';

  @override
  String get integrationTypeLabel => 'Integration Type';

  @override
  String get selectWebhookType => 'Select webhook type';

  @override
  String get selectEnvironment => 'Select environment';

  @override
  String get slackChannelSettings => 'Slack Channel Settings (per environment)';

  @override
  String get slackIntegrationDocumentation => 'Slack Integration Documentation';

  @override
  String get enabled => 'Enabled';

  @override
  String get slackChannelId => 'Slack channel ID (leave empty to use default)';

  @override
  String get slackChannelIdExample => 'e.g. C0150T7AF25';

  @override
  String get slackSettingsUpdated => 'Slack settings have been updated';

  @override
  String get messageDeliveryStatus => 'Message delivery status';

  @override
  String get refresh => 'Refresh';

  @override
  String get noActivity => 'There is no activity as yet.';

  @override
  String unacknowledgedRequest(String time) {
    return 'Unacknowledged request sent at $time';
  }

  @override
  String deliveryStatusReceived(String status, String time) {
    return 'Status: $status, received at $time';
  }

  @override
  String deliveryStatusError(String status, String time) {
    return '$status received at $time';
  }

  @override
  String get responseHeaders => 'Response headers:';

  @override
  String get content => 'Content';

  @override
  String get moreRecords => 'More records';

  @override
  String get retry => 'Retry';

  @override
  String get deliveredSuccessfully => 'Successfully delivered';

  @override
  String get undeliverableInfo => 'Undeliverable, some information missing';

  @override
  String get unableToCreateData =>
      'Unable to create the necessary data to send to remote system';

  @override
  String get systemConfigMissing =>
      'Some system configuration is missing to be able to complete';

  @override
  String get remoteSystemError =>
      'Some system error talking to remote system (e.g. system was down)';

  @override
  String get unexpectedResult => 'Unexpected result from remote system';

  @override
  String get networkError => 'Network error, host unknown';

  @override
  String get webhookHistory => 'Webhook History';

  @override
  String get webhookConfiguration => 'Webhook Configuration';

  @override
  String get webhooksDocumentation => 'Webhooks Documentation';

  @override
  String get colType => 'Type';

  @override
  String get colMethod => 'Method';

  @override
  String get colHttpCode => 'HTTP Code';

  @override
  String get colWhenSent => 'When Sent';

  @override
  String get colActions => 'Actions';

  @override
  String get webhookWhenSent => 'When sent';

  @override
  String get webhookCloudEventType => 'Webhook Cloud Event type';

  @override
  String get webhookUrl => 'URL';

  @override
  String get webhookDetailMethod => 'Method';

  @override
  String get webhookHttpStatus => 'HTTP status';

  @override
  String get cloudEventType => 'Cloud Event type';

  @override
  String get incomingHeaders => 'Incoming headers';

  @override
  String get outgoingHeaders => 'Outgoing headers';

  @override
  String get webhookContent => 'Webhook Content';

  @override
  String get copyContent => 'Copy Content';

  @override
  String get searchServiceAccounts => 'Search Service Accounts';

  @override
  String get colName => 'Name';

  @override
  String get colGroups => 'Groups';

  @override
  String get resetAdminSdkToken => 'Reset Admin SDK access token';

  @override
  String get adminSADeleteContent =>
      'This service account will be removed from all groups and deleted from the organization. \n\nThis cannot be undone!';

  @override
  String adminSADeleted(String name) {
    return 'Service account \'$name\' deleted!';
  }

  @override
  String get adminSADetailsTitle => 'Admin Service Account details';

  @override
  String get accessToken => 'Access token';

  @override
  String get copyAccessToken => 'Copy access token to clipboard';

  @override
  String get accessTokenSecurityNote =>
      'For security, you will not be able to view the access token once you close this window.';

  @override
  String get systemConfigurationsTitle => 'System Configurations';

  @override
  String get siteConfigurationTitle => 'Site Configuration';

  @override
  String get siteConfigurationSubtitle => 'Configure your FeatureHub system';

  @override
  String get slackConfigurationTitle => 'Slack Configuration';

  @override
  String get slackConfigurationSubtitle =>
      'Enable FeatureHub to send Slack messages';

  @override
  String get encryptionRequiredForSlack =>
      'You are required to configure encryption key/password in the FeatureHub system properties file to enable Slack integration';

  @override
  String get encryptionDocumentation => 'Encryption documentation';

  @override
  String get siteUrlLabel => 'The URL of your organisation\'s FeatureHub app';

  @override
  String get siteUrlEmptyError => 'You cannot specify an empty url';

  @override
  String get siteUrlInvalidError =>
      'You must specify a valid url for your site';

  @override
  String get allowSearchRobots => 'Allow search robots to index';

  @override
  String get redirectBadHostsHeader => 'Redirect traffic with bad Hosts header';

  @override
  String get enableSlack => 'Enable Slack';

  @override
  String get connectFeatureHubToSlack => 'Connect FeatureHub to Slack';

  @override
  String get installFeatureHubBot =>
      'Install FeatureHub Bot app to your Slack workspace';

  @override
  String get connectToSlack => 'Connect to Slack';

  @override
  String get slackBotTokenLabel => 'Slack Bot User OAuth Token';

  @override
  String get slackBotTokenRequired => 'Please enter Slack Bot User OAuth token';

  @override
  String get defaultSlackChannelIdLabel => 'Default Slack channel ID';

  @override
  String get slackChannelIdRequired => 'Please enter Slack channel ID';

  @override
  String get externalSlackDeliveryMessage =>
      'If your Slack delivery is offloaded to an external application, please specify the details here.';

  @override
  String externalSlackDeliveryUrlLabel(String prefixes) {
    return 'External Slack message delivery service (optional, valid prefixes $prefixes)';
  }

  @override
  String get invalidUrlPrefix => 'You must choose a valid URL prefix';

  @override
  String get clickToEdit => 'Click to edit';

  @override
  String get showAction => 'Show';

  @override
  String get clearAction => 'Clear';

  @override
  String get decryptAction => 'Decrypt';

  @override
  String get encryptAction => 'Encrypt';

  @override
  String addRowButton(String name) {
    return 'Add $name';
  }

  @override
  String get headerColumnLabel => 'Header';

  @override
  String get valueColumnLabel => 'Value';

  @override
  String get selectEnvironmentsToDisplay => 'Select environments to display';

  @override
  String appThingLabel(String name) {
    return 'application \'$name\'';
  }

  @override
  String appDeleted(String name) {
    return 'Application \'$name\' deleted!';
  }

  @override
  String appDeleteError(String name) {
    return 'Couldn\'t delete application $name';
  }

  @override
  String get undelivered => 'undelivered';

  @override
  String get webhookDetailsTitle => 'Webhook details';

  @override
  String get viewWebhookDetails => 'View webhook details';

  @override
  String get enableJsonValidation => 'Enable JSON validation';

  @override
  String get formatJson => 'Format json';

  @override
  String get jsonValue => 'JSON Value';

  @override
  String get errorNotFound => 'The requested resource was not found';

  @override
  String get errorForbidden =>
      'You do not have permission to access this resource';

  @override
  String get errorInternalServer => 'An internal server error occurred';

  @override
  String get errorLoadingData => 'An error occurred while loading the data';

  @override
  String get yourCurrentPortfolio => 'Your current portfolio';

  @override
  String get selectPortfolio => 'Select portfolio';

  @override
  String get selectApplication => 'Select application';

  @override
  String get featureOn => 'ON';

  @override
  String get featureOff => 'OFF';

  @override
  String get notSet => 'not set';

  @override
  String get retired => 'Retired';

  @override
  String featureThingLabel(String name) {
    return 'feature \'$name\'';
  }

  @override
  String get featureDeleteContent =>
      'You need to make sure all your code is cleaned up and can deal without this feature!\n\nThis cannot be undone!';

  @override
  String featureDeleted(String name) {
    return 'Feature \'$name\' deleted!';
  }

  @override
  String get noPermissionsForOperation =>
      'You don\'t have permissions to perform this operation';

  @override
  String featureDeleteError(String name) {
    return 'Couldn\'t delete feature $name';
  }

  @override
  String get featureGroupDeleteContent =>
      'This action will delete a feature group and a strategy associated with it.\n\nThe features will not be deleted and remain present in your system.\n\nThis cannot be undone!';

  @override
  String featureGroupDeleted(String name) {
    return 'Feature group \'$name\' deleted!';
  }

  @override
  String featureGroupDeleteError(String name) {
    return 'Couldn\'t delete feature group $name';
  }

  @override
  String get noEnvironmentsAvailable => 'No environments available';

  @override
  String get selectFeatureToAdd => 'Select feature to add';

  @override
  String get setupWelcomeTitle => 'Lets get this party started!';

  @override
  String get setupWelcomeMessage =>
      'Well done, FeatureHub is up and running.  You\'ll be the first \'Organization super admin\' of your FeatureHub account.';

  @override
  String get setupOrRegisterBelow =>
      'or register by providing the details below';

  @override
  String get next => 'Next';

  @override
  String get setupAllSet => 'All set!';

  @override
  String get setupNextStepsMessage =>
      'Next step is to create your first application and add some features. Your first environment called \"Production\" will be created by default. You can follow the \"Quick Setup\" helper by clicking the \"rocket\" icon on the right of the app bar to see your progress.';

  @override
  String get stepperTitle => 'Application setup progress';

  @override
  String get stepCreateApplication => 'Create application';

  @override
  String get stepSelectApplicationHint =>
      'Select application or create a new one by following the link below';

  @override
  String get goToApplications => 'Go to Applications';

  @override
  String get stepCreateTeamGroup => 'Create team group';

  @override
  String get stepCreateTeamGroupHint =>
      'Groups are portfolio-wide, we recommend creating application specific groups eg \"MyApp developers\"';

  @override
  String get goToGroups => 'Go to Groups';

  @override
  String get stepCreateServiceAccount => 'Create service account';

  @override
  String get stepCreateServiceAccountHint =>
      'Service accounts are portfolio-wide, we recommend creating at least two service accounts specific to an application, e.g. \"SA-MyApp-Prod\" and \"SA-MyApp-Non-Prod\"';

  @override
  String get goToServiceAccounts => 'Go to Service Accounts';

  @override
  String get stepCreateEnvironment => 'Create environment';

  @override
  String get stepCreateEnvironmentHint =>
      'Create an environment for selected application, e.g. \"test\", \"dev\", \"prod\"';

  @override
  String get goToEnvironments => 'Go to Environments';

  @override
  String get stepGiveAccessToGroups => 'Give access to groups';

  @override
  String get stepGiveAccessToGroupsHint =>
      'Assign an application environment level permissions to a group of users';

  @override
  String get goToGroupPermissions => 'Go to Group Permissions';

  @override
  String get stepGiveAccessToServiceAccount => 'Give access to service account';

  @override
  String get stepGiveAccessToServiceAccountHint =>
      'Assign an application environment level permissions to a service account';

  @override
  String get goToSAPermissions => 'Go to SA Permissions';

  @override
  String get stepCreateFeature => 'Create a feature';

  @override
  String get stepCreateFeatureHint => 'Create a feature for an application';

  @override
  String get goToFeatures => 'Go to Features';

  @override
  String get featureHubAdministrators => 'FeatureHub Administrators';

  @override
  String get portfolioLabel => 'Portfolio';

  @override
  String get andOperator => 'AND';

  @override
  String systemConfigUpdated(String section) {
    return '$section was successfully updated';
  }

  @override
  String systemConfigNoUpdates(String section) {
    return 'No updates for $section found';
  }

  @override
  String get unableToSaveUpdates => 'Unable to save updates';

  @override
  String get showValue => 'Show value';

  @override
  String get strategyNameTooLong => 'Strategy name is too long';

  @override
  String get strategyEmptyMatchCriteria =>
      'You have not provided any rules to match against, please add a rule';

  @override
  String get strategyNegativePercentage =>
      'Percentage cannot be a negative number';

  @override
  String get strategyPercentageOver100 =>
      'The total percentage value across all strategies is above 100%, please decrease the percentage rule';

  @override
  String get strategyArrayAttributeNoValues =>
      'Please provide at least one value for this rule';

  @override
  String get strategyAttrInvalidWellKnownEnum =>
      'Please select a value for this rule';

  @override
  String get strategyAttrMissingValue => 'Please provide a value for this rule';

  @override
  String get strategyAttrMissingConditional =>
      'Please select a matching condition for this rule';

  @override
  String get strategyAttrMissingFieldName => 'Please enter the rule name';

  @override
  String get strategyAttrMissingFieldType =>
      'Please select a value type for this rule';

  @override
  String get strategyAttrValNotSemanticVersion =>
      'Please provide a valid semantic version';

  @override
  String get strategyAttrValNotNumber => 'Please provide a valid number';

  @override
  String get strategyAttrValNotDate =>
      'Please provide a valid date in YYYY-MM-DD format';

  @override
  String get strategyAttrValNotDateTime =>
      'Please provide a valid date and time in YYYY-MM-DDTHH:MM:SS format';

  @override
  String get strategyAttrValNotCidr =>
      'Please provide a valid IP or CIDR address';

  @override
  String get strategyAttrUnknownFailure =>
      'There was an unknown strategy validation error';

  @override
  String get strategyDefault => 'default';

  @override
  String get strategyServe => 'serve';

  @override
  String get editStrategySettings => 'Edit Strategy Settings';

  @override
  String get selectStrategyToAdd => 'Select strategy to add';

  @override
  String get wellKnownCountry => 'Country';

  @override
  String get wellKnownDevice => 'Device';

  @override
  String get wellKnownPlatform => 'Platform';

  @override
  String get wellKnownVersion => 'Version';

  @override
  String get wellKnownUserKey => 'User Key';

  @override
  String get tooltipAppliedRules => 'Applied rules';

  @override
  String tooltipPercentage(String value) {
    return 'Percentage: $value%';
  }

  @override
  String get tooltipUserKey => 'User key';

  @override
  String get tooltipCountry => 'Country';

  @override
  String get tooltipPlatform => 'Platform';

  @override
  String get tooltipDevice => 'Device';

  @override
  String get tooltipVersion => 'Version';

  @override
  String get tooltipCustom => 'Custom';

  @override
  String get unsavedChanges => 'You have unsaved changes, save?';

  @override
  String featureValueUpdated(String feature, String environment) {
    return 'Feature $feature in the environment $environment has been updated!';
  }

  @override
  String get defaultValue => 'Default value';

  @override
  String get strategyVariations => 'Strategy variations';

  @override
  String get strategyVariationsInfo =>
      'Add a strategy variation to serve a value other than default. You can change strategies evaluation order by dragging and dropping the cards below. Strategies are evaluated in order from top to bottom. Evaluation stops when it hits a matching strategy. \'Group Strategy\' evaluation comes last. If no strategies match, then \'default\' feature value is served.';

  @override
  String get noStrategiesSet => 'No strategies set';

  @override
  String get groupStrategyVariations => 'Group strategy variations';

  @override
  String get groupStrategyVariationsInfo =>
      'Feature groups are recommended when you want to set the same strategy for multiple features in the same environment. Feature group strategy can be created and edited from the Feature Groups page.';

  @override
  String get noGroupStrategiesSet => 'No group strategies set';

  @override
  String get applicationStrategyVariations => 'Application strategy variations';

  @override
  String get applicationStrategyVariationsInfo =>
      'Application strategies are created at application level and can be assigned to multiple features in any environment. Application strategy can be created and edited from the Application Strategies page.';

  @override
  String get noApplicationStrategiesSet => 'No application strategies set';

  @override
  String get showAvailableAppStrategies => 'Show available app strategies';

  @override
  String get addStrategy => 'Add Strategy';

  @override
  String get retiredStatus => 'Retired status';

  @override
  String get retiredStatusInfo =>
      'When feature flag is not needed any longer in your application, and ready to be removed, you can first \'retire\' this feature in a given environment to test how your application behaves. This means that the feature won\'t be visible by the SDKs, imitating the \'deleted\' state. You can uncheck the box to \'un-retire\' a feature if you change your mind as this operation is reversible. Once you retire feature values across all the environments and test that your application behaves as expected, you can delete your entire feature.';

  @override
  String get hideHistory => 'Hide history';

  @override
  String get showHistory => 'Show history';

  @override
  String get showingLast20 => 'Showing last 20';

  @override
  String get historyColumnTimestamp => 'Timestamp (UTC)';

  @override
  String get historyColumnName => 'Name';

  @override
  String get historyColumnEmail => 'Email';

  @override
  String get historyColumnType => 'Type';

  @override
  String get historyColumnDefaultValue => 'Default Value';

  @override
  String get historyColumnLocked => 'Locked';

  @override
  String get historyColumnRetired => 'Retired';

  @override
  String get historyColumnRolloutStrategies => 'Rollout Strategies';

  @override
  String get historyTypeUser => 'User';

  @override
  String get historyTypeServiceAccount => 'Service Account';

  @override
  String get strategyRules => 'Strategy Rules';

  @override
  String get percentageRollout => 'Percentage Rollout';

  @override
  String get moreDetails => 'more';

  @override
  String get groupStrategyTooltip => 'Group Strategy';

  @override
  String get applicationStrategyTooltip => 'Application Strategy';

  @override
  String get lockedStatus => 'Locked status';

  @override
  String get lockedStatusInfo =>
      'Locking mechanism provides an additional safety for feature changes when deploying incomplete code to production. Locked status prevents any changes to default value, strategies, strategy values and \'retired\' status. Typically, developers keep features locked to indicate they are not ready to be turned on for testers, product owners, customers and other stakeholders.';

  @override
  String get clickToUnlock => 'Click to unlock';

  @override
  String get clickToLock => 'Click to lock';

  @override
  String get featureIsLocked => 'Feature is locked and cannot be changed';

  @override
  String get featureIsUnlocked => 'Feature is unlocked and can be changed';

  @override
  String get featuresColumnHeader => 'Features';

  @override
  String get noFeaturesToDisplay => 'No features to display';

  @override
  String get noEnvironmentsForApp =>
      'Either there are no environments defined for this application or you don\'t have permissions to access any of them';

  @override
  String get goToEnvironmentsSettings => 'Go to environments settings';

  @override
  String get noFeaturesForApp =>
      'There are no features defined for this application';

  @override
  String get setAsOrgSuperAdmin => 'Set this user as organization super admin';

  @override
  String get copyFeatureKeyToClipboard => 'Copy feature key to clipboard';

  @override
  String get editDetails => 'Edit details';

  @override
  String get viewDetails => 'View details';

  @override
  String get editMetadata => 'Edit metadata';

  @override
  String get viewMetadata => 'View metadata';

  @override
  String get manageGroup => 'Manage Group';

  @override
  String get strategy => 'Strategy';
}
