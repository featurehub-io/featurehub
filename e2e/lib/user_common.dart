import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

/// import this to store and get details about a person between steps

class UserCommon {
  ApiClient _apiClient;
  PersonServiceApi _personService;
  SetupServiceApi _setupService;
  GroupServiceApi _groupService;
  AuthServiceApi _authServiceApi;
  PortfolioServiceApi _portfolioServiceApi;
  ApplicationServiceApi _applicationServiceApi;
  EnvironmentServiceApi _environmentServiceApi;
  FeatureServiceApi _featureServiceApi;
  ServiceAccountServiceApi _serviceAccountServiceApi;
  EnvironmentFeatureServiceApi _environmentFeatureServiceApi;

  UserCommon() {
    _apiClient = ApiClient(basePath: "http://localhost:8903");

    _personService = PersonServiceApi(_apiClient);
    _setupService = SetupServiceApi(_apiClient);
    _groupService = GroupServiceApi(_apiClient);
    _authServiceApi = AuthServiceApi(_apiClient);
    _portfolioServiceApi = PortfolioServiceApi(_apiClient);
    _applicationServiceApi = ApplicationServiceApi(_apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_apiClient);
    _featureServiceApi = FeatureServiceApi(_apiClient);
    _serviceAccountServiceApi = ServiceAccountServiceApi(_apiClient);
    _environmentFeatureServiceApi = EnvironmentFeatureServiceApi(_apiClient);
  }

  PersonServiceApi get personService => _personService;

  SetupServiceApi get setupService => _setupService;

  GroupServiceApi get groupService => _groupService;

  AuthServiceApi get authService => _authServiceApi;

  PortfolioServiceApi get portfolioService => _portfolioServiceApi;

  ApplicationServiceApi get applicationService => _applicationServiceApi;

  EnvironmentServiceApi get environmentService => _environmentServiceApi;

  FeatureServiceApi get featureService => _featureServiceApi;

  EnvironmentFeatureServiceApi get environmentFeatureServiceApi =>
      _environmentFeatureServiceApi;

  ServiceAccountServiceApi get serviceAccountService =>
      _serviceAccountServiceApi;

  Person _person;
  String _token;

  Person get person => _person;

  bool get hasToken => _token != null;

  void set person(Person p) => _person = p;

  void set tokenized(TokenizedPerson p) {
    print("logged in as ${p.person.name} with token ${p.accessToken}");
    _person = p.person;
    _apiClient.setAuthentication(
        'bearerAuth', OAuth(accessToken: p.accessToken));
    _token = p.accessToken;
    print("auth token is now ${p.accessToken}");
  }

  void clearAuth() {
    _apiClient.setAuthentication('bearerAuth', null);
    _token = null;
  }

  void completeRegistration(String name, String password, String email,
      String registrationUrl) async {
    clearAuth();

    var uriParse = Uri.parse(registrationUrl);
    String token = uriParse.fragment;
    token = token
        .substring(token.indexOf('?') + 1)
        .split("&")
        .firstWhere((frag) => frag.startsWith("token="), orElse: () => 'token=')
        .substring('token='.length);

    assert(token != null && token.isNotEmpty, 'token is empty or null $token');

    var person = await authService.personByToken(token);

    assert(person.email == email.toLowerCase());

    TokenizedPerson tokenizedPerson =
        await authService.registerPerson(PersonRegistrationDetails()
          ..email = email
          ..password = password
          ..confirmPassword = password
          ..name = name
          ..registrationToken = token);

    assert(tokenizedPerson != null, 'person not registered');

    tokenized = tokenizedPerson;
  }

  // operates in user space
  Future<Portfolio> findExactPortfolio(String portfolioName,
      {PortfolioServiceApi portfolioServiceApi}) async {
    PortfolioServiceApi _pService =
        portfolioServiceApi ?? this.portfolioService;
    var portfolios = await _pService.findPortfolios(
        filter: portfolioName, includeGroups: true);
    return portfolios.firstWhere((p) => p.name == portfolioName,
        orElse: () => null);
  }

  Future<Application> findExactApplication(String appName, String portfolioId,
      {ApplicationServiceApi applicationServiceApi}) async {
    ApplicationServiceApi _aService =
        applicationServiceApi ?? _applicationServiceApi;
    var apps = await _aService.findApplications(portfolioId, filter: appName);
    return apps.firstWhere((a) => a.name == appName, orElse: () => null);
  }

  Future<Environment> findExactEnvironment(String envName, String appId,
      {EnvironmentServiceApi environmentServiceApi}) async {
    EnvironmentServiceApi _eService =
        environmentServiceApi ?? _environmentServiceApi;
    var envs = await _eService.findEnvironments(appId, filter: envName);
    return envs.firstWhere((e) => e.name == envName, orElse: () => null);
  }

  Future<Group> findExactGroup(String groupName, String portfolioId,
      {GroupServiceApi groupServiceApi}) async {
    GroupServiceApi _gService = groupServiceApi ?? _groupService;
    var groups = await _gService.findGroups(portfolioId,
        filter: groupName, includePeople: true);
    return groups.firstWhere((g) => g.name == groupName, orElse: () => null);
  }

  Future<ServiceAccount> findExactServiceAccount(
      String serviceAccount, String portfolioId,
      {ServiceAccountServiceApi serviceAccountServiceApi}) async {
    ServiceAccountServiceApi _saService =
        serviceAccountService ?? _serviceAccountServiceApi;
    var serviceAccounts = await _saService.searchServiceAccountsInPortfolio(
        portfolioId,
        includePermissions: true);
    return serviceAccounts.firstWhere((sa) => sa.name == serviceAccount,
        orElse: () => null);
  }

  Future<Group> findExactGroupWithPerms(String groupName, String portfolioId,
      {GroupServiceApi groupServiceApi}) async {
    GroupServiceApi _gService = groupServiceApi ?? _groupService;
    var groups = await _gService.findGroups(portfolioId, filter: groupName);
    Group group =
        groups.firstWhere((g) => g.name == groupName, orElse: () => null);
    return await _gService.getGroup(group.id,
        includeGroupRoles: true, includeMembers: true);
  }

  Future<Person> findExactEmail(String email,
      {PersonServiceApi personServiceApi}) async {
    var people = await (personServiceApi ?? this.personService)
        .findPeople(filter: email);

    if (people.people.isNotEmpty) {
      return people.people[0];
    } else {
      return null;
    }
  }
}
