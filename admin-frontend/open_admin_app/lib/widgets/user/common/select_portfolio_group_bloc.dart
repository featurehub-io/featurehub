import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

class SelectPortfolioGroupBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  List<Portfolio> portfoliosList = [];
  Portfolio? currentPortfolio;
  Set<PortfolioGroup> listOfAddedPortfolioGroups = <PortfolioGroup>{};

  final _currentPortfoliosStream = rxdart.BehaviorSubject<List<Portfolio>?>();
  Stream<List<Portfolio>?> get portfolios => _currentPortfoliosStream.stream;

  final _currentGroupsStream = rxdart.BehaviorSubject<List<Group>?>();
  Stream<List<Group>?> get groups => _currentGroupsStream.stream;

  final _addedGroupsSource = rxdart.BehaviorSubject<Set<PortfolioGroup>?>();
  Stream<Set<PortfolioGroup>?> get addedGroupsStream =>
      _addedGroupsSource.stream;

  SelectPortfolioGroupBloc(this.mrClient) {
    loadInitialData();
  }

  void loadInitialData() async {
    await _findPortfoliosAndAddToTheStream();
    if (currentPortfolio != null) {
      _findGroupsAndPushToStream();
    }
  }

  Future<void> _findPortfoliosAndAddToTheStream() async {
    try {
      var data = await mrClient.portfolioServiceApi
          .findPortfolios(includeGroups: true, order: SortOrder.ASC);
      _currentPortfoliosStream.add(data);
      portfoliosList = data;
    } catch (e, s) {
      await mrClient.dialogError(e, s);
    }
  }

  void setCurrentPortfolioAndGroups(String portfolioID) {
    currentPortfolio = portfoliosList.firstWhere((p) => p.id == portfolioID);
    _findGroupsAndPushToStream();
  }

  void _findGroupsAndPushToStream() {
    if (currentPortfolio != null) {
      final groups =
          portfoliosList.firstWhere((p) => p.id == currentPortfolio!.id).groups;
      _currentGroupsStream.add(groups);
    }
  }

  void pushExistingGroupToStream(List<PortfolioGroup> groupList) {
    for (var group in groupList) {
      {
        listOfAddedPortfolioGroups.add(group);
      }
    }
    _addedGroupsSource.add(listOfAddedPortfolioGroups);
  }

  void pushAddedGroupToStream(String groupID) {
    //identify group and add to the list along with the portfolio
    if (currentPortfolio != null) {
      final foundGroup =
          currentPortfolio!.groups.firstWhere((g) => g.id == groupID);
      listOfAddedPortfolioGroups
          .add(PortfolioGroup(currentPortfolio!, foundGroup));
      //add both current portfolio and group so we can later display selected groups in chips
      _addedGroupsSource.add(listOfAddedPortfolioGroups);
    }
  }

  void pushAdminGroupToStream() {
    final adminGroup = mrClient.personState.personInSuperuserGroup();
    if (adminGroup != null) {
      listOfAddedPortfolioGroups.add(PortfolioGroup(null, adminGroup));
      _addedGroupsSource.add(listOfAddedPortfolioGroups);
    }
  }

  void removeAdminGroupFromStream() {
    final adminGroup = mrClient.organization!.orgGroup!;
    removeGroupFromStream(PortfolioGroup(null, adminGroup));
  }

  void removeGroupFromStream(PortfolioGroup groupToBeDeleted) {
    listOfAddedPortfolioGroups.remove(groupToBeDeleted);
    _addedGroupsSource.add(listOfAddedPortfolioGroups);
  }

  void clearAddedPortfoliosAndGroups() {
    // same as in dispose?
    listOfAddedPortfolioGroups.clear();
  }

  @override
  void dispose() {
    _addedGroupsSource.close();
    _currentGroupsStream.close();
    _currentPortfoliosStream.close();
    listOfAddedPortfolioGroups.clear();
    currentPortfolio = null;
  }
}
