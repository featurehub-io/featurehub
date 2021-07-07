import 'dart:async';
import 'dart:convert';

import 'package:open_admin_app/api/identity_providers.dart';
import 'package:open_admin_app/api/router.dart';
import 'package:open_admin_app/api/web_interface/url_handler.dart';
import 'package:open_admin_app/api/web_interface/url_handler_stub.dart'
    if (dart.library.io) 'package:open_admin_app/api/web_interface/io_url_handler.dart'
    if (dart.library.html) 'package:open_admin_app/api/web_interface/web_url_handler.dart';
import 'package:open_admin_app/common/fh_shared_prefs.dart';
import 'package:open_admin_app/common/person_state.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:open_admin_app/config/routes.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';
import 'package:rxdart/rxdart.dart';

///
/// This represents the state of the whole application, which starts off in 'unknown',
/// makes a call to the backend which will (if found) return that it is initialized
/// or uninitialized. Initialized transitions to a  login page, Uninitialized transitions
/// to a setup page, which then transitions to initialized by calling the same method below.
///
/// When they are in the 'logged_in' state, we have a bearer token and information about the
/// organisation and their basic role within it (admin or not).
///

// this was an enum, but it is too restrictive
typedef InitializedCheckState = String;

final preRouterStateUninitialized = 'uninitialized';
final preRouterStateInitialized = 'initialized';
final preRouterStateUnknown = 'unknown';
final preRouterStateRequiresPasswordReset = 'requires_password_reset';
final preRouterStateZombie = 'zombie';

// if true then if we find we are on localhost, we redirect to 8903 for api calls
bool overrideOrigin = true;

typedef TokenizedPersonHook = void Function(TokenizedPerson person);

List<TokenizedPersonHook> tokenizedPersonHooks = <TokenizedPersonHook>[];

final _log = Logger('mr_bloc');

///
/// This is the overall master BLoC that controls the state of the main application and holds
/// the authentication token. Mini-BLoC's will pop up through the code (such as in Setup) where
/// it may need access to this configuration, but otherwise its state is not interesting to the whole
/// application.
///
///
class ManagementRepositoryClientBloc implements Bloc {
  final ApiClient _client;
  late PersonState personState;
  late FHSharedPrefs sharedPreferences;

  late SetupServiceApi setupApi;
  late PersonServiceApi personServiceApi;
  late AuthServiceApi authServiceApi;
  late PortfolioServiceApi portfolioServiceApi;
  late ServiceAccountServiceApi serviceAccountServiceApi;
  late EnvironmentServiceApi environmentServiceApi;
  late FeatureServiceApi featureServiceApi;
  late ApplicationServiceApi applicationServiceApi;
  static late FHRouter router;

  // this reflects actual requests to change the route driven externally, so a user clicks on
  // something that should cause the page to change to this route.
  final _routerSource = BehaviorSubject<RouteChange?>();

  // this is when route change events are being notified of downstream systems but don't cause
  // an actual change in route (usually because this has already happened such as on tab changes
  // the Applications page is like this. The tabs cause changes in routes that are not actual route
  // changes otherwise they would endlessly cause the page to redraw.
  final _routerExternalSource = PublishSubject<RouteChange>();

  // this represents the current route state. When _routerSource changes it should push to here
  // and when _routerExternalSource changes, it should push to here
  final _routerCollectedSource = BehaviorSubject<RouteChange?>();
  final _routerRedrawRouteSource = BehaviorSubject<RouteChange?>();
  final _menuOpened = BehaviorSubject<bool>.seeded(true);
  final _stepperOpened = BehaviorSubject<bool>.seeded(false);
  late Uri _basePath;
  late StreamSubscription<Portfolio?> _personPermissionInPortfolioChanged;
  late IdentityProviders identityProviders;

  BehaviorSubject<bool> get stepperOpened => _stepperOpened;

  set stepperOpened(value) {
    _stepperOpened.add(value);
  }

  BehaviorSubject<bool> get menuOpened => _menuOpened;

  set menuOpened(value) {
    if (personState.isLoggedIn && value || !value) {
      _menuOpened.add(value);
    }
  }

  Stream<RouteChange?> get routeCurrentStream => _routerCollectedSource.stream;
  Stream<RouteChange?> get routeChangedStream => _routerSource.stream;
  Stream<RouteChange?> get redrawChangedStream =>
      _routerRedrawRouteSource.stream;

  RouteChange? get currentRoute => _routerSource.value;

  void swapRoutes(RouteChange route) {
    // this is for gross route changes, and causes the widget to redraw
    // for multi-tabbed routes, we don't want this to happen, so we separate the two
    if (_routerRedrawRouteSource.value == null ||
        _routerRedrawRouteSource.value?.route != route.route) {
      _routerRedrawRouteSource.add(route);
    }

    // this is for fine grained route changes, like tab changes
    _routerSource.add(route);
    sharedPreferences.saveString('current-route', route.toJson());
  }

  void _initializeRouteStreams() {
    _routerSource.listen((value) {
      if (value != null) _routerCollectedSource.add(value);
    });

    _routerExternalSource.listen((value) {
      _routerCollectedSource.add(value);
    });
  }

  // called when something else has caused the widget redrawing (like tabs) and
  // we need to notify things that are tracking the routes (e.g. the menu) that
  // the route has actually changed.
  void notifyExternalRouteChange(RouteChange rc) {
    _routerExternalSource.add(rc);
  }

  /// we changed permission, or at least changed portfolio, so check if we
  /// still have permission to this route and if not, go to the default route
  void _checkRouteForPermission(Portfolio p) {
    if (_routerSource.hasValue) {
      if (!router.hasRoutePermissions(_routerSource.value!, userIsSuperAdmin,
          personState.userIsPortfolioAdmin(p.id!, person.groups))) {
        swapRoutes(router.defaultRoute());
      }
    }

    personState.currentPortfolioOrSuperAdminUpdateState(p);
  }

  Future<void> _setCurrentRoute() async {
    try {
      var currentRoute = await sharedPreferences.getString('current-route');
      if (currentRoute != null) {
        _routerSource.add(RouteChange.fromJson(currentRoute));
      }
      // ignore: empty_catches
    } catch (e) {}
    ;
  }

  bool get isLoggedIn => personState.isLoggedIn;

  Stream<InitializedCheckState> get initializedState =>
      _initializedSource.stream;
  final _initializedSource = BehaviorSubject<InitializedCheckState>();

  void replaceSuperState(InitializedCheckState ics) {
    _initializedSource.add(ics);
  }

  Stream<FHError?> get errorStream => _errorSource.stream;
  final _errorSource = PublishSubject<FHError?>();

  Stream<WidgetBuilder?> get overlayStream => _overlaySource.stream;
  final _overlaySource = PublishSubject<WidgetBuilder?>();

  Stream<Widget?> get snackbarStream => _snackbarSource.stream;
  final _snackbarSource = PublishSubject<Widget?>();

  Stream<Person> get personStream => personState.personStream;

  Organization? organization;

  Person get person => personState.person;

  bool get userIsSuperAdmin => personState.userIsSuperAdmin;

  bool get userIsFeatureAdminOfCurrentApplication {
    final currentAid = getCurrentAid();

    return person.groups.any((g) => g.applicationRoles.any((ar) =>
            ar.applicationId == currentAid &&
            ar.roles.contains(ApplicationRoleType.FEATURE_EDIT))) ==
        true;
  }

  bool get userIsCurrentPortfolioAdmin =>
      personState.userIsCurrentPortfolioAdmin;

  bool get userIsAnyPortfolioOrSuperAdmin =>
      personState.userIsAnyPortfolioOrSuperAdmin;

  String? get currentPid => getCurrentPid();

  String? get currentAid => getCurrentAid();

  set currentAid(String? aid) => setCurrentAid(aid);

  set currentPid(String? pid) => setCurrentPid(pid);

  Portfolio? get currentPortfolio {
    return streamValley.currentPortfolio;
  }

  late StreamValley streamValley;

  static AbstractWebInterface webInterface = getUrlAuthInstance();

  static String homeUrl() {
    return webInterface.homeUrl(overrideOrigin);
  }

  ManagementRepositoryClientBloc({String? basePathUrl})
      : _client = ApiClient(basePath: basePathUrl ?? homeUrl()) {
    _basePath = Uri.parse(_client.basePath);
    webInterface.setOrigin();

    _client.passErrorsAsApiResponses = true;

    initializeApis(_client);
  }

  void setupFeaturehubClientApis(ApiClient client) {
    setupApi = SetupServiceApi(client);
    personServiceApi = PersonServiceApi(client);

    authServiceApi = AuthServiceApi(client);

    portfolioServiceApi = PortfolioServiceApi(client);
    serviceAccountServiceApi = ServiceAccountServiceApi(client);
    environmentServiceApi = EnvironmentServiceApi(client);
    personState = PersonState(personServiceApi);
    featureServiceApi = FeatureServiceApi(client);
    applicationServiceApi = ApplicationServiceApi(client);
    _errorSource.add(null);
    streamValley = StreamValley(this, personState);

    _personPermissionInPortfolioChanged = streamValley.routeCheckPortfolioStream
        .listen((portfolio) =>
            {if (portfolio != null) _checkRouteForPermission(portfolio)});

    _initializeRouteStreams();
  }

  void initializeApis(ApiClient client) {
    router = Routes.configureRoutes(this);

    identityProviders = setupIdentityProviders(this);

    setupFeaturehubClientApis(client);

    init();
  }

  ApiClient get apiClient => _client;

  IdentityProviders setupIdentityProviders(
      ManagementRepositoryClientBloc bloc) {
    return IdentityProviders(this, _client);
  }

  Future<void> init() async {
    sharedPreferences = await FHSharedPrefs.getSharedInstance();
    await _setCurrentRoute();
    await isInitialized();
  }

  Future isInitialized() async {
    if (personState.isLoggedIn) {
      _initializedSource.add(preRouterStateZombie);
      return;
    }

    await setupApi.isInstalled().then((setupResponse) {
      // if we are initialized, check for an existing cookie
      // to see if we have a bearer token. This would mean the user
      // has simply refreshed their page

      final bearerToken = getBearerCookie();
      organization = setupResponse.organization;
      identityProviders.identityProviders = setupResponse.providers;
      if (bearerToken != null) {
        setBearerToken(bearerToken);
        requestOwnDetails();
      } else if (setupResponse.redirectUrl != null) {
        // they can only authenticate via one provider, so lets use them
        webInterface.authenticateViaProvider(setupResponse.redirectUrl!);
      } else {
        _initializedSource.add(preRouterStateInitialized);
      }
    }).catchError((e, s) {
      if (e is ApiException) {
        if (e.code == 404) {
          final smr = LocalApiClient.deserialize(
                  jsonDecode(e.message!), 'SetupMissingResponse')
              as SetupMissingResponse;
          identityProviders.identityProviders = smr.providers;
          _initializedSource.add(preRouterStateUninitialized);
        } else {
          dialogError(e, s);
        }
      }
    });
  }

  String? getBearerCookie() {
    return webInterface.getStoredAuthToken();
  }

  void _setBearerCookie(String token) {
    webInterface.setStoredAuthToken(token);
  }

  void _clearBearerCookie() {
    webInterface.clearStoredAuthToken();
  }

  // ask for my own details and if there are some, set the person and transition
  // to logged in, otherwise ask them to log in.
  Future requestOwnDetails() async {
    return personServiceApi
        .getPerson('self', includeAcls: true, includeGroups: true)
        .then((p) {
      setPerson(p);
      if (_initializedSource.value != preRouterStateZombie) {
        _initializedSource.add(preRouterStateZombie);
      }
    }).catchError((_) {
      setBearerToken(null);
      _initializedSource.add(preRouterStateInitialized);
    });
  }

  Future logout() async {
    await authServiceApi.logout();
    _initializedSource.add(preRouterStateInitialized);
    setBearerToken(null);
    personState.logout();
    menuOpened.add(false);
    currentPid = null;
    currentAid = null;
  }

  void setOrg(Organization o) {
    organization = o;
    _initializedSource.add(preRouterStateZombie);
  }

  void setBearerToken(String? token) {
    _client.setAuthentication('bearerAuth', OAuth(accessToken: token));

    if (token == null) {
      _clearBearerCookie();
    } else {
      _setBearerCookie(token);
    }
  }

  void setCurrentAid(String? aid) {
    streamValley.currentAppId = aid;
    _setAidSharedPrefs(aid);
  }

  void setCurrentPid(String? pid) {
    // do this first so that the permissions are set up
    streamValley.currentPortfolioId = pid;
    _setAidSharedPrefs(null);
    _setPidSharedPrefs(pid);
  }

  String? getCurrentPid() {
    return streamValley.currentPortfolioId;
  }

  String? getCurrentAid() {
    return streamValley.currentAppId;
  }

  void setPerson(Person p) {
    personState.person = p;
    _addPortfoliosToStream();
  }

  bool isPortfolioOrSuperAdminForCurrentPid() {
    return currentPid == null ? false : isPortfolioOrSuperAdmin(currentPid!);
  }

  bool isPortfolioOrSuperAdmin(String? pid) {
    return personState.userIsSuperAdmin ||
        personState.userIsPortfolioAdmin(pid);
  }

  void addOverlay(WidgetBuilder builder) {
    _overlaySource.add(builder);
  }

  void removeOverlay() {
    _overlaySource.add(null);
  }

  void addError(FHError? error) {
    _errorSource.add(error);
  }

  void addSnackbar(Widget? content) {
    _snackbarSource.add(content);
  }

  void customError({String messageTitle = 'failure', String messageBody = ''}) {
    addError(FHError(messageTitle,
        exception: null, showDetails: false, errorMessage: messageBody));
  }

  Future<void> dialogError(e, StackTrace? s,
      {String? messageTitle,
      bool showDetails = true,
      String messageBody = ''}) async {
    _log.warning(messageBody, e, s);
    if (messageTitle != null) {
      addError(FHError(messageTitle,
          exception: e,
          stackTrace: s,
          showDetails: showDetails,
          errorMessage: messageBody));
    } else {
      addError(FHError.createError(e, s, showDetails: showDetails));
    }
  }

  void consoleError(e, s) {
    _log.severe('Failed', e, s);
  }

  Future<void> login(String email, String password) async {
    await authServiceApi
        .login(UserCredentials(
      email: email,
      password: password,
    ))
        .then((tp) {
      hasToken(tp);
    });
  }

  Future hasToken(TokenizedPerson tp) async {
    final person = tp.person;

    if (person == null) {
      return;
    }

    setBearerToken(tp.accessToken);

    final previousPerson = await lastUsername();

    // if we are swapping users, remove all shared preferences (including last portfolio, route, etc)
    if (person.email != previousPerson) {
      await sharedPreferences.clear();
    }

    setLastUsername(person.email!);

    setPerson(person);

    if (person.passwordRequiresReset == true) {
      _initializedSource.add(preRouterStateRequiresPasswordReset);
    } else {
      _initializedSource.add(preRouterStateZombie);
    }
  }

  Future<void> replaceTempPassword(String password) {
    return authServiceApi
        .replaceTempPassword(
            person.id!.id,
            PasswordReset(
              password: password,
            ))
        .then((tp) {
      setBearerToken(tp.accessToken);
      _initializedSource.add(preRouterStateZombie);
    });
  }

  // this can be called when a portfolio is deleted
  void refreshPortfolios() async {
    _addPortfoliosToStream();
  }

  void _addPortfoliosToStream() async {
    try {
      final _portfolios = await streamValley.loadPortfolios();

      var foundValidStoredPortfolio = false;

      if (await sharedPreferences.getString('currentPid') != null) {
        final aid = await sharedPreferences.getString('currentAid');
        final pid = await sharedPreferences.getString('currentPid');
        if (streamValley.containsPid(pid)) {
          setCurrentPid(pid);
          foundValidStoredPortfolio = true;
          if (aid != null) {
            setCurrentAid(aid);
          }
        }
      }

      if (!foundValidStoredPortfolio && _portfolios.isNotEmpty == true) {
        setCurrentPid(_portfolios.first.id.toString());
        setCurrentAid(null);
      }
    } catch (e, s) {
      // ignore: unawaited_futures
      dialogError(e, s);
    }
  }

  @override
  void dispose() {
    _initializedSource.close();
    _errorSource.close();
    _overlaySource.close();
    _snackbarSource.close();
    _personPermissionInPortfolioChanged.cancel();
    personState.dispose();
    streamValley.currentPortfolioAdminOrSuperAdminSubscription.cancel();
  }

  void _setPidSharedPrefs(String? pid) async {
    if (pid == null) {
      await sharedPreferences.delete('currentPid');
    } else {
      await sharedPreferences.saveString('currentPid', pid);
    }
  }

  void _setAidSharedPrefs(String? aid) async {
    if (aid == null) {
      await sharedPreferences.delete('currentAid');
    } else {
      await sharedPreferences.saveString('currentAid', aid);
    }
  }

  Future<String?> lastUsername() async {
    return await sharedPreferences.getString('lastUsername');
  }

  void setLastUsername(String lastUsername) async {
    await sharedPreferences.saveString('lastUsername', lastUsername);
  }

  // if a url comes back from the backend with a back-end url, we need to rewrite it to our
  // own "window api" front end url
  String rewriteUrl(String url) {
    final uri = Uri.parse(url);

    if (uri.host == _basePath.host && uri.port == _basePath.port) {
      return uri
          .replace(
              host: webInterface.originUri!.host,
              port: webInterface.originUri!.port)
          .toString();
    }

    return url;
  }

  String registrationUrl(String token) {
    return Uri.base.replace(fragment: '/register-url?token=$token').toString();
  }

  void resetInitialized() {
    _initializedSource.add(preRouterStateInitialized);
  }
}
