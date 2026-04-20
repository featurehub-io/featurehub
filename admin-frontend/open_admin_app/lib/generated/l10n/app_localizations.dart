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

  /// Navigation menu item for feature filters
  ///
  /// In en, this message translates to:
  /// **'Feature Filters'**
  String get featureFilters;

  /// Page header for the manage feature filters screen
  ///
  /// In en, this message translates to:
  /// **'Manage feature filters'**
  String get manageFeatureFilters;

  /// Label for feature filters docs link
  ///
  /// In en, this message translates to:
  /// **'Feature Filters Documentation'**
  String get featureFiltersDocumentation;

  /// Button to create a new feature filter
  ///
  /// In en, this message translates to:
  /// **'Create New Feature Filter'**
  String get createNewFeatureFilter;

  /// Dialog title when editing a feature filter
  ///
  /// In en, this message translates to:
  /// **'Edit Feature Filter'**
  String get editFeatureFilter;

  /// Used to show a message when there are not any feature filters
  ///
  /// In en, this message translates to:
  /// **'No filters were found in the portfolio.'**
  String get noFeatureFiltersFound;

  /// Label for the filter name field
  ///
  /// In en, this message translates to:
  /// **'Filter name'**
  String get filterNameLabel;

  /// Label for the filter description field
  ///
  /// In en, this message translates to:
  /// **'Filter description'**
  String get filterDescriptionLabel;

  /// Label for the filter description field
  ///
  /// In en, this message translates to:
  /// **'Description'**
  String get filterTableDescriptionLabel;

  /// Validation: filter name empty
  ///
  /// In en, this message translates to:
  /// **'Please enter a filter name'**
  String get filterNameRequired;

  /// Validation: filter name too short
  ///
  /// In en, this message translates to:
  /// **'Filter name needs to be at least 4 characters long'**
  String get filterNameTooShort;

  /// Validation: filter description empty
  ///
  /// In en, this message translates to:
  /// **'Please enter a filter description'**
  String get filterDescriptionRequired;

  /// Validation: filter description too short
  ///
  /// In en, this message translates to:
  /// **'Filter description needs to be at least 4 characters long'**
  String get filterDescriptionTooShort;

  /// Snackbar after creating a feature filter
  ///
  /// In en, this message translates to:
  /// **'Feature filter \'{name}\' created!'**
  String filterCreated(String name);

  /// Snackbar after updating a feature filter
  ///
  /// In en, this message translates to:
  /// **'Feature filter \'{name}\' updated!'**
  String filterUpdated(String name);

  /// Snackbar after deleting a feature filter
  ///
  /// In en, this message translates to:
  /// **'Feature filter \'{name}\' deleted!'**
  String filterDeleted(String name);

  /// showing applications using feature filters and how many features they have using those filters. this is singular
  ///
  /// In en, this message translates to:
  /// **'{appName} ({count} feature)'**
  String appPlusFeatureCountSingular(String appName, int count);

  /// showing applications using feature filters and how many features they have using those filters. this is plural
  ///
  /// In en, this message translates to:
  /// **'{appName} ({count} features)'**
  String appPlusFeatureCountPlural(String appName, int count);

  /// Body of delete feature filter confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'Deleting this filter will remove it from all features and service accounts.\n\nThis cannot be undone!'**
  String get filterDeleteContent;

  /// Label for applications and their filters
  ///
  /// In en, this message translates to:
  /// **'Applications/Features'**
  String get appsUsingFilter;

  /// Label for service accounts using the feature
  ///
  /// In en, this message translates to:
  /// **'Service Accounts'**
  String get saUsingFilter;

  /// Label for filter selection dropdown
  ///
  /// In en, this message translates to:
  /// **'Select feature filters to apply'**
  String get selectFiltersToApply;

  /// Label for matching service accounts list
  ///
  /// In en, this message translates to:
  /// **'Service Accounts using filter'**
  String get matchingServiceAccounts;

  /// Label for matching applications list
  ///
  /// In en, this message translates to:
  /// **'Applications using filter'**
  String get matchingFeatures;

  /// Hint text for the dashboard filters filter
  ///
  /// In en, this message translates to:
  /// **'Filter by feature filters'**
  String get filterByFeatureFilters;

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

  /// Hint text for the group selector dropdown
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

  /// External link label for service accounts documentation
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

  /// Hint text on the feature type filter dropdown in the features dashboard
  ///
  /// In en, this message translates to:
  /// **'Filter by feature type'**
  String get filterByFeatureType;

  /// Hint text in the features search field in the features dashboard
  ///
  /// In en, this message translates to:
  /// **'Search features'**
  String get searchFeatures;

  /// Button to create a new application strategy
  ///
  /// In en, this message translates to:
  /// **'Create new strategy'**
  String get createNewStrategy;

  /// Hint text in the application strategies search field
  ///
  /// In en, this message translates to:
  /// **'Search strategy'**
  String get searchStrategy;

  /// Table column header: strategy name
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get columnStrategyName;

  /// Table column header: creation date
  ///
  /// In en, this message translates to:
  /// **'Date created (UTC)'**
  String get columnDateCreated;

  /// Table column header: last updated date
  ///
  /// In en, this message translates to:
  /// **'Date updated (UTC)'**
  String get columnDateUpdated;

  /// Table column header: creator
  ///
  /// In en, this message translates to:
  /// **'Created by'**
  String get columnCreatedBy;

  /// Table column header: usage summary
  ///
  /// In en, this message translates to:
  /// **'Used in'**
  String get columnUsedIn;

  /// Empty state when portfolio has no applications for strategy creation
  ///
  /// In en, this message translates to:
  /// **'Cannot create application strategy as there are no applications in this portfolio'**
  String get cannotCreateStrategyNoApps;

  /// Body of the delete application strategy confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'This application strategy will be deleted and unassigned from all the flags. \n\nThis cannot be undone!'**
  String get appStrategyDeleteContent;

  /// Snackbar after deleting an application strategy
  ///
  /// In en, this message translates to:
  /// **'Application strategy \'{name}\' deleted!'**
  String appStrategyDeleted(String name);

  /// Strategy usage cell: how many environments and feature values use this strategy
  ///
  /// In en, this message translates to:
  /// **'environments: {envCount}, feature values: {featureCount}'**
  String strategyUsage(int envCount, int featureCount);

  /// Label before the well-known rule type buttons in the strategy editor
  ///
  /// In en, this message translates to:
  /// **'Add rule'**
  String get addRule;

  /// Label before the custom rule button in the strategy editor
  ///
  /// In en, this message translates to:
  /// **'Add custom rule'**
  String get addCustomRule;

  /// Button to add a custom attribute rule in the strategy editor
  ///
  /// In en, this message translates to:
  /// **'+ Custom'**
  String get addCustomButton;

  /// Hint on the condition/matcher dropdown in the strategy rule editor
  ///
  /// In en, this message translates to:
  /// **'Select condition'**
  String get selectCondition;

  /// Hint on the value-type dropdown for custom attribute rules
  ///
  /// In en, this message translates to:
  /// **'Select value type'**
  String get selectValueType;

  /// Hint on the boolean value dropdown in the strategy rule editor
  ///
  /// In en, this message translates to:
  /// **'Select value'**
  String get selectValue;

  /// Hint on the country multiselect dropdown
  ///
  /// In en, this message translates to:
  /// **'Select Country'**
  String get selectCountry;

  /// Hint on the device multiselect dropdown
  ///
  /// In en, this message translates to:
  /// **'Select Device'**
  String get selectDevice;

  /// Hint on the platform multiselect dropdown
  ///
  /// In en, this message translates to:
  /// **'Select Platform'**
  String get selectPlatform;

  /// Label for the custom attribute key field
  ///
  /// In en, this message translates to:
  /// **'Custom key'**
  String get customKey;

  /// Helper text example for the custom attribute key field
  ///
  /// In en, this message translates to:
  /// **'e.g. \"warehouse-id\"'**
  String get customKeyExample;

  /// Validation message when the custom rule key is empty
  ///
  /// In en, this message translates to:
  /// **'Rule name required'**
  String get ruleNameRequired;

  /// Tooltip on the delete-rule icon button
  ///
  /// In en, this message translates to:
  /// **'Delete rule'**
  String get deleteRule;

  /// Label for the user-key value input field
  ///
  /// In en, this message translates to:
  /// **'User key(s)'**
  String get userKeys;

  /// Helper text example for user key values
  ///
  /// In en, this message translates to:
  /// **'e.g. bob@xyz.com'**
  String get userKeyExample;

  /// Label for the version value input field
  ///
  /// In en, this message translates to:
  /// **'Version(s)'**
  String get versions;

  /// Helper text example for version values
  ///
  /// In en, this message translates to:
  /// **'e.g. 1.3.4, 7.8.1-SNAPSHOT'**
  String get versionExample;

  /// Label for the custom attribute value input field
  ///
  /// In en, this message translates to:
  /// **'Custom value(s)'**
  String get customValues;

  /// Helper text example for custom attribute values
  ///
  /// In en, this message translates to:
  /// **'e.g. WarehouseA, WarehouseB'**
  String get customValuesExample;

  /// Label for the number value input field
  ///
  /// In en, this message translates to:
  /// **'Number(s)'**
  String get numbers;

  /// Helper text example for number values
  ///
  /// In en, this message translates to:
  /// **'e.g. 6, 7.87543'**
  String get numberExample;

  /// Label for the date value input field
  ///
  /// In en, this message translates to:
  /// **'Date(s) - YYYY-MM-DD'**
  String get dates;

  /// Helper text example for date values
  ///
  /// In en, this message translates to:
  /// **'e.g. 2017-04-16'**
  String get dateExample;

  /// Label for the datetime value input field
  ///
  /// In en, this message translates to:
  /// **'Date/Time(s) - UTC/ISO8601 format'**
  String get dateTimes;

  /// Helper text example for datetime values
  ///
  /// In en, this message translates to:
  /// **'e.g. 2007-03-01T13:00:00Z'**
  String get dateTimeExample;

  /// Label for the IP address value input field
  ///
  /// In en, this message translates to:
  /// **'IP Address(es) with or without CIDR'**
  String get ipAddresses;

  /// Helper text example for IP address values
  ///
  /// In en, this message translates to:
  /// **'e.g. 168.192.54.3 or 192.168.86.1/8'**
  String get ipAddressExample;

  /// Tooltip and label on the add-value button in the strategy rule editor
  ///
  /// In en, this message translates to:
  /// **'Add value'**
  String get addValue;

  /// Snackbar after drag-reorder of environments
  ///
  /// In en, this message translates to:
  /// **'Environment order updated!'**
  String get envOrderUpdated;

  /// Tooltip on the production environment badge
  ///
  /// In en, this message translates to:
  /// **'Production environment'**
  String get productionEnvironment;

  /// Warning when deleting a production environment
  ///
  /// In en, this message translates to:
  /// **'The environment \'{name}\' is your production environment, are you sure you wish to remove it?'**
  String deleteProductionEnvWarning(String name);

  /// Snackbar after deleting an environment
  ///
  /// In en, this message translates to:
  /// **'Environment \'{name}\' deleted!'**
  String envDeleted(String name);

  /// Error when environment deletion fails
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t delete environment {name}'**
  String envDeleteError(String name);

  /// Button label and dialog title for creating a new environment
  ///
  /// In en, this message translates to:
  /// **'Create new environment'**
  String get createNewEnvironment;

  /// Dialog title for editing an environment
  ///
  /// In en, this message translates to:
  /// **'Edit environment'**
  String get editEnvironment;

  /// Label for the environment name field
  ///
  /// In en, this message translates to:
  /// **'Environment name'**
  String get environmentName;

  /// Validation message when environment name is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter an environment name'**
  String get envNameRequired;

  /// Validation message when environment name is too short
  ///
  /// In en, this message translates to:
  /// **'Environment name needs to be at least 2 characters long'**
  String get envNameTooShort;

  /// Checkbox label for flagging an environment as production
  ///
  /// In en, this message translates to:
  /// **'Mark as production environment'**
  String get markAsProductionEnvironment;

  /// Snackbar after updating an environment
  ///
  /// In en, this message translates to:
  /// **'Environment {name} updated!'**
  String envUpdated(String name);

  /// Snackbar after creating an environment
  ///
  /// In en, this message translates to:
  /// **'Environment {name} created!'**
  String envCreated(String name);

  /// Error when environment name is already taken
  ///
  /// In en, this message translates to:
  /// **'Environment with name {name} already exists'**
  String envAlreadyExists(String name);

  /// Info card on the Environments tab
  ///
  /// In en, this message translates to:
  /// **'Environments can be ordered by dragging the cards below, showing the deployment promotion order to production (top to bottom). This order will be reflected on the \'Features\' dashboard. It helps your teams see their feature status per environment in the correct order.'**
  String get environmentsInfoMessage;

  /// External link label for environments documentation
  ///
  /// In en, this message translates to:
  /// **'Environments Documentation'**
  String get environmentsDocumentation;

  /// Label above the group selector dropdown
  ///
  /// In en, this message translates to:
  /// **'Group'**
  String get group;

  /// Link button to navigate to group members management
  ///
  /// In en, this message translates to:
  /// **'Go to manage group members'**
  String get goToManageGroupMembers;

  /// External link label for group permissions documentation
  ///
  /// In en, this message translates to:
  /// **'Group Permissions Documentation'**
  String get groupPermissionsDocumentation;

  /// Placeholder text when no group is selected
  ///
  /// In en, this message translates to:
  /// **'You need to select a group to edit the permissions for.'**
  String get selectGroupToEditPermissions;

  /// Message when no environments exist yet
  ///
  /// In en, this message translates to:
  /// **'You need to first create some \'Environments\' for this application.'**
  String get needToCreateEnvironmentsFirst;

  /// Label above the feature-level permissions dropdown
  ///
  /// In en, this message translates to:
  /// **'Set feature level permissions'**
  String get setFeatureLevelPermissions;

  /// Label above the app strategy permissions dropdown
  ///
  /// In en, this message translates to:
  /// **'Set application strategy permissions'**
  String get setAppStrategyPermissions;

  /// Label above the per-environment permissions table
  ///
  /// In en, this message translates to:
  /// **'Set feature value level permissions per environment'**
  String get setFeatureValuePermissions;

  /// Table column header for Read permission
  ///
  /// In en, this message translates to:
  /// **'Read'**
  String get permRead;

  /// Table column header for Lock permission
  ///
  /// In en, this message translates to:
  /// **'Lock'**
  String get permLock;

  /// Table column header for Unlock permission
  ///
  /// In en, this message translates to:
  /// **'Unlock'**
  String get permUnlock;

  /// Table column header for Change value / Retire permission
  ///
  /// In en, this message translates to:
  /// **'Change value / Retire'**
  String get permChangeValue;

  /// Table column header for extended data read permission
  ///
  /// In en, this message translates to:
  /// **'Read Extended Feature Data'**
  String get permReadExtendedData;

  /// Message when a portfolio has no service accounts
  ///
  /// In en, this message translates to:
  /// **'There are no service accounts in the \"{name}\" portfolio.'**
  String noServiceAccountsInPortfolio(String name);

  /// Link button to navigate to service accounts page
  ///
  /// In en, this message translates to:
  /// **'Go to service accounts settings'**
  String get goToServiceAccountSettings;

  /// Label above the service account selector dropdown
  ///
  /// In en, this message translates to:
  /// **'Service account'**
  String get serviceAccount;

  /// Info card on the Service Account Permissions tab
  ///
  /// In en, this message translates to:
  /// **'We strongly recommend setting production environments with only \'Read\' permission for service accounts. The \'Lock/Unlock\' and \'Change value\' permissions typically given to service accounts for testing purposes, e.g. changing feature values states through the SDK when running tests.'**
  String get serviceAccountInfoMessage;

  /// Hint text for the service account selector dropdown
  ///
  /// In en, this message translates to:
  /// **'Select service account'**
  String get selectServiceAccount;

  /// Label above the service account permissions table
  ///
  /// In en, this message translates to:
  /// **'Set the service account access to features for each environment'**
  String get setServiceAccountPermissions;

  /// Snackbar after updating service account permissions
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' updated!'**
  String serviceAccountUpdated(String name);

  /// Label above the environment selector on the Integrations tab
  ///
  /// In en, this message translates to:
  /// **'Environment'**
  String get environmentLabel;

  /// Shown when no environments are available on the Integrations tab
  ///
  /// In en, this message translates to:
  /// **'no environments'**
  String get noEnvironments;

  /// Label above the integration type selector on the Integrations tab
  ///
  /// In en, this message translates to:
  /// **'Integration Type'**
  String get integrationTypeLabel;

  /// Hint text for the webhook type dropdown
  ///
  /// In en, this message translates to:
  /// **'Select webhook type'**
  String get selectWebhookType;

  /// Hint text for the environment dropdown on the Integrations tab
  ///
  /// In en, this message translates to:
  /// **'Select environment'**
  String get selectEnvironment;

  /// Title for the Slack channel settings panel
  ///
  /// In en, this message translates to:
  /// **'Slack Channel Settings (per environment)'**
  String get slackChannelSettings;

  /// Label for the Slack integration documentation external link
  ///
  /// In en, this message translates to:
  /// **'Slack Integration Documentation'**
  String get slackIntegrationDocumentation;

  /// Checkbox label for enabling an integration
  ///
  /// In en, this message translates to:
  /// **'Enabled'**
  String get enabled;

  /// Label for the Slack channel ID field
  ///
  /// In en, this message translates to:
  /// **'Slack channel ID (leave empty to use default)'**
  String get slackChannelId;

  /// Hint text for the Slack channel ID field
  ///
  /// In en, this message translates to:
  /// **'e.g. C0150T7AF25'**
  String get slackChannelIdExample;

  /// Snackbar after saving Slack settings
  ///
  /// In en, this message translates to:
  /// **'Slack settings have been updated'**
  String get slackSettingsUpdated;

  /// Header for the message delivery status panel
  ///
  /// In en, this message translates to:
  /// **'Message delivery status'**
  String get messageDeliveryStatus;

  /// Button label for refreshing data
  ///
  /// In en, this message translates to:
  /// **'Refresh'**
  String get refresh;

  /// Empty state for the delivery status list
  ///
  /// In en, this message translates to:
  /// **'There is no activity as yet.'**
  String get noActivity;

  /// Status for a sent but unacknowledged webhook request
  ///
  /// In en, this message translates to:
  /// **'Unacknowledged request sent at {time}'**
  String unacknowledgedRequest(String time);

  /// Status line for a successfully received webhook
  ///
  /// In en, this message translates to:
  /// **'Status: {status}, received at {time}'**
  String deliveryStatusReceived(String status, String time);

  /// Status line for a failed webhook delivery
  ///
  /// In en, this message translates to:
  /// **'{status} received at {time}'**
  String deliveryStatusError(String status, String time);

  /// Label for response headers section in delivery status
  ///
  /// In en, this message translates to:
  /// **'Response headers:'**
  String get responseHeaders;

  /// Label for the content section in delivery status
  ///
  /// In en, this message translates to:
  /// **'Content'**
  String get content;

  /// Button to load more records in the delivery status list
  ///
  /// In en, this message translates to:
  /// **'More records'**
  String get moreRecords;

  /// Button to retry loading records after an error
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get retry;

  /// Delivery status: HTTP 2xx
  ///
  /// In en, this message translates to:
  /// **'Successfully delivered'**
  String get deliveredSuccessfully;

  /// Delivery status: HTTP 400
  ///
  /// In en, this message translates to:
  /// **'Undeliverable, some information missing'**
  String get undeliverableInfo;

  /// Delivery status: HTTP 418
  ///
  /// In en, this message translates to:
  /// **'Unable to create the necessary data to send to remote system'**
  String get unableToCreateData;

  /// Delivery status: HTTP 422
  ///
  /// In en, this message translates to:
  /// **'Some system configuration is missing to be able to complete'**
  String get systemConfigMissing;

  /// Delivery status: HTTP 424
  ///
  /// In en, this message translates to:
  /// **'Some system error talking to remote system (e.g. system was down)'**
  String get remoteSystemError;

  /// Delivery status: HTTP 500
  ///
  /// In en, this message translates to:
  /// **'Unexpected result from remote system'**
  String get unexpectedResult;

  /// Delivery status: HTTP 503
  ///
  /// In en, this message translates to:
  /// **'Network error, host unknown'**
  String get networkError;

  /// Tooltip for the webhook history tab button
  ///
  /// In en, this message translates to:
  /// **'Webhook History'**
  String get webhookHistory;

  /// Tooltip for the webhook configuration tab button
  ///
  /// In en, this message translates to:
  /// **'Webhook Configuration'**
  String get webhookConfiguration;

  /// External link label for webhooks documentation
  ///
  /// In en, this message translates to:
  /// **'Webhooks Documentation'**
  String get webhooksDocumentation;

  /// Table column header: event type
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get colType;

  /// Table column header: HTTP method
  ///
  /// In en, this message translates to:
  /// **'Method'**
  String get colMethod;

  /// Table column header: HTTP status code
  ///
  /// In en, this message translates to:
  /// **'HTTP Code'**
  String get colHttpCode;

  /// Table column header: send timestamp
  ///
  /// In en, this message translates to:
  /// **'When Sent'**
  String get colWhenSent;

  /// Table column header: row actions
  ///
  /// In en, this message translates to:
  /// **'Actions'**
  String get colActions;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'When sent'**
  String get webhookWhenSent;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Webhook Cloud Event type'**
  String get webhookCloudEventType;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'URL'**
  String get webhookUrl;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Method'**
  String get webhookDetailMethod;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'HTTP status'**
  String get webhookHttpStatus;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Cloud Event type'**
  String get cloudEventType;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Incoming headers'**
  String get incomingHeaders;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Outgoing headers'**
  String get outgoingHeaders;

  /// Row label in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Webhook Content'**
  String get webhookContent;

  /// Tooltip on the copy button in the webhook detail view
  ///
  /// In en, this message translates to:
  /// **'Copy Content'**
  String get copyContent;

  /// Hint text for the service accounts search field
  ///
  /// In en, this message translates to:
  /// **'Search Service Accounts'**
  String get searchServiceAccounts;

  /// Table column header and info-row label for name
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get colName;

  /// Table column header and info-row label for groups
  ///
  /// In en, this message translates to:
  /// **'Groups'**
  String get colGroups;

  /// Tooltip on the reset-token icon button in the admin service accounts table
  ///
  /// In en, this message translates to:
  /// **'Reset Admin SDK access token'**
  String get resetAdminSdkToken;

  /// Body text of the delete-service-account confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'This service account will be removed from all groups and deleted from the organization. \n\nThis cannot be undone!'**
  String get adminSADeleteContent;

  /// Snackbar after deleting an admin service account
  ///
  /// In en, this message translates to:
  /// **'Service account \'{name}\' deleted!'**
  String adminSADeleted(String name);

  /// Title of the admin service account info dialog
  ///
  /// In en, this message translates to:
  /// **'Admin Service Account details'**
  String get adminSADetailsTitle;

  /// Label above the access token value
  ///
  /// In en, this message translates to:
  /// **'Access token'**
  String get accessToken;

  /// Caption on the copy-to-clipboard button for access tokens
  ///
  /// In en, this message translates to:
  /// **'Copy access token to clipboard'**
  String get copyAccessToken;

  /// Security notice shown when displaying an access token
  ///
  /// In en, this message translates to:
  /// **'For security, you will not be able to view the access token once you close this window.'**
  String get accessTokenSecurityNote;

  /// Page header for the system configurations screen
  ///
  /// In en, this message translates to:
  /// **'System Configurations'**
  String get systemConfigurationsTitle;

  /// Expansion tile title for site configuration section
  ///
  /// In en, this message translates to:
  /// **'Site Configuration'**
  String get siteConfigurationTitle;

  /// Expansion tile subtitle for site configuration section
  ///
  /// In en, this message translates to:
  /// **'Configure your FeatureHub system'**
  String get siteConfigurationSubtitle;

  /// Expansion tile title for Slack configuration section
  ///
  /// In en, this message translates to:
  /// **'Slack Configuration'**
  String get slackConfigurationTitle;

  /// Expansion tile subtitle for Slack configuration section
  ///
  /// In en, this message translates to:
  /// **'Enable FeatureHub to send Slack messages'**
  String get slackConfigurationSubtitle;

  /// Warning shown when encryption is not configured and Slack cannot be enabled
  ///
  /// In en, this message translates to:
  /// **'You are required to configure encryption key/password in the FeatureHub system properties file to enable Slack integration'**
  String get encryptionRequiredForSlack;

  /// Label for the encryption documentation external link
  ///
  /// In en, this message translates to:
  /// **'Encryption documentation'**
  String get encryptionDocumentation;

  /// Label for the site URL input field
  ///
  /// In en, this message translates to:
  /// **'The URL of your organisation\'s FeatureHub app'**
  String get siteUrlLabel;

  /// Validation error when site URL is empty
  ///
  /// In en, this message translates to:
  /// **'You cannot specify an empty url'**
  String get siteUrlEmptyError;

  /// Validation error when site URL does not start with http:// or https://
  ///
  /// In en, this message translates to:
  /// **'You must specify a valid url for your site'**
  String get siteUrlInvalidError;

  /// Checkbox label for allowing search robots to index the site
  ///
  /// In en, this message translates to:
  /// **'Allow search robots to index'**
  String get allowSearchRobots;

  /// Checkbox label for redirecting traffic with invalid Hosts header
  ///
  /// In en, this message translates to:
  /// **'Redirect traffic with bad Hosts header'**
  String get redirectBadHostsHeader;

  /// Checkbox label to enable Slack integration
  ///
  /// In en, this message translates to:
  /// **'Enable Slack'**
  String get enableSlack;

  /// Label on the outlined button to connect FeatureHub to Slack workspace
  ///
  /// In en, this message translates to:
  /// **'Connect FeatureHub to Slack'**
  String get connectFeatureHubToSlack;

  /// Tooltip for the connect-to-Slack outlined button
  ///
  /// In en, this message translates to:
  /// **'Install FeatureHub Bot app to your Slack workspace'**
  String get installFeatureHubBot;

  /// Label on the filled button to initiate Slack OAuth connection
  ///
  /// In en, this message translates to:
  /// **'Connect to Slack'**
  String get connectToSlack;

  /// Label for the Slack bot token input field
  ///
  /// In en, this message translates to:
  /// **'Slack Bot User OAuth Token'**
  String get slackBotTokenLabel;

  /// Validation error when Slack bot token is empty and Slack is enabled
  ///
  /// In en, this message translates to:
  /// **'Please enter Slack Bot User OAuth token'**
  String get slackBotTokenRequired;

  /// Label for the default Slack channel ID input field
  ///
  /// In en, this message translates to:
  /// **'Default Slack channel ID'**
  String get defaultSlackChannelIdLabel;

  /// Validation error when Slack channel ID is empty
  ///
  /// In en, this message translates to:
  /// **'Please enter Slack channel ID'**
  String get slackChannelIdRequired;

  /// Explanatory text above the external Slack delivery URL field
  ///
  /// In en, this message translates to:
  /// **'If your Slack delivery is offloaded to an external application, please specify the details here.'**
  String get externalSlackDeliveryMessage;

  /// Label for the external Slack delivery URL field, showing valid URL prefixes
  ///
  /// In en, this message translates to:
  /// **'External Slack message delivery service (optional, valid prefixes {prefixes})'**
  String externalSlackDeliveryUrlLabel(String prefixes);

  /// Validation error when the external delivery URL does not match an allowed prefix
  ///
  /// In en, this message translates to:
  /// **'You must choose a valid URL prefix'**
  String get invalidUrlPrefix;

  /// Tooltip on editable cells in the delivery headers table
  ///
  /// In en, this message translates to:
  /// **'Click to edit'**
  String get clickToEdit;

  /// Action button to reveal an encrypted value in the table
  ///
  /// In en, this message translates to:
  /// **'Show'**
  String get showAction;

  /// Action button to clear a value in the table (removes encryption flag)
  ///
  /// In en, this message translates to:
  /// **'Clear'**
  String get clearAction;

  /// Action button to decrypt an encrypted value in the table
  ///
  /// In en, this message translates to:
  /// **'Decrypt'**
  String get decryptAction;

  /// Action button to mark a value as encrypted in the table
  ///
  /// In en, this message translates to:
  /// **'Encrypt'**
  String get encryptAction;

  /// Button above the table to add a new row, where name is the key column header
  ///
  /// In en, this message translates to:
  /// **'Add {name}'**
  String addRowButton(String name);

  /// Column header label for the key column in the delivery headers table
  ///
  /// In en, this message translates to:
  /// **'Header'**
  String get headerColumnLabel;

  /// Column header label for the value column in the delivery headers table
  ///
  /// In en, this message translates to:
  /// **'Value'**
  String get valueColumnLabel;

  /// Hint text for the environment multi-select dropdown on the features data table
  ///
  /// In en, this message translates to:
  /// **'Select environments to display'**
  String get selectEnvironmentsToDisplay;

  /// The 'thing' label for the delete dialog for an application
  ///
  /// In en, this message translates to:
  /// **'application \'{name}\''**
  String appThingLabel(String name);

  /// Snackbar after deleting an application
  ///
  /// In en, this message translates to:
  /// **'Application \'{name}\' deleted!'**
  String appDeleted(String name);

  /// Error when deleting an application fails
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t delete application {name}'**
  String appDeleteError(String name);

  /// Status text for an undelivered webhook
  ///
  /// In en, this message translates to:
  /// **'undelivered'**
  String get undelivered;

  /// Title of the webhook detail dialog
  ///
  /// In en, this message translates to:
  /// **'Webhook details'**
  String get webhookDetailsTitle;

  /// Tooltip on the info icon in the webhook table
  ///
  /// In en, this message translates to:
  /// **'View webhook details'**
  String get viewWebhookDetails;

  /// Checkbox label to enable JSON validation in the JSON editor
  ///
  /// In en, this message translates to:
  /// **'Enable JSON validation'**
  String get enableJsonValidation;

  /// Button to pretty-print JSON in the JSON editor
  ///
  /// In en, this message translates to:
  /// **'Format json'**
  String get formatJson;

  /// Label above the JSON text editor area
  ///
  /// In en, this message translates to:
  /// **'JSON Value'**
  String get jsonValue;

  /// Error message for HTTP 404
  ///
  /// In en, this message translates to:
  /// **'The requested resource was not found'**
  String get errorNotFound;

  /// Error message for HTTP 403
  ///
  /// In en, this message translates to:
  /// **'You do not have permission to access this resource'**
  String get errorForbidden;

  /// Error message for HTTP 500
  ///
  /// In en, this message translates to:
  /// **'An internal server error occurred'**
  String get errorInternalServer;

  /// Generic error message when data fails to load
  ///
  /// In en, this message translates to:
  /// **'An error occurred while loading the data'**
  String get errorLoadingData;

  /// Label above the portfolio selector in the drawer
  ///
  /// In en, this message translates to:
  /// **'Your current portfolio'**
  String get yourCurrentPortfolio;

  /// Hint text for the portfolio selector dropdown
  ///
  /// In en, this message translates to:
  /// **'Select portfolio'**
  String get selectPortfolio;

  /// Hint text for the application selector dropdown
  ///
  /// In en, this message translates to:
  /// **'Select application'**
  String get selectApplication;

  /// Label when a boolean feature flag is enabled
  ///
  /// In en, this message translates to:
  /// **'ON'**
  String get featureOn;

  /// Label when a boolean feature flag is disabled
  ///
  /// In en, this message translates to:
  /// **'OFF'**
  String get featureOff;

  /// Placeholder when a feature value has not been set
  ///
  /// In en, this message translates to:
  /// **'not set'**
  String get notSet;

  /// Checkbox label for retiring a feature value
  ///
  /// In en, this message translates to:
  /// **'Retired'**
  String get retired;

  /// The 'thing' label for the delete dialog for a feature
  ///
  /// In en, this message translates to:
  /// **'feature \'{name}\''**
  String featureThingLabel(String name);

  /// Body text of the delete-feature confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'You need to make sure all your code is cleaned up and can deal without this feature!\n\nThis cannot be undone!'**
  String get featureDeleteContent;

  /// Snackbar after deleting a feature
  ///
  /// In en, this message translates to:
  /// **'Feature \'{name}\' deleted!'**
  String featureDeleted(String name);

  /// Error when the user lacks permission for an operation
  ///
  /// In en, this message translates to:
  /// **'You don\'t have permissions to perform this operation'**
  String get noPermissionsForOperation;

  /// Error when deleting a feature fails
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t delete feature {name}'**
  String featureDeleteError(String name);

  /// Body text of the delete-feature-group confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'This action will delete a feature group and a strategy associated with it.\n\nThe features will not be deleted and remain present in your system.\n\nThis cannot be undone!'**
  String get featureGroupDeleteContent;

  /// Snackbar after deleting a feature group
  ///
  /// In en, this message translates to:
  /// **'Feature group \'{name}\' deleted!'**
  String featureGroupDeleted(String name);

  /// Error when deleting a feature group fails
  ///
  /// In en, this message translates to:
  /// **'Couldn\'t delete feature group {name}'**
  String featureGroupDeleteError(String name);

  /// Message in the environment dropdown when none exist
  ///
  /// In en, this message translates to:
  /// **'No environments available'**
  String get noEnvironmentsAvailable;

  /// Hint text for the features dropdown in the feature groups editor
  ///
  /// In en, this message translates to:
  /// **'Select feature to add'**
  String get selectFeatureToAdd;

  /// Heading on setup page 1
  ///
  /// In en, this message translates to:
  /// **'Lets get this party started!'**
  String get setupWelcomeTitle;

  /// Subtitle on setup page 1 explaining the super admin role
  ///
  /// In en, this message translates to:
  /// **'Well done, FeatureHub is up and running.  You\'ll be the first \'Organization super admin\' of your FeatureHub account.'**
  String get setupWelcomeMessage;

  /// Divider label between SSO and local registration on setup page 1
  ///
  /// In en, this message translates to:
  /// **'or register by providing the details below'**
  String get setupOrRegisterBelow;

  /// Button label to advance to the next step
  ///
  /// In en, this message translates to:
  /// **'Next'**
  String get next;

  /// Title of the final setup confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'All set!'**
  String get setupAllSet;

  /// Body of the final setup confirmation dialog
  ///
  /// In en, this message translates to:
  /// **'Next step is to create your first application and add some features. Your first environment called \"Production\" will be created by default. You can follow the \"Quick Setup\" helper by clicking the \"rocket\" icon on the right of the app bar to see your progress.'**
  String get setupNextStepsMessage;

  /// Title of the quick-setup progress stepper panel
  ///
  /// In en, this message translates to:
  /// **'Application setup progress'**
  String get stepperTitle;

  /// Step 1 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Create application'**
  String get stepCreateApplication;

  /// Helper text under the application dropdown in step 1
  ///
  /// In en, this message translates to:
  /// **'Select application or create a new one by following the link below'**
  String get stepSelectApplicationHint;

  /// Link button in stepper step 1
  ///
  /// In en, this message translates to:
  /// **'Go to Applications'**
  String get goToApplications;

  /// Step 2 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Create team group'**
  String get stepCreateTeamGroup;

  /// Helper text in stepper step 2
  ///
  /// In en, this message translates to:
  /// **'Groups are portfolio-wide, we recommend creating application specific groups eg \"MyApp developers\"'**
  String get stepCreateTeamGroupHint;

  /// Link button in stepper step 2
  ///
  /// In en, this message translates to:
  /// **'Go to Groups'**
  String get goToGroups;

  /// Step 3 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Create service account'**
  String get stepCreateServiceAccount;

  /// Helper text in stepper step 3
  ///
  /// In en, this message translates to:
  /// **'Service accounts are portfolio-wide, we recommend creating at least two service accounts specific to an application, e.g. \"SA-MyApp-Prod\" and \"SA-MyApp-Non-Prod\"'**
  String get stepCreateServiceAccountHint;

  /// Link button in stepper step 3
  ///
  /// In en, this message translates to:
  /// **'Go to Service Accounts'**
  String get goToServiceAccounts;

  /// Step 4 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Create environment'**
  String get stepCreateEnvironment;

  /// Helper text in stepper step 4
  ///
  /// In en, this message translates to:
  /// **'Create an environment for selected application, e.g. \"test\", \"dev\", \"prod\"'**
  String get stepCreateEnvironmentHint;

  /// Link button in stepper step 4
  ///
  /// In en, this message translates to:
  /// **'Go to Environments'**
  String get goToEnvironments;

  /// Step 5 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Give access to groups'**
  String get stepGiveAccessToGroups;

  /// Helper text in stepper step 5
  ///
  /// In en, this message translates to:
  /// **'Assign an application environment level permissions to a group of users'**
  String get stepGiveAccessToGroupsHint;

  /// Link button in stepper step 5
  ///
  /// In en, this message translates to:
  /// **'Go to Group Permissions'**
  String get goToGroupPermissions;

  /// Step 6 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Give access to service account'**
  String get stepGiveAccessToServiceAccount;

  /// Helper text in stepper step 6
  ///
  /// In en, this message translates to:
  /// **'Assign an application environment level permissions to a service account'**
  String get stepGiveAccessToServiceAccountHint;

  /// Link button in stepper step 6
  ///
  /// In en, this message translates to:
  /// **'Go to SA Permissions'**
  String get goToSAPermissions;

  /// Step 7 title in the setup stepper
  ///
  /// In en, this message translates to:
  /// **'Create a feature'**
  String get stepCreateFeature;

  /// Helper text in stepper step 7
  ///
  /// In en, this message translates to:
  /// **'Create a feature for an application'**
  String get stepCreateFeatureHint;

  /// Link button in stepper step 7
  ///
  /// In en, this message translates to:
  /// **'Go to Features'**
  String get goToFeatures;

  /// Chip label for the site-wide admin group in the portfolio group selector
  ///
  /// In en, this message translates to:
  /// **'FeatureHub Administrators'**
  String get featureHubAdministrators;

  /// Label on the portfolio dropdown in the portfolio group selector
  ///
  /// In en, this message translates to:
  /// **'Portfolio'**
  String get portfolioLabel;

  /// Badge between strategy attribute rules indicating logical AND
  ///
  /// In en, this message translates to:
  /// **'AND'**
  String get andOperator;

  /// Snackbar after a system config section is saved
  ///
  /// In en, this message translates to:
  /// **'{section} was successfully updated'**
  String systemConfigUpdated(String section);

  /// Snackbar when saving system config with no changes
  ///
  /// In en, this message translates to:
  /// **'No updates for {section} found'**
  String systemConfigNoUpdates(String section);

  /// Error message when saving system config fails
  ///
  /// In en, this message translates to:
  /// **'Unable to save updates'**
  String get unableToSaveUpdates;

  /// Button to reveal an encrypted text field value
  ///
  /// In en, this message translates to:
  /// **'Show value'**
  String get showValue;

  /// Validation: strategy name exceeds max length
  ///
  /// In en, this message translates to:
  /// **'Strategy name is too long'**
  String get strategyNameTooLong;

  /// Validation: strategy has no attribute rules
  ///
  /// In en, this message translates to:
  /// **'You have not provided any rules to match against, please add a rule'**
  String get strategyEmptyMatchCriteria;

  /// Validation: strategy percentage is negative
  ///
  /// In en, this message translates to:
  /// **'Percentage cannot be a negative number'**
  String get strategyNegativePercentage;

  /// Validation: total strategy percentage exceeds 100%
  ///
  /// In en, this message translates to:
  /// **'The total percentage value across all strategies is above 100%, please decrease the percentage rule'**
  String get strategyPercentageOver100;

  /// Validation: array attribute rule has no values
  ///
  /// In en, this message translates to:
  /// **'Please provide at least one value for this rule'**
  String get strategyArrayAttributeNoValues;

  /// Validation: well-known enum attribute rule has invalid value
  ///
  /// In en, this message translates to:
  /// **'Please select a value for this rule'**
  String get strategyAttrInvalidWellKnownEnum;

  /// Validation: attribute rule is missing a value
  ///
  /// In en, this message translates to:
  /// **'Please provide a value for this rule'**
  String get strategyAttrMissingValue;

  /// Validation: attribute rule has no condition selected
  ///
  /// In en, this message translates to:
  /// **'Please select a matching condition for this rule'**
  String get strategyAttrMissingConditional;

  /// Validation: custom attribute rule has no field name
  ///
  /// In en, this message translates to:
  /// **'Please enter the rule name'**
  String get strategyAttrMissingFieldName;

  /// Validation: attribute rule has no field type selected
  ///
  /// In en, this message translates to:
  /// **'Please select a value type for this rule'**
  String get strategyAttrMissingFieldType;

  /// Validation: attribute value is not a valid semantic version
  ///
  /// In en, this message translates to:
  /// **'Please provide a valid semantic version'**
  String get strategyAttrValNotSemanticVersion;

  /// Validation: attribute value is not a valid number
  ///
  /// In en, this message translates to:
  /// **'Please provide a valid number'**
  String get strategyAttrValNotNumber;

  /// Validation: attribute value is not a valid date
  ///
  /// In en, this message translates to:
  /// **'Please provide a valid date in YYYY-MM-DD format'**
  String get strategyAttrValNotDate;

  /// Validation: attribute value is not a valid datetime
  ///
  /// In en, this message translates to:
  /// **'Please provide a valid date and time in YYYY-MM-DDTHH:MM:SS format'**
  String get strategyAttrValNotDateTime;

  /// Validation: attribute value is not a valid IP/CIDR
  ///
  /// In en, this message translates to:
  /// **'Please provide a valid IP or CIDR address'**
  String get strategyAttrValNotCidr;

  /// Validation: unknown strategy error
  ///
  /// In en, this message translates to:
  /// **'There was an unknown strategy validation error'**
  String get strategyAttrUnknownFailure;

  /// Label for the default (no-name) strategy card
  ///
  /// In en, this message translates to:
  /// **'default'**
  String get strategyDefault;

  /// Label preceding the served value in a strategy card
  ///
  /// In en, this message translates to:
  /// **'serve'**
  String get strategyServe;

  /// Tooltip on the navigate-to-strategy-settings button in a strategy card
  ///
  /// In en, this message translates to:
  /// **'Edit Strategy Settings'**
  String get editStrategySettings;

  /// Hint text for the application strategies dropdown
  ///
  /// In en, this message translates to:
  /// **'Select strategy to add'**
  String get selectStrategyToAdd;

  /// Display label for the Country well-known attribute in strategy editor
  ///
  /// In en, this message translates to:
  /// **'Country'**
  String get wellKnownCountry;

  /// Display label for the Device well-known attribute in strategy editor
  ///
  /// In en, this message translates to:
  /// **'Device'**
  String get wellKnownDevice;

  /// Display label for the Platform well-known attribute in strategy editor
  ///
  /// In en, this message translates to:
  /// **'Platform'**
  String get wellKnownPlatform;

  /// Display label for the Version well-known attribute in strategy editor
  ///
  /// In en, this message translates to:
  /// **'Version'**
  String get wellKnownVersion;

  /// Display label for the User Key well-known attribute in strategy editor
  ///
  /// In en, this message translates to:
  /// **'User Key'**
  String get wellKnownUserKey;

  /// Section header in the strategy tooltip listing which rules apply
  ///
  /// In en, this message translates to:
  /// **'Applied rules'**
  String get tooltipAppliedRules;

  /// Tooltip line showing the percentage rollout value
  ///
  /// In en, this message translates to:
  /// **'Percentage: {value}%'**
  String tooltipPercentage(String value);

  /// Tooltip line indicating a user-key rule is applied
  ///
  /// In en, this message translates to:
  /// **'User key'**
  String get tooltipUserKey;

  /// Tooltip line indicating a country rule is applied
  ///
  /// In en, this message translates to:
  /// **'Country'**
  String get tooltipCountry;

  /// Tooltip line indicating a platform rule is applied
  ///
  /// In en, this message translates to:
  /// **'Platform'**
  String get tooltipPlatform;

  /// Tooltip line indicating a device rule is applied
  ///
  /// In en, this message translates to:
  /// **'Device'**
  String get tooltipDevice;

  /// Tooltip line indicating a version rule is applied
  ///
  /// In en, this message translates to:
  /// **'Version'**
  String get tooltipVersion;

  /// Tooltip line indicating a custom attribute rule is applied
  ///
  /// In en, this message translates to:
  /// **'Custom'**
  String get tooltipCustom;

  /// Banner shown in the feature value side sheet when there are unsaved changes
  ///
  /// In en, this message translates to:
  /// **'You have unsaved changes, save?'**
  String get unsavedChanges;

  /// Snackbar after saving a feature value
  ///
  /// In en, this message translates to:
  /// **'Feature {feature} in the environment {environment} has been updated!'**
  String featureValueUpdated(String feature, String environment);

  /// Section label above the default strategy card in the feature value editor
  ///
  /// In en, this message translates to:
  /// **'Default value'**
  String get defaultValue;

  /// Section heading for custom rollout strategy variations in the feature value editor
  ///
  /// In en, this message translates to:
  /// **'Strategy variations'**
  String get strategyVariations;

  /// Info tooltip describing how strategy variations work
  ///
  /// In en, this message translates to:
  /// **'Add a strategy variation to serve a value other than default. You can change strategies evaluation order by dragging and dropping the cards below. Strategies are evaluated in order from top to bottom. Evaluation stops when it hits a matching strategy. \'Group Strategy\' evaluation comes last. If no strategies match, then \'default\' feature value is served.'**
  String get strategyVariationsInfo;

  /// Placeholder when no rollout strategy variations have been added
  ///
  /// In en, this message translates to:
  /// **'No strategies set'**
  String get noStrategiesSet;

  /// Section heading for feature group strategy variations
  ///
  /// In en, this message translates to:
  /// **'Group strategy variations'**
  String get groupStrategyVariations;

  /// Info tooltip describing group strategy variations
  ///
  /// In en, this message translates to:
  /// **'Feature groups are recommended when you want to set the same strategy for multiple features in the same environment. Feature group strategy can be created and edited from the Feature Groups page.'**
  String get groupStrategyVariationsInfo;

  /// Placeholder when no group strategies have been assigned
  ///
  /// In en, this message translates to:
  /// **'No group strategies set'**
  String get noGroupStrategiesSet;

  /// Section heading for application-level strategy variations
  ///
  /// In en, this message translates to:
  /// **'Application strategy variations'**
  String get applicationStrategyVariations;

  /// Info tooltip describing application strategy variations
  ///
  /// In en, this message translates to:
  /// **'Application strategies are created at application level and can be assigned to multiple features in any environment. Application strategy can be created and edited from the Application Strategies page.'**
  String get applicationStrategyVariationsInfo;

  /// Placeholder when no application strategies have been assigned
  ///
  /// In en, this message translates to:
  /// **'No application strategies set'**
  String get noApplicationStrategiesSet;

  /// Button to reveal the list of available application strategies to add
  ///
  /// In en, this message translates to:
  /// **'Show available app strategies'**
  String get showAvailableAppStrategies;

  /// Button label to add an application strategy to a feature value
  ///
  /// In en, this message translates to:
  /// **'Add Strategy'**
  String get addStrategy;

  /// Section label for the retire feature value checkbox
  ///
  /// In en, this message translates to:
  /// **'Retired status'**
  String get retiredStatus;

  /// Info tooltip explaining the retired feature status
  ///
  /// In en, this message translates to:
  /// **'When feature flag is not needed any longer in your application, and ready to be removed, you can first \'retire\' this feature in a given environment to test how your application behaves. This means that the feature won\'t be visible by the SDKs, imitating the \'deleted\' state. You can uncheck the box to \'un-retire\' a feature if you change your mind as this operation is reversible. Once you retire feature values across all the environments and test that your application behaves as expected, you can delete your entire feature.'**
  String get retiredStatusInfo;

  /// Button to collapse the feature value change history
  ///
  /// In en, this message translates to:
  /// **'Hide history'**
  String get hideHistory;

  /// Button to expand the feature value change history
  ///
  /// In en, this message translates to:
  /// **'Show history'**
  String get showHistory;

  /// Caption above the history table indicating only the last 20 entries are shown
  ///
  /// In en, this message translates to:
  /// **'Showing last 20'**
  String get showingLast20;

  /// Column header for the timestamp column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Timestamp (UTC)'**
  String get historyColumnTimestamp;

  /// Column header for the name column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get historyColumnName;

  /// Column header for the email column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Email'**
  String get historyColumnEmail;

  /// Column header for the type column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Type'**
  String get historyColumnType;

  /// Column header for the default value column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Default Value'**
  String get historyColumnDefaultValue;

  /// Column header for the locked column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Locked'**
  String get historyColumnLocked;

  /// Column header for the retired column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Retired'**
  String get historyColumnRetired;

  /// Column header for the rollout strategies column in the feature value history table
  ///
  /// In en, this message translates to:
  /// **'Rollout Strategies'**
  String get historyColumnRolloutStrategies;

  /// History entry type label for a human user
  ///
  /// In en, this message translates to:
  /// **'User'**
  String get historyTypeUser;

  /// History entry type label for a service account
  ///
  /// In en, this message translates to:
  /// **'Service Account'**
  String get historyTypeServiceAccount;

  /// Title of the dialog showing rollout strategy rules in the history view
  ///
  /// In en, this message translates to:
  /// **'Strategy Rules'**
  String get strategyRules;

  /// Label for the percentage rollout section in the strategy rules dialog
  ///
  /// In en, this message translates to:
  /// **'Percentage Rollout'**
  String get percentageRollout;

  /// Button to show more details about a rollout strategy in the history table
  ///
  /// In en, this message translates to:
  /// **'more'**
  String get moreDetails;

  /// Tooltip label on group strategy cells in the feature value table
  ///
  /// In en, this message translates to:
  /// **'Group Strategy'**
  String get groupStrategyTooltip;

  /// Tooltip label on application strategy cells in the feature value table
  ///
  /// In en, this message translates to:
  /// **'Application Strategy'**
  String get applicationStrategyTooltip;

  /// Section label for the lock/unlock switch in the feature value side sheet
  ///
  /// In en, this message translates to:
  /// **'Locked status'**
  String get lockedStatus;

  /// Info tooltip explaining the locked status feature
  ///
  /// In en, this message translates to:
  /// **'Locking mechanism provides an additional safety for feature changes when deploying incomplete code to production. Locked status prevents any changes to default value, strategies, strategy values and \'retired\' status. Typically, developers keep features locked to indicate they are not ready to be turned on for testers, product owners, customers and other stakeholders.'**
  String get lockedStatusInfo;

  /// Tooltip on the lock button when feature is currently locked
  ///
  /// In en, this message translates to:
  /// **'Click to unlock'**
  String get clickToUnlock;

  /// Tooltip on the lock button when feature is currently unlocked
  ///
  /// In en, this message translates to:
  /// **'Click to lock'**
  String get clickToLock;

  /// Status label shown when a feature value is locked
  ///
  /// In en, this message translates to:
  /// **'Feature is locked and cannot be changed'**
  String get featureIsLocked;

  /// Status label shown when a feature value is unlocked
  ///
  /// In en, this message translates to:
  /// **'Feature is unlocked and can be changed'**
  String get featureIsUnlocked;

  /// Column header for the features name column in the dashboard table
  ///
  /// In en, this message translates to:
  /// **'Features'**
  String get featuresColumnHeader;

  /// Placeholder shown when no features match the current filter
  ///
  /// In en, this message translates to:
  /// **'No features to display'**
  String get noFeaturesToDisplay;

  /// Message shown when the feature table has no environment columns
  ///
  /// In en, this message translates to:
  /// **'Either there are no environments defined for this application or you don\'t have permissions to access any of them'**
  String get noEnvironmentsForApp;

  /// Link button to navigate to the app environments settings tab
  ///
  /// In en, this message translates to:
  /// **'Go to environments settings'**
  String get goToEnvironmentsSettings;

  /// Message shown when the selected application has no features
  ///
  /// In en, this message translates to:
  /// **'There are no features defined for this application'**
  String get noFeaturesForApp;

  /// Checkbox label to grant a user organisation-wide super admin rights
  ///
  /// In en, this message translates to:
  /// **'Set this user as organization super admin'**
  String get setAsOrgSuperAdmin;

  /// Tooltip on the copy-to-clipboard icon in the feature name cell
  ///
  /// In en, this message translates to:
  /// **'Copy feature key to clipboard'**
  String get copyFeatureKeyToClipboard;

  /// Popup menu item to edit a feature's details
  ///
  /// In en, this message translates to:
  /// **'Edit details'**
  String get editDetails;

  /// Popup menu item to view a feature's details (read-only)
  ///
  /// In en, this message translates to:
  /// **'View details'**
  String get viewDetails;

  /// Popup menu item to edit a feature's metadata
  ///
  /// In en, this message translates to:
  /// **'Edit metadata'**
  String get editMetadata;

  /// Popup menu item to view a feature's metadata (read-only)
  ///
  /// In en, this message translates to:
  /// **'View metadata'**
  String get viewMetadata;

  /// Popup menu item to navigate to the feature group management page
  ///
  /// In en, this message translates to:
  /// **'Manage Group'**
  String get manageGroup;

  /// Label for the strategy status in the feature group card
  ///
  /// In en, this message translates to:
  /// **'Strategy'**
  String get strategy;
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
