import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

typedef SetPersonHook = void Function(PersonState personState, Person person);

List<SetPersonHook> setPersonHooks = <SetPersonHook>[];

// avoids a null person, this is a person with no permission to anything
Person _unauthenticatedPerson =
    Person(id: PersonId(id: ''), name: '', email: '');

class PersonState {
  final BehaviorSubject<Person> _personSource =
      BehaviorSubject.seeded(_unauthenticatedPerson);

  Stream<Person> get personStream => _personSource.stream;

  String? lastOrgId;
  Person get person => _personSource.value;
  bool get isLoggedIn => _personSource.value != _unauthenticatedPerson;

  bool _isUserIsSuperAdmin = false;

  bool get userIsSuperAdmin => _isUserIsSuperAdmin;
  List<Group> get groupList => _personSource.value.groups;

  bool _userIsAnyPortfolioOrSuperAdmin = false;
  bool get userIsAnyPortfolioOrSuperAdmin => _userIsAnyPortfolioOrSuperAdmin;

  void logout() {
    _personSource.add(_unauthenticatedPerson);
  }

  void updatePerson(Person p, String? orgId) {
    // we want to determine these _before_ we trigger the source update
    _isUserIsSuperAdmin = _isSuperAdminGroupFound(p);
    _userIsAnyPortfolioOrSuperAdmin = _isAnyPortfolioOrSuperAdmin(p);
    // print("person updated to superadmin: ${_isUserIsSuperAdmin} ${_userIsAnyPortfolioOrSuperAdmin} ${p}");
    _personSource.add(p);
  }

  final _featureCreateRoles = [ApplicationRoleType.EDIT, ApplicationRoleType.EDIT_AND_DELETE, ApplicationRoleType.CREATE];
  final _featureEditDeleteRoles = [ApplicationRoleType.EDIT, ApplicationRoleType.EDIT_AND_DELETE];

  bool personCanEditFeaturesForApplication(String? appId) {
    return _personHasApplicationRoleInApp(appId, _featureEditDeleteRoles );
  }

  bool personCanCreateFeaturesForApplication(String? appId) {
    return _personHasApplicationRoleInApp(appId, _featureCreateRoles );
  }

  // if we add roles that are NOT feature related, this will need to change to exclude them
  bool personCanAnythingFeaturesForApplication(String? appId) {
    return _isUserIsSuperAdmin ||
        person.groups.any((gp) => gp.applicationRoles?.any((ar) => ar.applicationId == appId && ar.roles.isNotEmpty) == true);
  }

  bool _personHasApplicationRoleInApp(String? appId, List<ApplicationRoleType> roles) {
    if (appId == null) {
      return _isUserIsSuperAdmin;
    }

    return _isUserIsSuperAdmin ||
        person.groups.any((gp) => gp.applicationRoles?.any((ar) => ar.applicationId == appId &&
            ( gp.admin == true || ar.roles.any((roleForAppInGroup) => roles.contains(roleForAppInGroup)) )
        ) == true);
  }

  bool userHasPortfolioPermission(String? pid) {
    // _log.finer("portfolio permission: ${pid} -> person ${person.groups}");
    if (pid == null) return false;

    // if any of their groups have that portfolio, they have permission to at least see it
    return person.groups.any((g) => g.portfolioId == pid);
  }

  bool userHasApplicationPermission(String? appId) {
    if (appId == null) return false;

    return person.groups.any((g) =>
        g.applicationRoles?.any((appRole) => appRole.applicationId == appId) == true);
  }

  // if they are admin in a group where there is no portfolio id, they are super-admin
  bool _isSuperAdminGroupFound([Person? providedPerson]) {
    Person p = providedPerson ?? person;
    return p.groups
        .any((group) => group.admin == true && group.portfolioId == null);
  }

  bool _isAnyPortfolioAdmin([Person? providedPerson]) {
    Person p = providedPerson ?? person;
    return p.groups.any((group) => group.admin == true);
  }

  bool userIsPortfolioAdmin(String? id) {
    if (id == null) {
      return false;
    }

    return person.groups
        .any((group) => group.admin == true && group.portfolioId == id);
  }

  bool _isAnyPortfolioOrSuperAdmin([Person? providedPerson]) {
    Person p = providedPerson ?? person;
    return (_isSuperAdminGroupFound(p) || _isAnyPortfolioAdmin(p));
  }

  Group? personInSuperuserGroup() {
    if (person != _unauthenticatedPerson) {
      return groupList.firstWhereOrNull(
          (group) => group.admin == true && group.portfolioId == null);
    }

    return null;
  }

  void dispose() {
    _personSource.close();
  }

  bool isPersonSuperUserOrPortfolioAdmin(String? portfolioId) {
    return _isUserIsSuperAdmin || userIsPortfolioAdmin(portfolioId);
  }
}
