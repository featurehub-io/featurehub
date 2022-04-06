import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

typedef SetPersonHook = void Function(PersonState personState, Person person);

List<SetPersonHook> setPersonHooks = <SetPersonHook>[];

// avoids a null person, this is a person with no permission to anything
Person _unauthenticatedPerson =
    Person(id: PersonId(id: ''), name: '', email: '');

class PersonState {
  //stream if person user is current portfolio or super admin user
  // final BehaviorSubject<ReleasedPortfolio?> _isCurrentPortfolioOrSuperAdmin =
  //     BehaviorSubject<ReleasedPortfolio?>();

  final BehaviorSubject<Person> _personSource =
      BehaviorSubject.seeded(_unauthenticatedPerson);

  Stream<Person> get personStream => _personSource.stream;

  String? lastOrgId;
  Person get person => _personSource.value!;
  bool get isLoggedIn => _personSource.value != _unauthenticatedPerson;

  bool _isUserIsSuperAdmin = false;

  bool get userIsSuperAdmin => _isUserIsSuperAdmin;
  List<Group> get groupList => _personSource.value!.groups;

  bool _userIsAnyPortfolioOrSuperAdmin = false;
  bool get userIsAnyPortfolioOrSuperAdmin => _userIsAnyPortfolioOrSuperAdmin;

  void logout() {
    _personSource.add(_unauthenticatedPerson);
  }

  void updatePerson(Person p, String? orgId) {
    // we want to determine these _before_ we trigger the source update
    _isUserIsSuperAdmin = _isSuperAdminGroupFound(p);
    _userIsAnyPortfolioOrSuperAdmin = _isAnyPortfolioOrSuperAdmin(p);
    print("adding $p to source");
    _personSource.add(p);
  }

  // set person(Person person) {
  //   if (person != _unauthenticatedPerson) {
  //     for (final callback in setPersonHooks) {
  //       callback(this, person);
  //     }
  //   }
  //
  //   _isUserIsSuperAdmin = isSuperAdminGroupFound();
  //
  //   _userIsAnyPortfolioOrSuperAdmin = isAnyPortfolioOrSuperAdmin(person.groups);
  //
  //   if (person == _unauthenticatedPerson ||
  //       (_personSource.value != null &&
  //           _personSource.value!.groups != person.groups)) {
  //     print("released portfolio is null");
  //     _isCurrentPortfolioOrSuperAdmin.add(null);
  //   } else if (_personSource.value != null &&
  //       _personSource.value!.groups != person.groups) {
  //     final releasedPortfolio = _isCurrentPortfolioOrSuperAdmin.value;
  //
  //     if (releasedPortfolio != null) {
  //       print("updating released portfolio $releasedPortfolio");
  //       _isCurrentPortfolioOrSuperAdmin.add(ReleasedPortfolio(
  //           portfolio: releasedPortfolio.portfolio,
  //           currentPortfolioOrSuperAdmin: userIsPortfolioAdmin(
  //               releasedPortfolio.portfolio.id, person.groups)));
  //     }
  //   }
  //
  //   print("person is $person");
  //
  //   _personSource.add(person);
  // }

  // Stream<ReleasedPortfolio?> get isCurrentPortfolioOrSuperAdmin =>
  //     _isCurrentPortfolioOrSuperAdmin.stream;

  bool personCanEditFeaturesForCurrentApplication(String? appId) {
    if (appId == null) {
      return _isUserIsSuperAdmin;
    }

    return _isUserIsSuperAdmin ||
        person.groups.any((gp) => gp.applicationRoles.any((ar) =>
            ar.roles.contains(ApplicationRoleType.FEATURE_EDIT) &&
            ar.applicationId == appId));
  }

  bool userHasPortfolioPermission(String? pid) {
    if (pid == null) return false;

    // if any of their groups have that portfolio, they have permission to at least see it
    return person.groups.any((g) => g.portfolioId == pid);
  }

  bool userHasApplicationPermission(String? appId) {
    if (appId == null) return false;

    return person.groups.any((g) =>
        g.applicationRoles.any((appRole) => appRole.applicationId == appId));
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

  // bool get userIsCurrentPortfolioAdmin =>
  //     _isCurrentPortfolioOrSuperAdmin.value?.currentPortfolioOrSuperAdmin ??
  //     false;
  //
  // void currentPortfolioOrSuperAdminUpdateState(Portfolio p) {
  //   print("resetting portfolio for current/super admin check to $p");
  //   final isAdmin = _isSuperAdminGroupFound() || userIsPortfolioAdmin(p.id);
  //   _isCurrentPortfolioOrSuperAdmin.add(
  //       ReleasedPortfolio(portfolio: p, currentPortfolioOrSuperAdmin: isAdmin));
  // }

  Group? personInSuperuserGroup() {
    if (person != _unauthenticatedPerson) {
      return groupList.firstWhereOrNull(
          (group) => group.admin == true && group.portfolioId == null);
    }

    return null;
  }

  void dispose() {
    _personSource.close();
    // _isCurrentPortfolioOrSuperAdmin.close();
  }

  bool isPersonSuperUserOrPortfolioAdmin(String? portfolioId) {
    return _isUserIsSuperAdmin || userIsPortfolioAdmin(portfolioId);
  }
}
