import 'package:mrapi/api.dart';
import 'package:openapi_dart_common/openapi.dart';

class SuperuserCommon {
  ApiClient _apiClient;
  PersonServiceApi _personService;
  SetupServiceApi _setupService;
  GroupServiceApi _groupService;
  AuthServiceApi _authServiceApi;
  PortfolioServiceApi _portfolioServiceApi;
  ApplicationServiceApi _applicationServiceApi;
  Person _superuser;
  String _superuserToken;
  TokenizedPerson _tokenizedPerson;

  bool _initialized = false;

  SuperuserCommon() {
    _apiClient = ApiClient(basePath: "http://localhost:8903");

    _personService = PersonServiceApi(_apiClient);
    _setupService = SetupServiceApi(_apiClient);
    _groupService = GroupServiceApi(_apiClient);
    _authServiceApi = AuthServiceApi(_apiClient);
    _portfolioServiceApi = PortfolioServiceApi(_apiClient);
    _applicationServiceApi = ApplicationServiceApi(_apiClient);
  }

  PersonServiceApi get personService => _personService;
  SetupServiceApi get setupService => _setupService;
  GroupServiceApi get groupService => _groupService;
  AuthServiceApi get authService => _authServiceApi;
  PortfolioServiceApi get portfolioService => _portfolioServiceApi;
  ApplicationServiceApi get applicationService => _applicationServiceApi;

  String get initUser => "superuser@mailinator.com";
  String get initPassword => "password123";
  String get superuserGroupId => _superuser.groups
      .firstWhere((g) => g.admin && g.portfolioId == null)
      .id; // explodes if not logged in

  // this MUST be called in every method that accesses this class if @superuser is not used
  void initialize() async {
    if (_initialized) {
      return;
    }

    TokenizedPerson tp;

    try {
      await _setupService.isInstalled();

      tp = await _authServiceApi.login(UserCredentials()
        ..password = initPassword
        ..email = initUser);
    } catch (e) {
      assert(e is ApiException);
      assert((e as ApiException).code == 404,
          'code is not 404 but ${(e as ApiException).code}');

      tp = await _setupService.setupSiteAdmin(SetupSiteAdmin()
        ..emailAddress = initUser
        ..name = "Superuser"
        ..portfolio = "Sample Portfolio"
        ..password = initPassword
        ..organizationName = "Sample Org Name");
    }

    _superuser = tp.person;
    _superuserToken = tp.accessToken;

    _tokenizedPerson = tp;
    _initialized = true;

    bearerToken = _superuserToken;
  }

  TokenizedPerson get tokenizedPerson => _tokenizedPerson;

  void set bearerToken(String token) =>
      _apiClient.setAuthentication('bearerAuth', OAuth(accessToken: token));

  void makeSuperuserCurrentUser() {
    _apiClient.setAuthentication(
        'bearerAuth', OAuth(accessToken: _superuserToken));
  }
}
