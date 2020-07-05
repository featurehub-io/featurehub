import 'package:app_singleapp/common/stream_valley.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

typedef SetPersonHook = void Function(PersonState personState, Person person);

List<SetPersonHook> setPersonHooks = <SetPersonHook>[];

class PersonState {
  PersonServiceApi _personServiceApi;
  //stream if person user is current portfolio or super admin user
  final BehaviorSubject<ReleasedPortfolio> _isCurrentPortfolioOrSuperAdmin =
      BehaviorSubject<ReleasedPortfolio>();

  final _personSource = BehaviorSubject<Person>();

  Stream<Person> get personStream => _personSource.stream;

  Person get person => _personSource.value;
  bool get isLoggedIn => _personSource.hasValue;

  bool _isUserIsSuperAdmin = false;

  bool get userIsSuperAdmin => _isUserIsSuperAdmin;
  List<Group> get groupList =>
      _personSource.hasValue ? _personSource.value.groups : <Group>[];

  bool _userIsAnyPortfolioOrSuperAdmin = false;
  bool get userIsAnyPortfolioOrSuperAdmin => _userIsAnyPortfolioOrSuperAdmin;

  set person(Person person) {
    setPersonHooks.forEach((callback) => callback(this, person));

    _isUserIsSuperAdmin = isSuperAdminGroupFound(person.groups);

    _userIsAnyPortfolioOrSuperAdmin = isAnyPortfolioOrSuperAdmin(groupList);

    _personSource.add(person);
  }

  Stream<ReleasedPortfolio> get isCurrentPortfolioOrSuperAdmin =>
      _isCurrentPortfolioOrSuperAdmin.stream;

  PersonState(PersonServiceApi personServiceApi) {
    _personServiceApi = personServiceApi;
  }

  Future<Person> _getSelfPersonWithGroups() async {
    final person = await _personServiceApi.getPerson('self',
        includeGroups: true, includeAcls: true);
    return person;
  }

  Future<bool> personCanEditFeaturesForCurrentApplication(String appId) async {
    if (appId == null) {
      return false;
    }

    final person = await _getSelfPersonWithGroups();
    return person.groups.any((gp) => gp.applicationRoles.any((ar) =>
        ar.roles.contains(ApplicationRoleType.FEATURE_EDIT) &&
        ar.applicationId == appId));
  }

  // if they are admin in a group where there is no portfolio id, they are super-admin
  bool isSuperAdminGroupFound(List<Group> groupList) {
    return groupList?.firstWhere(
            (group) => group.admin && group.portfolioId == null,
            orElse: () => null) !=
        null;
  }

  bool _isAnyPortfolioAdmin(List<Group> groupList) {
    return groupList?.any((group) => group.admin) ?? false;
  }

  bool userIsPortfolioAdmin(String id, [List<Group> groupList]) {
    if (id == null) {
      return false;
    }

    final groups = groupList ?? person?.groups;

    return groups?.firstWhere((group) => group.admin && group.portfolioId == id,
            orElse: () => null) !=
        null;
  }

  bool isAnyPortfolioOrSuperAdmin(List<Group> groups) {
    return (isSuperAdminGroupFound(groups) || _isAnyPortfolioAdmin(groups));
  }

  bool get userIsCurrentPortfolioAdmin =>
      _isCurrentPortfolioOrSuperAdmin?.value?.currentPortfolioOrSuperAdmin ??
      false;

  void currentPortfolioOrSuperAdminUpdateState(Portfolio p) {
    final isAdmin = isSuperAdminGroupFound(person.groups) ||
        userIsPortfolioAdmin(p?.id, person.groups);
    _isCurrentPortfolioOrSuperAdmin.add(ReleasedPortfolio()
      ..portfolio = p
      ..currentPortfolioOrSuperAdmin = isAdmin);
  }

  void logout() {
    _personSource.add(null);
  }

  void dispose() {
    _personSource.close();
    _isCurrentPortfolioOrSuperAdmin.close();
  }
}
