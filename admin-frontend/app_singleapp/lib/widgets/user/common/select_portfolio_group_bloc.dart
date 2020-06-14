import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/widgets/user/common/portfolio_group.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

class SelectPortfolioGroupBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  List<Portfolio> portfoliosList = [];
  Portfolio currentPortfolio;
  Set<PortfolioGroup> listOfAddedPortfolioGroups = <PortfolioGroup>{};

  final _currentPortfoliosStream = rxdart.BehaviorSubject<List<Portfolio>>();
  Stream<List<Portfolio>> get portfolios => _currentPortfoliosStream.stream;

  final _currentGroupsStream = rxdart.BehaviorSubject<List<Group>>();
  Stream<List<Group>> get groups => _currentGroupsStream.stream;

  final _addedGroupsStream = rxdart.BehaviorSubject<Set<PortfolioGroup>>();
  Stream<Set<PortfolioGroup>> get addedGroupsStream =>
      _addedGroupsStream.stream;

  SelectPortfolioGroupBloc(this.mrClient) : assert(mrClient != null) {
    loadInitialData();
  }

  void loadInitialData() async {
    await _findPortfoliosAndAddToTheStream();
    if (currentPortfolio != null) {
      _findGroupsAndPushToStream();
    }
  }

  void _findPortfoliosAndAddToTheStream() async {
    try {
      var data = await mrClient.portfolioServiceApi
          .findPortfolios(includeGroups: true, order: SortOrder.ASC);
      _currentPortfoliosStream.add(data);
      portfoliosList = data;
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  void setCurrentPortfolioAndGroups(String portfolioID) {
    currentPortfolio = portfoliosList.firstWhere((p) => p.id == portfolioID);
    _findGroupsAndPushToStream();
  }

  void _findGroupsAndPushToStream() {
    final groups =
        portfoliosList.firstWhere((p) => p.id == currentPortfolio.id).groups;
    _currentGroupsStream.add(groups);
  }

  void pushExistingGroupToStream(List<PortfolioGroup> groupList) {
    groupList.forEach((group) => {listOfAddedPortfolioGroups.add(group)});
    _addedGroupsStream.add(listOfAddedPortfolioGroups);
  }

  void pushAddedGroupToStream(String groupID) {
    //identify group and add to the list along with the portfolio
    Group foundGroup;
    if (currentPortfolio != null) {
      foundGroup = currentPortfolio.groups.firstWhere((g) => g.id == groupID);
      listOfAddedPortfolioGroups
          .add(PortfolioGroup(currentPortfolio, foundGroup));
      //add both current portfolio and group so we can later display selected groups in chips
      _addedGroupsStream.add(listOfAddedPortfolioGroups);
    }
  }

  void pushAdminGroupToStream() {
    final adminGroup = mrClient.organization.orgGroup;
    listOfAddedPortfolioGroups.add(PortfolioGroup(null, adminGroup));
    _addedGroupsStream.add(listOfAddedPortfolioGroups);
  }

  void removeAdminGroupFromStream() {
    final adminGroup = mrClient.organization.orgGroup;
    removeGroupFromStream(PortfolioGroup(null, adminGroup));
  }

  void removeGroupFromStream(PortfolioGroup groupToBeDeleted) {
    listOfAddedPortfolioGroups.remove(groupToBeDeleted);
    _addedGroupsStream.add(listOfAddedPortfolioGroups);
  }

  void clearAddedPortfoliosAndGroups() {
    // same as in dispose?
    if (listOfAddedPortfolioGroups != null) {
      listOfAddedPortfolioGroups.clear();
    }
  }

  @override
  void dispose() {
    _addedGroupsStream.close();
    _currentGroupsStream.close();
    _currentPortfoliosStream.close();
    listOfAddedPortfolioGroups.clear();
    currentPortfolio = null;
  }
}
