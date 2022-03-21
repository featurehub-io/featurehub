import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/common/stream_valley.dart';
import 'package:rxdart/rxdart.dart';

typedef SetPersonHook = void Function(PersonState personState, Person person);

List<SetPersonHook> setPersonHooks = <SetPersonHook>[];

// avoids a null person, this is a person with no permission to anything
Person _unauthenticatedPerson =
    Person(id: PersonId(id: ''), name: '', email: '');

class PersonState {
  final PersonServiceApi _personServiceApi;
  //stream if person user is current portfolio or super admin user
  final BehaviorSubject<ReleasedPortfolio?> _isCurrentPortfolioOrSuperAdmin =
      BehaviorSubject<ReleasedPortfolio?>();

  final BehaviorSubject<Person> _personSource =
      BehaviorSubject.seeded(_unauthenticatedPerson);

  Stream<Person> get personStream => _personSource.stream;

  Person get person => _personSource.value!;
  bool get isLoggedIn => _personSource.value != _unauthenticatedPerson;

  bool _isUserIsSuperAdmin = false;

  bool get userIsSuperAdmin => _isUserIsSuperAdmin;
  List<Group> get groupList => _personSource.value!.groups;

  bool _userIsAnyPortfolioOrSuperAdmin = false;
  bool get userIsAnyPortfolioOrSuperAdmin => _userIsAnyPortfolioOrSuperAdmin;

  void logout() {
    person = _unauthenticatedPerson;
  }

  set person(Person person) {
    if (person != _unauthenticatedPerson) {
      for (final callback in setPersonHooks) {
        callback(this, person);
      }
    }

    _isUserIsSuperAdmin = isSuperAdminGroupFound(person.groups);

    _userIsAnyPortfolioOrSuperAdmin = isAnyPortfolioOrSuperAdmin(person.groups);

    if (person == _unauthenticatedPerson) {
      _isCurrentPortfolioOrSuperAdmin.add(null);
    } else {
      final releasedPortfolio = _isCurrentPortfolioOrSuperAdmin.value;

      if (releasedPortfolio != null) {
        _isCurrentPortfolioOrSuperAdmin.add(ReleasedPortfolio(
            portfolio: releasedPortfolio.portfolio,
            currentPortfolioOrSuperAdmin: userIsPortfolioAdmin(
                releasedPortfolio.portfolio.id, person.groups)));
      }
    }

    _personSource.add(person);
  }

  Stream<ReleasedPortfolio?> get isCurrentPortfolioOrSuperAdmin =>
      _isCurrentPortfolioOrSuperAdmin.stream;

  PersonState(this._personServiceApi);

  Future<Person> _getSelfPersonWithGroups() async {
    final person = await _personServiceApi.getPerson('self',
        includeGroups: true, includeAcls: true);
    return person;
  }

  Future<bool> personCanEditFeaturesForCurrentApplication(String? appId) async {
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
    return groupList.firstWhereOrNull(
            (group) => group.admin == true && group.portfolioId == null) !=
        null;
  }

  bool _isAnyPortfolioAdmin(List<Group> groupList) {
    return groupList.any((group) => group.admin == true);
  }

  bool userIsPortfolioAdmin(String? id, [List<Group>? groupList]) {
    if (id == null) {
      return false;
    }

    final groups = groupList ?? person.groups;

    return groups.firstWhereOrNull(
            (group) => group.admin == true && group.portfolioId == id) !=
        null;
  }

  bool isAnyPortfolioOrSuperAdmin(List<Group> groups) {
    return (isSuperAdminGroupFound(groups) || _isAnyPortfolioAdmin(groups));
  }

  bool get userIsCurrentPortfolioAdmin =>
      _isCurrentPortfolioOrSuperAdmin.value?.currentPortfolioOrSuperAdmin ??
      false;

  void currentPortfolioOrSuperAdminUpdateState(Portfolio p) {
    final isAdmin = person != _unauthenticatedPerson &&
        (isSuperAdminGroupFound(person.groups) ||
            userIsPortfolioAdmin(p.id, person.groups));
    _isCurrentPortfolioOrSuperAdmin.add(
        ReleasedPortfolio(portfolio: p, currentPortfolioOrSuperAdmin: isAdmin));
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
    _isCurrentPortfolioOrSuperAdmin.close();
  }
}
