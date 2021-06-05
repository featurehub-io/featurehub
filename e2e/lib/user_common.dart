import 'package:collection/collection.dart' show IterableExtension;
import 'package:e2e_tests/util.dart';
import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

/// import this to store and get details about a person between steps

class UserCommon {
  final PersonServiceApi personService;
  final SetupServiceApi setupService;
  final GroupServiceApi groupService;
  final AuthServiceApi authService;
  final PortfolioServiceApi portfolioService;
  final ApplicationServiceApi applicationService;
  final EnvironmentServiceApi environmentService;
  final FeatureServiceApi featureService;
  final ServiceAccountServiceApi serviceAccountService;
  final EnvironmentFeatureServiceApi environmentFeatureServiceApi;
  final UserStateServiceApi userStateService;
  final RolloutStrategyServiceApi rolloutStrategyService;
  final ApiClient apiClient;

  UserCommon._(
      this.apiClient,
      this.personService,
      this.setupService,
      this.groupService,
      this.authService,
      this.portfolioService,
      this.applicationService,
      this.environmentService,
      this.featureService,
      this.serviceAccountService,
      this.environmentFeatureServiceApi,
      this.userStateService,
      this.rolloutStrategyService);

  factory UserCommon.create() {
    ApiClient apiClient = new ApiClient(basePath: baseUrl());

    return new UserCommon._(
        apiClient,
        PersonServiceApi(apiClient),
        SetupServiceApi(apiClient),
        GroupServiceApi(apiClient),
        AuthServiceApi(apiClient),
        PortfolioServiceApi(apiClient),
        ApplicationServiceApi(apiClient),
        EnvironmentServiceApi(apiClient),
        FeatureServiceApi(apiClient),
        ServiceAccountServiceApi(apiClient),
        EnvironmentFeatureServiceApi(apiClient),
        UserStateServiceApi(apiClient),
        RolloutStrategyServiceApi(apiClient));
  }

  Person? _person;
  String? _token;

  Person? get person => _person;

  bool get hasToken => _token != null;

  void set person(Person? p) => _person = p;

  void set tokenized(TokenizedPerson p) {
    print("logged in as ${p.person?.name} with token ${p.accessToken}");
    _person = p.person;
    apiClient.setAuthentication(
        'bearerAuth', OAuth(accessToken: p.accessToken));
    _token = p.accessToken;
    print("auth token is now ${p.accessToken}");
  }

  void clearAuth() {
    apiClient.setAuthentication('bearerAuth', null);
    _token = null;
  }

  Future<void> completeRegistration(String name, String password, String email,
      String registrationUrl) async {
    clearAuth();

    var uriParse = Uri.parse(registrationUrl);
    String token = uriParse.fragment;
    token = token
        .substring(token.indexOf('?') + 1)
        .split("&")
        .firstWhere((frag) => frag.startsWith("token="), orElse: () => 'token=')
        .substring('token='.length);

    assert(token.isNotEmpty, 'token is empty or null $token');

    var person = await authService.personByToken(token);

    assert(person.email == email.toLowerCase());

    TokenizedPerson tokenizedPerson = await authService.registerPerson(
        PersonRegistrationDetails(
            email: email,
            password: password,
            confirmPassword: password,
            name: name,
            registrationToken: token));

    tokenized = tokenizedPerson;
  }

  // operates in user space
  Future<Portfolio?> findExactPortfolio(String? portfolioName,
      {PortfolioServiceApi? portfolioServiceApi}) async {
    PortfolioServiceApi _pService =
        portfolioServiceApi ?? this.portfolioService;
    final portfolios = await _pService.findPortfolios(
        filter: portfolioName, includeGroups: true);
    return portfolios.firstWhereOrNull((p) => p.name == portfolioName);
  }

  Future<Application?> findExactApplication(
      String? appName, String? portfolioId,
      {ApplicationServiceApi? applicationServiceApi}) async {
    ApplicationServiceApi _aService =
        applicationServiceApi ?? this.applicationService;
    var apps = await _aService.findApplications(portfolioId!, filter: appName);
    return apps.firstWhereOrNull((a) => a.name == appName);
  }

  Future<Environment?> findExactEnvironment(String envName, String? appId,
      {EnvironmentServiceApi? environmentServiceApi}) async {
    assert(appId != null, 'appId is null');
    EnvironmentServiceApi _eService =
        environmentServiceApi ?? this.environmentService;
    var envs = await _eService.findEnvironments(appId!, filter: envName);
    return envs
        .firstWhereOrNull((e) => e.name.toLowerCase() == envName.toLowerCase());
  }

  Future<Group?> findExactGroup(String groupName, String? portfolioId,
      {GroupServiceApi? groupServiceApi}) async {
    GroupServiceApi _gService = groupServiceApi ?? this.groupService;
    assert(portfolioId != null, 'portfolio id is null');
    try {
      var groups = await _gService.findGroups(portfolioId!,
          filter: groupName, includePeople: true);
      return groups.firstWhereOrNull((g) => g.name == groupName);
    } catch (e) {
      if (e is ApiException && e.code == 404) {
        return null;
      }

      throw e;
    }
  }

  Future<ServiceAccount?> findExactServiceAccount(
      String? serviceAccount, String? portfolioId,
      {ServiceAccountServiceApi? serviceAccountServiceApi,
      String? applicationId}) async {
    assert(serviceAccount != null, 'service account is null');
    assert(portfolioId != null, 'portfolio id is null');
    ServiceAccountServiceApi _saService =
        serviceAccountServiceApi ?? this.serviceAccountService;
    var serviceAccounts = await _saService.searchServiceAccountsInPortfolio(
        portfolioId!,
        applicationId: applicationId,
        includePermissions: true);
    return serviceAccounts.firstWhereOrNull((sa) => sa.name == serviceAccount);
  }

  Future<Group?> findExactGroupWithPerms(String groupName, String portfolioId,
      {GroupServiceApi? groupServiceApi}) async {
    GroupServiceApi _gService = groupServiceApi ?? this.groupService;
    var groups = await _gService.findGroups(portfolioId, filter: groupName);
    Group? group = groups.firstWhereOrNull((g) => g.name == groupName);
    return group?.id == null
        ? null
        : await _gService.getGroup(group!.id!,
            includeGroupRoles: true, includeMembers: true);
  }

  Future<Person?> findExactEmail(String email,
      {PersonServiceApi? personServiceApi}) async {
    var people = await (personServiceApi ?? this.personService)
        .findPeople(filter: email);

    if (people.people.isNotEmpty) {
      return people.people[0];
    } else {
      return null;
    }
  }
}
