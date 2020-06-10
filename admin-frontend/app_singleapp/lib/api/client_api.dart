import 'dart:async';
import 'dart:html';

import 'package:app_singleapp/api/router.dart';
import 'package:app_singleapp/common/fh_shared_prefs.dart';
import 'package:app_singleapp/common/person_state.dart';
import 'package:app_singleapp/common/stream_valley.dart';
import 'package:app_singleapp/utils/utils.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
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

enum InitializedCheckState {
  uninitialized,
  initialized,
  unknown,
  requires_password_reset,
  zombie
}

// if true then if we find we are on localhost, we redirect to 8903 for api calls
bool overrideOrigin = true;

///
/// This is the overall master BLoC that controls the state of the main application and holds
/// the authentication token. Mini-BLoC's will pop up through the code (such as in Setup) where
/// it may need access to this configuration, but otherwise its state is not interesting to the whole
/// application.
///
///
class ManagementRepositoryClientBloc implements Bloc {
  final ApiClient _client;
  PersonState personState;
  FHSharedPrefs _sharedPreferences;

  SetupServiceApi setupApi;
  PersonServiceApi personServiceApi;
  AuthServiceApi authServiceApi;
  PortfolioServiceApi portfolioServiceApi;
  ServiceAccountServiceApi serviceAccountServiceApi;
  EnvironmentServiceApi environmentServiceApi;
  FeatureServiceApi featureServiceApi;
  ApplicationServiceApi applicationServiceApi;
  static Router router;
  final _routerSource = BehaviorSubject<RouteChange>();
  final _routerRedrawRouteSource = BehaviorSubject<RouteChange>();
  final _menuOpened = BehaviorSubject<bool>.seeded(true);
  final _stepperOpened = BehaviorSubject<bool>.seeded(false);
  Uri _basePath;

  BehaviorSubject<bool> get stepperOpened => _stepperOpened;

  set stepperOpened(value) {
    _stepperOpened.add(value);
  }

  BehaviorSubject<bool> get menuOpened => _menuOpened;

  set menuOpened(value) {
    _menuOpened.add(value);
  }

  Stream<RouteChange> get routeChangedStream => _routerSource.stream;
  Stream<RouteChange> get redrawChangedStream =>
      _routerRedrawRouteSource.stream;

  RouteChange get currentRoute => _routerSource.value;

  void swapRoutes(RouteChange route) {
    // this is for gross route changes, and causes the widget to redraw
    // for multi-tabbed routes, we don't want this to happen, so we separate the two
    if (_routerRedrawRouteSource.value == null ||
        _routerRedrawRouteSource.value.route != route.route) {
      _routerRedrawRouteSource.add(route);
    }

    // this is for fine grained route changes, like tab changes
    _routerSource.add(route);
  }

  bool get isLoggedIn => _personSource.hasValue;

  // this gets set when a person comes into the system for the first time and the person isn't set
  // we should redirect the navigator back there once a person has been set
  String desiredRoute;

  Stream<InitializedCheckState> get initializedState =>
      _initializedSource.stream;
  final _initializedSource = BehaviorSubject<InitializedCheckState>();

  Stream<FHError> get errorStream => _errorSource.stream;
  final _errorSource = PublishSubject<FHError>();

  Stream<WidgetBuilder> get overlayStream => _overlaySource.stream;
  final _overlaySource = PublishSubject<WidgetBuilder>();

  Stream<Widget> get snackbarStream => _snackbarSource.stream;
  final _snackbarSource = PublishSubject<Widget>();

  final _personSource = BehaviorSubject<Person>();
  Stream<Person> get personStream => _personSource.stream;

  Organization organization;
  Person person;
  List<Group> groupList;

  bool _userIsSuperAdmin;
  bool get userIsSuperAdmin => _userIsSuperAdmin;

  bool _userIsAnyPortfolioOrSuperAdmin;
  bool get userIsAnyPortfolioOrSuperAdmin => _userIsAnyPortfolioOrSuperAdmin;

  String get currentPid => getCurrentPid();
  String get currentAid => getCurrentAid();
  set currentAid(String aid) => setCurrentAid(aid);
  set currentPid(String pid) => setCurrentPid(pid);

  Portfolio get currentPortfolio {
    return streamValley.currentPortfolio;
  }

  StreamValley streamValley;

  static Uri originUri;

  static String homeUrl() {
    final origin = window.location.origin;
    originUri = Uri.parse(origin);
    if (overrideOrigin && origin.startsWith('http://localhost')) {
      return 'http://localhost:8903';
    } else if (overrideOrigin && origin.startsWith('http://[::1]')) {
      return 'http://[::1]:8903';
    } else {
      final url = Uri.parse(origin);
      return url.replace(path: url.path).toString();
    }
  }

  ManagementRepositoryClientBloc() : _client = ApiClient(basePath: homeUrl()) {
    _basePath = Uri.parse(_client.basePath);
    setupApi = SetupServiceApi(_client);
    personServiceApi = PersonServiceApi(_client);
    authServiceApi = AuthServiceApi(_client);
    portfolioServiceApi = PortfolioServiceApi(_client);
    serviceAccountServiceApi = ServiceAccountServiceApi(_client);
    environmentServiceApi = EnvironmentServiceApi(_client);
    personState = PersonState(personServiceApi);
    featureServiceApi = FeatureServiceApi(_client);
    applicationServiceApi = ApplicationServiceApi(_client);
    _errorSource.add(null);
    streamValley = StreamValley(this, personState);
    init();
//    setHistory();
  }

  ApiClient get apiClient => _client;

  void init() async {
    _sharedPreferences = await FHSharedPrefs.getSharedInstance();
    await isInitialized();
  }

  Future isInitialized() async {
    if (person != null) {
      _initializedSource.add(InitializedCheckState.zombie);
      return;
    }

    await setupApi.isInstalled().then((org) {
      // if we are initialized, check for an existing cookie
      // to see if we have a bearer token. This would mean the user
      // has simply refreshed their page

      final bearerToken = _getBearerCookie();
      organization = org;
      if (bearerToken != null) {
        setBearerToken(bearerToken);
        _requestOwnDetails();
      } else {
        _initializedSource.add(InitializedCheckState.initialized);
      }
    }).catchError((e, s) {
      if (e is ApiException) {
        if (e.code == 404) {
          _initializedSource.add(InitializedCheckState.uninitialized);
          //fakeInitialize();
        } else {
          dialogError(e, s);
        }
      }
    });
  }

  static const bearerToken = 'bearer-token';

  String _getBearerCookie() {
    final cookies = document.cookie.split(';')
      ..retainWhere((s) => s.trim().startsWith('$bearerToken='));

    if (cookies.isNotEmpty) {
      return cookies.first.trim().substring('$bearerToken='.length);
    }

    return null;
  }

  void _setBearerCookie(String token) {
    document.cookie = '$bearerToken=$token; path=/';
  }

  void _clearBearerCookie() {
    // expires back in 1970
    document.cookie =
        '$bearerToken=Da; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
  }

  // ask for my own details and if there are some, set the person and transition
  // to logged in, otherwise ask them to log in.
  void _requestOwnDetails() {
    personServiceApi
        .getPerson('self', includeAcls: true, includeGroups: true)
        .then((p) {
      setPerson(p);
      _initializedSource.add(InitializedCheckState.zombie);
    }).catchError((_) {
      setBearerToken(null);
      _initializedSource.add(InitializedCheckState.initialized);
    });
  }

  Future logout() async {
    await authServiceApi.logout();
    setBearerToken(null);
    return;
  }

  void fakeInitialize() {
    var ssa = SetupSiteAdmin()
      ..portfolio = 'Portfolio name'
      ..name = 'Name'
      ..organizationName = 'Org Name'
      ..password = 'password123'
      ..emailAddress = 'superuser@mailinator.com';
    setupApi.setupSiteAdmin(ssa).then((tp) {
      setPerson(tp.person);
      setBearerToken(tp.accessToken);
      _initializedSource.add(InitializedCheckState.zombie);
    }).catchError((e, s) {
      dialogError(e, s);
      _initializedSource.add(InitializedCheckState.unknown);
    });
  }

  void setOrg(Organization o) {
    organization = o;
    _initializedSource.add(InitializedCheckState.zombie);
  }

  void setBearerToken(String token) {
    _client.setAuthentication('bearerAuth', OAuth(accessToken: token));

    if (token == null) {
      _clearBearerCookie();
    } else {
      _setBearerCookie(token);
    }
  }

  void setCurrentAid(String aid) {
    streamValley.currentAppId = aid;
    _setAidSharedPrefs(aid);
  }

  void setCurrentPid(String pid) {
    streamValley.currentPortfolioId = pid;
    _setAidSharedPrefs(null);
    _setPidSharedPrefs(pid);
    personState.currentPortfolioOrSuperAdminUpdateState(pid, groupList);
  }

  String getCurrentPid() {
    return streamValley.currentPortfolioId;
  }

  String getCurrentAid() {
    return streamValley.currentAppId;
  }

  void setPerson(Person p) {
    person = p;
    groupList = p.groups;

    _userIsSuperAdmin = personState.isSuperAdminGroupFound(groupList);
    _userIsAnyPortfolioOrSuperAdmin =
        personState.isAnyPortfolioOrSuperAdmin(groupList);

    _personSource.add(p);
    _addPortfoliosToStream();
  }

  bool isPortfolioOrSuperAdmin(String pid) {
    return (personState.isSuperAdminGroupFound(groupList) ||
        personState.userIsPortfolioAdmin(pid, groupList));
  }

  void addOverlay(WidgetBuilder builder) {
    _overlaySource.add(builder);
  }

  void removeOverlay() {
    _overlaySource.add(null);
  }

  void addError(FHError error) {
    _errorSource.add(error);
  }

  void addSnackbar(Widget content) {
    _snackbarSource.add(content);
  }

  void customError({String messageTitle = '', String messageBody = ''}) {
    addError(FHError(messageTitle,
        exception: null,
        stackTrace: null,
        showDetails: false,
        errorMessage: messageBody));
  }

  void dialogError(e, StackTrace s,
      {String messageTitle, bool showDetails = true, String messageBody = ''}) {
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
    window.console.error(e?.toString());
    window.console.error(s?.toString());
  }

  Future login(String email, String password) async {
    await authServiceApi
        .login(UserCredentials()
          ..email = email
          ..password = password)
        .then((tp) {
      setBearerToken(tp.accessToken);
      setPerson(tp.person);
      setLastUsername(tp.person.email);
      if (!tp.person.passwordRequiresReset) {
        _initializedSource.add(InitializedCheckState.zombie);
      } else {
        _initializedSource.add(InitializedCheckState.requires_password_reset);
      }
    });
  }

  Future<void> replaceTempPassword(String password) {
    return authServiceApi
        .replaceTempPassword(person.id.id, PasswordReset()..password = password)
        .then((tp) {
      setBearerToken(tp.accessToken);
      _initializedSource.add(InitializedCheckState.zombie);
    });
  }

  // this can be called when a portfolio is deleted
  void refreshPortfolios() async {
    _addPortfoliosToStream();
  }

  void _addPortfoliosToStream() async {
    try {
      final _portfolios = await streamValley.loadPortfolios();
      if (await _sharedPreferences.getString('currentPid') != null) {
        final aid = await _sharedPreferences.getString('currentAid');
        setCurrentPid(await _sharedPreferences.getString('currentPid'));
        if (aid != null) {
          setCurrentAid(aid);
        }
      } else if (_portfolios != null && _portfolios.isNotEmpty) {
        setCurrentPid(_portfolios.first.id.toString());
      }
    } catch (e, s) {
      dialogError(e, s);
    }
  }

  @override
  void dispose() {
    _initializedSource.close();
    _errorSource.close();
    _overlaySource.close();
    _snackbarSource.close();
    _personSource.close();
    streamValley.currentPortfolioAdminOrSuperAdminSubscription.cancel();
  }

  void _setPidSharedPrefs(pid) async {
    await _sharedPreferences.saveString('currentPid', pid);
  }

  void _setAidSharedPrefs(aid) async {
    await _sharedPreferences.saveString('currentAid', aid);
  }

  Future<String> lastUsername() async {
    return await _sharedPreferences.getString('lastUsername');
  }

  void setLastUsername(String lastUsername) async {
    await _sharedPreferences.saveString('lastUsername', lastUsername);
  }

  // if a url comes back from the backend with a back-end url, we need to rewrite it to our
  // own "window api" front end url
  String rewriteUrl(String url) {
    final uri = Uri.parse(url);

    if (uri.host == _basePath.host && uri.port == _basePath.port) {
      return uri.replace(host: originUri.host, port: originUri.port).toString();
    }

    return url;
  }

  void resetInitialized() {
    _initializedSource.add(InitializedCheckState.initialized);
  }
}
