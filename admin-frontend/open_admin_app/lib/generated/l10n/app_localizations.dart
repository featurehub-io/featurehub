import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('zh')
  ];

  /// Title on the sign-in screen
  ///
  /// In en, this message translates to:
  /// **'Sign in to FeatureHub'**
  String get signInTitle;

  /// Divider text between SSO and local sign-in
  ///
  /// In en, this message translates to:
  /// **'or sign in with a username and password'**
  String get signInWithCredentials;

  /// Label for the email input field
  ///
  /// In en, this message translates to:
  /// **'Email address'**
  String get emailLabel;

  /// Validation message when email is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter your email'**
  String get emailRequired;

  /// Label for the password input field
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get passwordLabel;

  /// Validation message when password is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter your password'**
  String get passwordRequired;

  /// Error shown when credentials are wrong
  ///
  /// In en, this message translates to:
  /// **'Incorrect email address or password'**
  String get incorrectCredentials;

  /// Sign-in submit button label
  ///
  /// In en, this message translates to:
  /// **'Sign in'**
  String get signInButton;

  /// Tooltip for the dark mode toggle button
  ///
  /// In en, this message translates to:
  /// **'Dark mode'**
  String get darkMode;

  /// Tooltip for the light mode toggle button
  ///
  /// In en, this message translates to:
  /// **'Light mode'**
  String get lightMode;

  /// Tooltip for the sign-out button
  ///
  /// In en, this message translates to:
  /// **'Sign out'**
  String get signOut;

  /// Drawer section heading for application settings
  ///
  /// In en, this message translates to:
  /// **'Application Settings'**
  String get applicationSettings;

  /// Drawer section heading for portfolio settings
  ///
  /// In en, this message translates to:
  /// **'Portfolio Settings'**
  String get portfolioSettings;

  /// Drawer section heading for organization settings
  ///
  /// In en, this message translates to:
  /// **'Organization Settings'**
  String get organizationSettings;

  /// Navigation menu item for portfolios
  ///
  /// In en, this message translates to:
  /// **'Portfolios'**
  String get portfolios;

  /// Navigation menu item for users
  ///
  /// In en, this message translates to:
  /// **'Users'**
  String get users;

  /// Navigation menu item for admin service accounts
  ///
  /// In en, this message translates to:
  /// **'Admin Service Accounts'**
  String get adminServiceAccounts;

  /// Navigation menu item for system configuration
  ///
  /// In en, this message translates to:
  /// **'System Config'**
  String get systemConfig;

  /// Navigation menu item for groups
  ///
  /// In en, this message translates to:
  /// **'Groups'**
  String get groups;

  /// Navigation menu item for service accounts
  ///
  /// In en, this message translates to:
  /// **'Service Accounts'**
  String get serviceAccounts;

  /// Navigation menu item for environments
  ///
  /// In en, this message translates to:
  /// **'Environments'**
  String get environments;

  /// Navigation menu item for group permissions
  ///
  /// In en, this message translates to:
  /// **'Group permissions'**
  String get groupPermissions;

  /// Navigation menu item for service account permissions
  ///
  /// In en, this message translates to:
  /// **'Service account permissions'**
  String get serviceAccountPermissions;

  /// Navigation menu item for integrations
  ///
  /// In en, this message translates to:
  /// **'Integrations'**
  String get integrations;

  /// Navigation menu item for applications
  ///
  /// In en, this message translates to:
  /// **'Applications'**
  String get applications;

  /// Navigation menu item for features
  ///
  /// In en, this message translates to:
  /// **'Features'**
  String get features;

  /// Navigation menu item for feature groups
  ///
  /// In en, this message translates to:
  /// **'Feature Groups'**
  String get featureGroups;

  /// Navigation menu item for application strategies
  ///
  /// In en, this message translates to:
  /// **'Application Strategies'**
  String get applicationStrategies;

  /// Navigation menu item for API keys
  ///
  /// In en, this message translates to:
  /// **'API Keys'**
  String get apiKeys;

  /// Confirmation dialog title when deleting something
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to delete the {thing}?'**
  String deleteConfirmTitle(String thing);

  /// Warning content in delete confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'This cannot be undone!'**
  String get cannotBeUndone;

  /// Cancel button label
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// Delete button label
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get delete;

  /// Reset button label
  ///
  /// In en, this message translates to:
  /// **'Reset'**
  String get reset;

  /// OK button label
  ///
  /// In en, this message translates to:
  /// **'OK'**
  String get ok;

  /// Edit action label
  ///
  /// In en, this message translates to:
  /// **'Edit'**
  String get edit;

  /// Tooltip for documentation links
  ///
  /// In en, this message translates to:
  /// **'View documentation'**
  String get viewDocumentation;

  /// Button to create a new application
  ///
  /// In en, this message translates to:
  /// **'Create new application'**
  String get createNewApplication;

  /// Label for applications docs link
  ///
  /// In en, this message translates to:
  /// **'Applications Documentation'**
  String get applicationsDocumentation;

  /// Button to republish portfolio cache
  ///
  /// In en, this message translates to:
  /// **'Republish portfolio cache'**
  String get republishPortfolioCache;

  /// Title of republish cache confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'Warning: Intensive system operation'**
  String get republishPortfolioCacheWarningTitle;

  /// Body of republish cache confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to republish this entire portfolio\'s cache?'**
  String get republishPortfolioCacheWarningContent;

  /// Tooltip label for feature flags count
  ///
  /// In en, this message translates to:
  /// **'Feature flags'**
  String get featureFlags;

  /// Tooltip for the app card popup menu
  ///
  /// In en, this message translates to:
  /// **'Show more'**
  String get showMore;

  /// Popup menu item to republish cache for a single app
  ///
  /// In en, this message translates to:
  /// **'Republish cache for this app'**
  String get republishCacheForApp;

  /// Page header for the manage users screen
  ///
  /// In en, this message translates to:
  /// **'Manage users'**
  String get manageUsers;

  /// Label for manage users docs link
  ///
  /// In en, this message translates to:
  /// **'Manage Users Documentation'**
  String get manageUsersDocumentation;

  /// Button to create a new user
  ///
  /// In en, this message translates to:
  /// **'Create new user'**
  String get createNewUser;

  /// Hint text in the users search field
  ///
  /// In en, this message translates to:
  /// **'Search users'**
  String get searchUsers;

  /// Table column header: name
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get columnName;

  /// Table column header: status
  ///
  /// In en, this message translates to:
  /// **'Status'**
  String get columnStatus;

  /// Table column header: email
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get columnEmail;

  /// Table column header: last sign-in time
  ///
  /// In en, this message translates to:
  /// **'Last sign in (UTC)'**
  String get columnLastSignIn;

  /// Table column header: actions
  ///
  /// In en, this message translates to:
  /// **'Actions'**
  String get columnActions;

  /// Shown in the name cell when a user hasn't completed registration
  ///
  /// In en, this message translates to:
  /// **'Not yet registered'**
  String get notYetRegistered;

  /// User status: active
  ///
  /// In en, this message translates to:
  /// **'active'**
  String get statusActive;

  /// User status: deactivated
  ///
  /// In en, this message translates to:
  /// **'deactivated'**
  String get statusDeactivated;

  /// Tooltip on the activate-user icon button
  ///
  /// In en, this message translates to:
  /// **'Activate user'**
  String get activateUserTooltip;

  /// Dialog title when activating a user
  ///
  /// In en, this message translates to:
  /// **'Activate user \'{name}\''**
  String activateUserTitle(String name);

  /// Dialog body when activating a user
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to activate user with email address {email}?'**
  String activateUserConfirm(String email);

  /// Activate button label
  ///
  /// In en, this message translates to:
  /// **'Activate'**
  String get activate;

  /// Snackbar message after activating a user
  ///
  /// In en, this message translates to:
  /// **'User \'{name}\' activated!'**
  String userActivated(String name);

  /// Snackbar message after deactivating a user
  ///
  /// In en, this message translates to:
  /// **'User \'{name}\' deactivated!'**
  String userDeactivated(String name);

  /// Title of the user info dialog
  ///
  /// In en, this message translates to:
  /// **'User information'**
  String get userInformation;

  /// Label for the registration URL in user info dialog
  ///
  /// In en, this message translates to:
  /// **'Registration URL'**
  String get registrationUrl;

  /// Tooltip for the copy-registration-URL button
  ///
  /// In en, this message translates to:
  /// **'Copy URL to Clipboard'**
  String get copyUrlToClipboard;

  /// Label shown when a registration token has expired
  ///
  /// In en, this message translates to:
  /// **'Registration expired'**
  String get registrationExpired;

  /// Button to renew an expired registration token
  ///
  /// In en, this message translates to:
  /// **'Renew registration URL and copy to clipboard'**
  String get renewRegistrationUrl;

  /// Snackbar after renewing registration URL
  ///
  /// In en, this message translates to:
  /// **'Registration URL renewed and copied to clipboard'**
  String get registrationUrlRenewed;

  /// Dialog title when trying to delete own account
  ///
  /// In en, this message translates to:
  /// **'You can\'t delete yourself!'**
  String get cantDeleteYourself;

  /// Dialog body when trying to delete own account
  ///
  /// In en, this message translates to:
  /// **'To delete yourself from the organization, you\'ll need to contact a site administrator.'**
  String get cantDeleteYourselfContent;

  /// Body of the delete-user confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'This user will be removed from all groups and deactivated in this organization.'**
  String get deleteUserContent;

  /// Page header for the portfolios screen
  ///
  /// In en, this message translates to:
  /// **'Manage portfolios'**
  String get managePortfolios;

  /// Label for portfolios docs link
  ///
  /// In en, this message translates to:
  /// **'Manage Portfolios Documentation'**
  String get managePortfoliosDocumentation;

  /// Button to create a new portfolio
  ///
  /// In en, this message translates to:
  /// **'Create new portfolio'**
  String get createNewPortfolio;

  /// Hint text in the portfolio search field
  ///
  /// In en, this message translates to:
  /// **'Search portfolios'**
  String get searchPortfolios;

  /// Button to republish the entire system cache
  ///
  /// In en, this message translates to:
  /// **'Republish system cache'**
  String get republishSystemCache;

  /// Body of republish entire cache confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to republish the entire cache?'**
  String get republishEntireCacheWarningContent;

  /// Page header for the group members screen
  ///
  /// In en, this message translates to:
  /// **'Manage group members'**
  String get manageGroupMembers;

  /// Label for user groups docs link
  ///
  /// In en, this message translates to:
  /// **'User Groups Documentation'**
  String get userGroupsDocumentation;

  /// Loading text while groups are being fetched
  ///
  /// In en, this message translates to:
  /// **'Fetching Groups...'**
  String get fetchingGroups;

  /// Button to create a new group
  ///
  /// In en, this message translates to:
  /// **'Create new group'**
  String get createNewGroup;

  /// Button to open the add-members dialog
  ///
  /// In en, this message translates to:
  /// **'Add members'**
  String get addMembers;

  /// Table column header for member type
  ///
  /// In en, this message translates to:
  /// **'Type (User or Admin Service Account)'**
  String get columnMemberType;

  /// Member type label: regular user
  ///
  /// In en, this message translates to:
  /// **'User'**
  String get memberTypeUser;

  /// Member type label: service account
  ///
  /// In en, this message translates to:
  /// **'Service Account'**
  String get memberTypeServiceAccount;

  /// Tooltip on the remove-member icon button
  ///
  /// In en, this message translates to:
  /// **'Remove from group'**
  String get removeFromGroup;

  /// Snackbar after removing a member from a group
  ///
  /// In en, this message translates to:
  /// **'\'{name}\' removed from group \'{groupName}\''**
  String memberRemovedFromGroup(String name, String groupName);

  /// Empty state when no groups exist in the portfolio
  ///
  /// In en, this message translates to:
  /// **'No groups found in the portfolio'**
  String get noGroupsFound;

  /// Label above the group dropdown
  ///
  /// In en, this message translates to:
  /// **'Portfolio groups'**
  String get portfolioGroups;

  /// Hint text in the group dropdown
  ///
  /// In en, this message translates to:
  /// **'Select group'**
  String get selectGroup;

  /// Title of the add-members dialog
  ///
  /// In en, this message translates to:
  /// **'Add members to group {groupName}'**
  String addMembersToGroupTitle(String groupName);

  /// Label in the members chip input field
  ///
  /// In en, this message translates to:
  /// **'Enter members to add to group...'**
  String get enterMembersToAdd;

  /// Submit button in the add-members dialog
  ///
  /// In en, this message translates to:
  /// **'Add to group'**
  String get addToGroup;

  /// Snackbar after a group is updated
  ///
  /// In en, this message translates to:
  /// **'Group \'{name}\' updated!'**
  String groupUpdated(String name);

  /// Page header for the service accounts screen
  ///
  /// In en, this message translates to:
  /// **'Manage service accounts'**
  String get manageServiceAccounts;

  /// Label for service accounts docs link
  ///
  /// In en, this message translates to:
  /// **'Service Accounts Documentation'**
  String get serviceAccountsDocumentation;

  /// Button to create a new service account
  ///
  /// In en, this message translates to:
  /// **'Create new service account'**
  String get createNewServiceAccount;

  /// Label on service account environment card when it has access
  ///
  /// In en, this message translates to:
  /// **'The service account has permissions to one or more environments in this application'**
  String get saHasPermissions;

  /// Label on service account environment card when it has no access
  ///
  /// In en, this message translates to:
  /// **'The service account has no permissions to any environments in this application'**
  String get saHasNoPermissions;

  /// Button on service account environment card when access exists
  ///
  /// In en, this message translates to:
  /// **'Change access'**
  String get changeAccess;

  /// Button on service account environment card when no access
  ///
  /// In en, this message translates to:
  /// **'Add access'**
  String get addAccess;

  /// Body of delete service account confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'All applications using this service account will no longer have access to features!\n\nThis cannot be undone!'**
  String get saDeleteContent;

  /// Snackbar after a service account is deleted
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' deleted!'**
  String saDeleted(String name);

  /// Error title when deleting a service account fails
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t delete service account {name}'**
  String saDeleteError(String name);

  /// Dialog title when editing a service account
  ///
  /// In en, this message translates to:
  /// **'Edit service account'**
  String get editServiceAccount;

  /// Label for the service account name field
  ///
  /// In en, this message translates to:
  /// **'Service account name'**
  String get saNameLabel;

  /// Validation: name field empty
  ///
  /// In en, this message translates to:
  /// **'Please enter a service account name'**
  String get saNameRequired;

  /// Validation: name too short
  ///
  /// In en, this message translates to:
  /// **'Service account name needs to be at least 4 characters long'**
  String get saNameTooShort;

  /// Label for the service account description field
  ///
  /// In en, this message translates to:
  /// **'Service account description'**
  String get saDescriptionLabel;

  /// Validation: description field empty
  ///
  /// In en, this message translates to:
  /// **'Please enter service account description'**
  String get saDescriptionRequired;

  /// Validation: description too short
  ///
  /// In en, this message translates to:
  /// **'Service account description needs to be at least 4 characters long'**
  String get saDescriptionTooShort;

  /// Update button label
  ///
  /// In en, this message translates to:
  /// **'Update'**
  String get update;

  /// Create button label
  ///
  /// In en, this message translates to:
  /// **'Create'**
  String get create;

  /// Snackbar after a service account is updated
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' updated!'**
  String saUpdated(String name);

  /// Snackbar after a service account is created
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' created!'**
  String saCreated(String name);

  /// Error when creating a service account that already exists
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' already exists'**
  String saAlreadyExists(String name);

  /// Tooltip for the reset client API keys button
  ///
  /// In en, this message translates to:
  /// **'Reset client eval API keys'**
  String get resetClientApiKeys;

  /// Tooltip for the reset server API keys button
  ///
  /// In en, this message translates to:
  /// **'Reset server eval API keys'**
  String get resetServerApiKeys;

  /// Page header for the features dashboard
  ///
  /// In en, this message translates to:
  /// **'Features console'**
  String get featuresConsole;

  /// Label for features docs link
  ///
  /// In en, this message translates to:
  /// **'Features Documentation'**
  String get featuresDocumentation;

  /// Button to create a new feature flag
  ///
  /// In en, this message translates to:
  /// **'Create New Feature'**
  String get createNewFeature;

  /// Empty state when no applications exist in the portfolio
  ///
  /// In en, this message translates to:
  /// **'There are no applications in this portfolio'**
  String get noApplicationsInPortfolio;

  /// Message shown when user has no access to any applications
  ///
  /// In en, this message translates to:
  /// **'Either there are no applications in this portfolio or you don\'t have access to any of the applications.\nPlease contact your administrator.'**
  String get noApplicationsAccessMessage;

  /// Label for API keys docs link
  ///
  /// In en, this message translates to:
  /// **'API Keys Documentation'**
  String get apiKeysDocumentation;

  /// Link to navigate to service accounts settings
  ///
  /// In en, this message translates to:
  /// **'Go to service accounts settings'**
  String get goToServiceAccountsSettings;

  /// Empty state when no service accounts exist
  ///
  /// In en, this message translates to:
  /// **'No service accounts available'**
  String get noServiceAccountsAvailable;

  /// Column header for permissions
  ///
  /// In en, this message translates to:
  /// **'Permissions'**
  String get permissions;

  /// Column header for client and server API keys
  ///
  /// In en, this message translates to:
  /// **'Client & Server API Keys'**
  String get clientServerApiKeys;

  /// Label for the client evaluation API key
  ///
  /// In en, this message translates to:
  /// **'Client eval API Key'**
  String get clientEvalApiKey;

  /// Label for the server evaluation API key
  ///
  /// In en, this message translates to:
  /// **'Server eval API Key'**
  String get serverEvalApiKey;

  /// Shown when a service account has no permissions for an environment
  ///
  /// In en, this message translates to:
  /// **'No permissions defined'**
  String get noPermissionsDefined;

  /// Tooltip on the error icon when an API key is unavailable
  ///
  /// In en, this message translates to:
  /// **'API Key is unavailable because your current permissions for this environment are lower level'**
  String get apiKeyUnavailable;

  /// Page header for admin SDK service accounts
  ///
  /// In en, this message translates to:
  /// **'Manage admin SDK service accounts'**
  String get manageAdminSdkServiceAccounts;

  /// Label for admin service accounts docs link
  ///
  /// In en, this message translates to:
  /// **'Admin Service Accounts Documentation'**
  String get adminServiceAccountsDocumentation;

  /// Button to create an admin service account
  ///
  /// In en, this message translates to:
  /// **'Create Admin Service Account'**
  String get createAdminServiceAccount;

  /// Instruction text on the create user form
  ///
  /// In en, this message translates to:
  /// **'To create a new user please first provide their email address'**
  String get createUserInstructions;

  /// Validation message for invalid email
  ///
  /// In en, this message translates to:
  /// **'Please enter a valid email address'**
  String get invalidEmailAddress;

  /// Hint text for the portfolio group selector on create user form
  ///
  /// In en, this message translates to:
  /// **'Add user to some portfolio groups or leave it blank to add them later'**
  String get addUserToGroupsHint;

  /// Success heading after creating a user
  ///
  /// In en, this message translates to:
  /// **'User created!'**
  String get userCreated;

  /// Instruction shown after user creation with local auth
  ///
  /// In en, this message translates to:
  /// **'You will need to email this URL to the new user, so they can complete their registration and set their password.'**
  String get sendRegistrationUrlInstructions;

  /// Instruction shown after user creation with SSO auth
  ///
  /// In en, this message translates to:
  /// **'The user can now sign in and they will be able to access the system.'**
  String get userCanSignIn;

  /// Close button label
  ///
  /// In en, this message translates to:
  /// **'Close'**
  String get close;

  /// Button to go back and create another user
  ///
  /// In en, this message translates to:
  /// **'Create another user'**
  String get createAnotherUser;

  /// Error when creating a user whose email already exists
  ///
  /// In en, this message translates to:
  /// **'User with email \'{email}\' already exists'**
  String userEmailAlreadyExists(String email);

  /// Page header for the application settings screen
  ///
  /// In en, this message translates to:
  /// **'Application settings'**
  String get appSettingsTitle;

  /// Tab label for environments
  ///
  /// In en, this message translates to:
  /// **'Environments'**
  String get tabEnvironments;

  /// Tab label for group permissions
  ///
  /// In en, this message translates to:
  /// **'Group Permissions'**
  String get tabGroupPermissions;

  /// Tab label for service account permissions
  ///
  /// In en, this message translates to:
  /// **'Service Account Permissions'**
  String get tabServiceAccountPermissions;

  /// Tab label for integrations
  ///
  /// In en, this message translates to:
  /// **'Integrations'**
  String get tabIntegrations;

  /// OAuth2 failure screen: unauthorised heading
  ///
  /// In en, this message translates to:
  /// **'You are not authorised to access FeatureHub'**
  String get oauth2NotAuthorized;

  /// OAuth2 failure screen: instructions
  ///
  /// In en, this message translates to:
  /// **'Please contact your administrator and ask them nicely to add your email to your organization\'s user list'**
  String get oauth2ContactAdmin;

  /// Register URL screen: unexpected server error
  ///
  /// In en, this message translates to:
  /// **'Unexpected error occured\n.Please contact your FeatureHub administrator.'**
  String get registerUrlUnexpectedError;

  /// Register URL screen: link is expired or invalid
  ///
  /// In en, this message translates to:
  /// **'This Register URL is either expired or invalid.\n\nCheck your URL is correct or contact your FeatureHub administrator.'**
  String get registerUrlExpiredOrInvalid;

  /// Register URL screen: loading state while validating token
  ///
  /// In en, this message translates to:
  /// **'Validating your invitation URL'**
  String get validatingInvitationUrl;

  /// Register URL screen: welcome heading
  ///
  /// In en, this message translates to:
  /// **'Welcome to FeatureHub'**
  String get welcomeToFeatureHub;

  /// Register URL screen: sub-heading
  ///
  /// In en, this message translates to:
  /// **'To register please complete the following details'**
  String get registerCompleteDetails;

  /// Label for the name input field
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get nameLabel;

  /// Validation message when name field is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter your name'**
  String get nameRequired;

  /// Validation message when password is too short
  ///
  /// In en, this message translates to:
  /// **'Password must be at least 7 characters!'**
  String get passwordMustBe7Chars;

  /// Validation message when passwords don't match
  ///
  /// In en, this message translates to:
  /// **'Passwords don\'t match'**
  String get passwordsDoNotMatch;

  /// Label for the confirm password field
  ///
  /// In en, this message translates to:
  /// **'Confirm Password'**
  String get confirmPasswordLabel;

  /// Validation message when confirm password is empty
  ///
  /// In en, this message translates to:
  /// **'Please confirm your password'**
  String get confirmPasswordRequired;

  /// Register form submit button
  ///
  /// In en, this message translates to:
  /// **'Register'**
  String get registerButton;

  /// Password strength indicator: weak
  ///
  /// In en, this message translates to:
  /// **'Weak'**
  String get passwordStrengthWeak;

  /// Password strength indicator: below average
  ///
  /// In en, this message translates to:
  /// **'Below average'**
  String get passwordStrengthBelowAverage;

  /// Password strength indicator: good
  ///
  /// In en, this message translates to:
  /// **'Good'**
  String get passwordStrengthGood;

  /// Password strength indicator: strong
  ///
  /// In en, this message translates to:
  /// **'Strong'**
  String get passwordStrengthStrong;

  /// 404 page message
  ///
  /// In en, this message translates to:
  /// **'Sorry, we couldn\'t find the page!'**
  String get notFoundMessage;

  /// Page-not-found fallback header
  ///
  /// In en, this message translates to:
  /// **'Looks like we couldn\'t find any relevant information to display!'**
  String get pageNotFoundMessage;

  /// Label for feature groups docs link
  ///
  /// In en, this message translates to:
  /// **'Feature Groups Documentation'**
  String get featureGroupsDocumentation;

  /// Button to create a new feature group
  ///
  /// In en, this message translates to:
  /// **'Create feature group'**
  String get createFeatureGroup;

  /// Label for application strategies docs link
  ///
  /// In en, this message translates to:
  /// **'Application Strategies Documentation'**
  String get applicationStrategiesDocumentation;

  /// Page header for the edit user screen
  ///
  /// In en, this message translates to:
  /// **'Edit user'**
  String get editUser;

  /// Button and dialog title for resetting a password
  ///
  /// In en, this message translates to:
  /// **'Reset password'**
  String get resetPassword;

  /// Validation message when email field is empty on edit user form
  ///
  /// In en, this message translates to:
  /// **'Edit email address'**
  String get editEmailAddress;

  /// Validation message when name field is empty on edit user form
  ///
  /// In en, this message translates to:
  /// **'Edit names'**
  String get editNames;

  /// Hint text above the group selector on edit user form
  ///
  /// In en, this message translates to:
  /// **'Remove user from a group or add a new one'**
  String get removeOrAddUserToGroup;

  /// Save and close button label
  ///
  /// In en, this message translates to:
  /// **'Save and close'**
  String get saveAndClose;

  /// Snackbar after updating a user
  ///
  /// In en, this message translates to:
  /// **'User {name} has been updated'**
  String userUpdated(String name);

  /// Instructions in the reset password dialog
  ///
  /// In en, this message translates to:
  /// **'After you reset the password below, make sure you email the new password to the user.'**
  String get resetPasswordInstructions;

  /// Label for the new password field
  ///
  /// In en, this message translates to:
  /// **'New password'**
  String get newPasswordLabel;

  /// Validation when new password is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter new password'**
  String get newPasswordRequired;

  /// Label for the confirm new password field
  ///
  /// In en, this message translates to:
  /// **'Confirm new password'**
  String get confirmNewPasswordLabel;

  /// Validation when confirm new password is empty
  ///
  /// In en, this message translates to:
  /// **'Please confirm new password'**
  String get confirmNewPasswordRequired;

  /// Save button label
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get save;

  /// Label prefix before a group name
  ///
  /// In en, this message translates to:
  /// **'Group: '**
  String get groupPrefix;

  /// Label prefix before an application name
  ///
  /// In en, this message translates to:
  /// **'Application: '**
  String get applicationPrefix;

  /// Label prefix before an environment name
  ///
  /// In en, this message translates to:
  /// **'Environment: '**
  String get environmentPrefix;

  /// Button to save all pending changes in the feature group settings
  ///
  /// In en, this message translates to:
  /// **'Apply all changes'**
  String get applyAllChanges;

  /// Snackbar after saving feature group settings
  ///
  /// In en, this message translates to:
  /// **'Settings for group \'{name}\' have been updated'**
  String featureGroupSettingsUpdated(String name);

  /// Shown when user has no permissions for an environment in feature group settings
  ///
  /// In en, this message translates to:
  /// **'No permissions'**
  String get noPermissions;

  /// Dialog title when editing a rollout strategy
  ///
  /// In en, this message translates to:
  /// **'Edit split targeting rules'**
  String get editSplitTargetingRules;

  /// Dialog title when viewing (read-only) a rollout strategy
  ///
  /// In en, this message translates to:
  /// **'View split targeting rules'**
  String get viewSplitTargetingRules;

  /// Button to remove the current rollout strategy
  ///
  /// In en, this message translates to:
  /// **'Remove strategy'**
  String get removeStrategy;

  /// Button to add a new rollout strategy
  ///
  /// In en, this message translates to:
  /// **'Add rollout strategy'**
  String get addRolloutStrategy;

  /// Section heading for the features list in feature group settings
  ///
  /// In en, this message translates to:
  /// **'Features List'**
  String get featuresList;

  /// Button to add a feature to a feature group
  ///
  /// In en, this message translates to:
  /// **'Add Feature'**
  String get addFeature;

  /// Tooltip on the lock icon when a feature value is locked
  ///
  /// In en, this message translates to:
  /// **'Feature value is locked. Unlock from the main Features dashboard to enable editing'**
  String get featureValueLocked;

  /// Validation when admin SA name field is empty
  ///
  /// In en, this message translates to:
  /// **'Please provide a name for the Admin Service Account'**
  String get adminSaNameRequired;

  /// Hint text for the group selector on create admin SA form
  ///
  /// In en, this message translates to:
  /// **'Assign to some portfolio groups or leave it blank to add them later'**
  String get adminSaGroupsHint;

  /// Success heading after creating an admin service account
  ///
  /// In en, this message translates to:
  /// **'Admin Service Account \'{name}\' created!'**
  String adminSaCreated(String name);

  /// Button to go back and create another admin SA
  ///
  /// In en, this message translates to:
  /// **'Create another Service Account'**
  String get createAnotherServiceAccount;

  /// Error when creating an admin SA whose name already exists
  ///
  /// In en, this message translates to:
  /// **'Service Account with name \'{name}\' already exists'**
  String adminSaAlreadyExists(String name);

  /// Page header for the edit admin SDK SA screen
  ///
  /// In en, this message translates to:
  /// **'Edit admin SDK service account'**
  String get editAdminSdkServiceAccount;

  /// Validation message when name field is empty on edit admin SA form
  ///
  /// In en, this message translates to:
  /// **'Edit name'**
  String get editName;

  /// Hint text above the group selector on edit admin SA form
  ///
  /// In en, this message translates to:
  /// **'Remove Admin Service Account from a group or add a new one'**
  String get removeOrAddAdminSaToGroup;

  /// Snackbar after updating an admin service account
  ///
  /// In en, this message translates to:
  /// **'Admin Service Account {name} has been updated'**
  String adminSaUpdated(String name);

  /// Page header when creating an application strategy
  ///
  /// In en, this message translates to:
  /// **'Create Application Strategy for {name}'**
  String createApplicationStrategyTitle(String name);

  /// Page header when editing an application strategy
  ///
  /// In en, this message translates to:
  /// **'Edit Application Strategy for {name}'**
  String editApplicationStrategyTitle(String name);

  /// Dialog title when editing an application
  ///
  /// In en, this message translates to:
  /// **'Edit application'**
  String get editApplication;

  /// Label for the application name field
  ///
  /// In en, this message translates to:
  /// **'Application name'**
  String get appNameLabel;

  /// Validation: app name empty
  ///
  /// In en, this message translates to:
  /// **'Please enter an application name'**
  String get appNameRequired;

  /// Validation: app name too short
  ///
  /// In en, this message translates to:
  /// **'Application name needs to be at least 4 characters long'**
  String get appNameTooShort;

  /// Label for the application description field
  ///
  /// In en, this message translates to:
  /// **'Application description'**
  String get appDescriptionLabel;

  /// Validation: app description empty
  ///
  /// In en, this message translates to:
  /// **'Please enter app description'**
  String get appDescriptionRequired;

  /// Validation: app description too short
  ///
  /// In en, this message translates to:
  /// **'Application description needs to be at least 4 characters long'**
  String get appDescriptionTooShort;

  /// Snackbar after updating an application
  ///
  /// In en, this message translates to:
  /// **'Application {name} updated!'**
  String appUpdated(String name);

  /// Snackbar after creating an application
  ///
  /// In en, this message translates to:
  /// **'Application {name} created!'**
  String appCreated(String name);

  /// Error when creating an application that already exists
  ///
  /// In en, this message translates to:
  /// **'Application \'{name}\' already exists'**
  String appAlreadyExists(String name);

  /// Dialog title when creating a feature group
  ///
  /// In en, this message translates to:
  /// **'Create new Feature Group'**
  String get createNewFeatureGroup;

  /// Dialog title when editing a feature group
  ///
  /// In en, this message translates to:
  /// **'Edit Feature Group'**
  String get editFeatureGroup;

  /// Label for the feature group name field
  ///
  /// In en, this message translates to:
  /// **'Feature group name'**
  String get featureGroupNameLabel;

  /// Validation: feature group name empty
  ///
  /// In en, this message translates to:
  /// **'Please enter feature group name'**
  String get featureGroupNameRequired;

  /// Validation: feature group name too short
  ///
  /// In en, this message translates to:
  /// **'Group name needs to be at least 4 characters long'**
  String get featureGroupNameTooShort;

  /// Label for the feature group description field
  ///
  /// In en, this message translates to:
  /// **'Feature group description'**
  String get featureGroupDescriptionLabel;

  /// Validation: feature group description empty
  ///
  /// In en, this message translates to:
  /// **'Please enter feature group description'**
  String get featureGroupDescriptionRequired;

  /// Validation: feature group description too short
  ///
  /// In en, this message translates to:
  /// **'Description needs to be at least 4 characters long'**
  String get featureGroupDescriptionTooShort;

  /// Snackbar after updating a feature group
  ///
  /// In en, this message translates to:
  /// **'Feature group {name} updated!'**
  String featureGroupUpdated(String name);

  /// Snackbar after creating a feature group
  ///
  /// In en, this message translates to:
  /// **'Feature group {name} created!'**
  String featureGroupCreated(String name);

  /// Error when creating a feature group that already exists
  ///
  /// In en, this message translates to:
  /// **'Feature group \'{name}\' already exists'**
  String featureGroupAlreadyExists(String name);

  /// Dialog title when editing a group
  ///
  /// In en, this message translates to:
  /// **'Edit group'**
  String get editGroup;

  /// Label for the group name field
  ///
  /// In en, this message translates to:
  /// **'Group name'**
  String get groupNameLabel;

  /// Validation: group name empty
  ///
  /// In en, this message translates to:
  /// **'Please enter a group name'**
  String get groupNameRequired;

  /// Validation: group name too short
  ///
  /// In en, this message translates to:
  /// **'Group name needs to be at least 4 characters long'**
  String get groupNameTooShort;

  /// Snackbar after creating a group
  ///
  /// In en, this message translates to:
  /// **'Group \'{name}\' created!'**
  String groupCreated(String name);

  /// Error when creating a group that already exists
  ///
  /// In en, this message translates to:
  /// **'Group \'{name}\' already exists'**
  String groupAlreadyExists(String name);

  /// Body of delete group confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'All permissions belonging to this group will be deleted \n\nThis cannot be undone!'**
  String get groupDeleteContent;

  /// Snackbar after deleting a group
  ///
  /// In en, this message translates to:
  /// **'Group \'{name}\' deleted!'**
  String groupDeleted(String name);

  /// Error when deleting a group fails
  ///
  /// In en, this message translates to:
  /// **'Could not delete group {name}'**
  String couldNotDeleteGroup(String name);

  /// Dialog title when viewing a feature (read-only)
  ///
  /// In en, this message translates to:
  /// **'View feature'**
  String get viewFeature;

  /// Dialog title when editing a feature
  ///
  /// In en, this message translates to:
  /// **'Edit feature'**
  String get editFeature;

  /// Label for the feature name field
  ///
  /// In en, this message translates to:
  /// **'Feature name'**
  String get featureNameLabel;

  /// Validation: feature name empty
  ///
  /// In en, this message translates to:
  /// **'Please enter feature name'**
  String get featureNameRequired;

  /// Validation: feature name too short
  ///
  /// In en, this message translates to:
  /// **'Feature name needs to be at least 4 characters long'**
  String get featureNameTooShort;

  /// Label for the feature key field
  ///
  /// In en, this message translates to:
  /// **'Feature key'**
  String get featureKeyLabel;

  /// Hint text for the feature key field
  ///
  /// In en, this message translates to:
  /// **'To be used in the code with FeatureHub SDK'**
  String get featureKeyHint;

  /// Validation: feature key empty
  ///
  /// In en, this message translates to:
  /// **'Please enter feature key'**
  String get featureKeyRequired;

  /// Validation: feature key contains whitespace
  ///
  /// In en, this message translates to:
  /// **'Cannot contain whitespace'**
  String get featureKeyNoWhitespace;

  /// Label for the feature description field
  ///
  /// In en, this message translates to:
  /// **'Description (optional)'**
  String get featureDescriptionLabel;

  /// Hint text for the feature description field
  ///
  /// In en, this message translates to:
  /// **'Some information about feature'**
  String get featureDescriptionHint;

  /// Label for the feature reference link field
  ///
  /// In en, this message translates to:
  /// **'Reference link (optional)'**
  String get featureLinkLabel;

  /// Hint text for the feature reference link field
  ///
  /// In en, this message translates to:
  /// **'Optional link to external tracking system, e.g. Jira'**
  String get featureLinkHint;

  /// Hint and error text for the feature type dropdown
  ///
  /// In en, this message translates to:
  /// **'Select feature type'**
  String get selectFeatureType;

  /// Snackbar after updating a feature
  ///
  /// In en, this message translates to:
  /// **'Feature {name} updated!'**
  String featureUpdated(String name);

  /// Snackbar after creating a feature
  ///
  /// In en, this message translates to:
  /// **'Feature {name} created!'**
  String featureCreated(String name);

  /// Error when creating a feature whose key already exists
  ///
  /// In en, this message translates to:
  /// **'Feature with key \'{key}\' already exists'**
  String featureKeyAlreadyExists(String key);

  /// Feature type dropdown option: string
  ///
  /// In en, this message translates to:
  /// **'String'**
  String get featureTypeString;

  /// Feature type dropdown option: number
  ///
  /// In en, this message translates to:
  /// **'Number'**
  String get featureTypeNumber;

  /// Feature type dropdown option: boolean
  ///
  /// In en, this message translates to:
  /// **'Standard flag (boolean)'**
  String get featureTypeBoolean;

  /// Feature type dropdown option: JSON
  ///
  /// In en, this message translates to:
  /// **'Remote configuration (JSON)'**
  String get featureTypeJson;

  /// Warning text in the admin SA token reset dialog
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to reset the access token for this service account?\nThis will invalidate the current token!'**
  String get adminSaResetTokenWarning;

  /// Dialog title after resetting an admin SDK token
  ///
  /// In en, this message translates to:
  /// **'Admin SDK access token has been reset'**
  String get adminSdkTokenReset;

  /// Snackbar after resetting an admin SDK token
  ///
  /// In en, this message translates to:
  /// **'Admin SDK access token has been reset!'**
  String get adminSdkTokenResetSnackbar;

  /// Error when resetting an admin SA token fails
  ///
  /// In en, this message translates to:
  /// **'Unable to reset access token'**
  String get unableToResetToken;

  /// Warning text in the client API key reset dialog
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to reset ALL client eval API keys for this service account?\nThis will affect the keys across all environments and all applications that this service account has access to!'**
  String get resetClientApiKeysWarning;

  /// Warning text in the server API key reset dialog
  ///
  /// In en, this message translates to:
  /// **'Are you sure you want to reset ALL server eval API keys for this service account?\nThis will affect the keys across all environments and all applications that this service account has access to!'**
  String get resetServerApiKeysWarning;

  /// Snackbar after resetting client API keys
  ///
  /// In en, this message translates to:
  /// **'\'Client\' eval API Keys have been reset!'**
  String get clientApiKeysReset;

  /// Snackbar after resetting server API keys
  ///
  /// In en, this message translates to:
  /// **'\'Server\' eval API Keys have been reset!'**
  String get serverApiKeysReset;

  /// Error when resetting an API key fails
  ///
  /// In en, this message translates to:
  /// **'Unable to reset API Key'**
  String get unableToResetApiKey;

  /// Dialog title when viewing feature metadata (read-only)
  ///
  /// In en, this message translates to:
  /// **'View metadata for \'{name}\''**
  String viewMetadataFor(String name);

  /// Dialog title when editing feature metadata
  ///
  /// In en, this message translates to:
  /// **'Edit metadata for \'{name}\''**
  String editMetadataFor(String name);

  /// Button to confirm setting a value in a dialog
  ///
  /// In en, this message translates to:
  /// **'Set value'**
  String get setValue;

  /// Snackbar after updating feature metadata
  ///
  /// In en, this message translates to:
  /// **'Feature {name} metadata has been updated!'**
  String featureMetadataUpdated(String name);

  /// Dialog title for the JSON value editor
  ///
  /// In en, this message translates to:
  /// **'Set feature value'**
  String get setFeatureValue;

  /// Dialog title when adding a new rollout strategy
  ///
  /// In en, this message translates to:
  /// **'Add rollout strategy targeting rules'**
  String get addRolloutStrategyTargetingRules;

  /// Label for the strategy name field in the strategy editor
  ///
  /// In en, this message translates to:
  /// **'Split strategy name'**
  String get splitStrategyName;

  /// Helper text for the strategy name field
  ///
  /// In en, this message translates to:
  /// **'E.g. 20% rollout'**
  String get splitStrategyNameExample;

  /// Validation: strategy name is empty
  ///
  /// In en, this message translates to:
  /// **'Strategy name required'**
  String get strategyNameRequired;

  /// Label for the percentage value field
  ///
  /// In en, this message translates to:
  /// **'Percentage value'**
  String get percentageValue;

  /// Helper text for the percentage value field
  ///
  /// In en, this message translates to:
  /// **'You can enter a value with up to 4 decimal points, e.g. 0.0005 %'**
  String get percentageValueHelperText;

  /// Validation: percentage value is empty
  ///
  /// In en, this message translates to:
  /// **'Percentage value required'**
  String get percentageValueRequired;

  /// Label next to the add-percentage button
  ///
  /// In en, this message translates to:
  /// **'Add percentage rollout rule'**
  String get addPercentageRolloutRule;

  /// Button to add a percentage rollout rule
  ///
  /// In en, this message translates to:
  /// **'+ Percentage'**
  String get addPercentage;

  /// Error when percentage totals exceed 100%
  ///
  /// In en, this message translates to:
  /// **'Your percentage total across all rollout values cannot be over 100%. Please enter different value.'**
  String get percentageTotalOver100Error;

  /// Button label to add/save a new strategy
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get add;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'zh'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
