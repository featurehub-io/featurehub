import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class PersonState {
  PersonServiceApi _personServiceApi;
  //stream if person user is current portfolio or super admin user
  final BehaviorSubject<bool> _isCurrentPortfolioOrSuperAdmin =
      BehaviorSubject<bool>();

  Stream<bool> get currentPortfolioAdminOrSuperAdminStream =>
      _isCurrentPortfolioOrSuperAdmin.stream;
  Stream<bool> get isCurrentPortfolioOrSuperAdmin =>
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

  bool userIsPortfolioAdmin(String id, List<Group> groupList) {
    return groupList?.firstWhere(
            (group) => group.admin && group.portfolioId == id,
            orElse: () => null) !=
        null;
  }

  bool isAnyPortfolioOrSuperAdmin(List<Group> groups) {
    return (isSuperAdminGroupFound(groups) || _isAnyPortfolioAdmin(groups));
  }

  void currentPortfolioOrSuperAdminUpdateState(String pid, List<Group> groups) {
    final isAdmin =
        isSuperAdminGroupFound(groups) || userIsPortfolioAdmin(pid, groups);
    _isCurrentPortfolioOrSuperAdmin.add(isAdmin);
  }
}
