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
