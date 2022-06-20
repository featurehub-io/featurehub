import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/user/common/portfolio_group.dart';
import 'package:open_admin_app/widgets/user/common/select_portfolio_group_bloc.dart';
import 'package:rxdart/rxdart.dart' as rxdart;

enum EditUserForm { loadingState, initialState, errorState, successState }

class EditUserBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;
  final SelectPortfolioGroupBloc selectGroupBloc;
  final String? personId;
  Person? person;

  final _formStateStream = rxdart.BehaviorSubject<EditUserForm?>();
  Stream<EditUserForm?> get formState => _formStateStream.stream;

  EditUserBloc(this.mrClient, this.personId, {required this.selectGroupBloc}) {
    _loadInitialPersonData();
  }

  @override
  void dispose() {
    _formStateStream.close();
  }

  Future<void> resetUserPassword(String password) async {
    if (personId != null) {
      final passwordReset = PasswordReset(
        password: password,
      );

      await mrClient.authServiceApi.resetPassword(personId!, passwordReset);
    }
  }

  void _loadInitialPersonData() async {
    if (personId != null) {
      await _findPersonAndTriggerInitialState(personId!);
      if (person != null) {
        _findPersonsGroupsAndPushToStream();
      }
    }
  }

  Future<void> _findPersonAndTriggerInitialState(String queryParameter) async {
    try {
      person = await mrClient.personServiceApi
          .getPerson(queryParameter, includeGroups: true);
      _formStateStream.add(EditUserForm.initialState);
    } catch (e, s) {
      await mrClient.dialogError(e, s);
    }
  }

  Future<void> updatePersonDetails(String email, String name) async {
    if (person != null) {
      final pers = person!;
      pers.groups = selectGroupBloc.listOfAddedPortfolioGroups
          .map((pg) => pg.group)
          .toList();
      pers.name = name;
      pers.email = email;
      await mrClient.personServiceApi
          .updatePerson(personId!, pers, includeGroups: true);
    }
  }

  Future<void> updateApiKeyDetails(String name) async {
    if (person != null) {
      final pers = person!;
      pers.groups = selectGroupBloc.listOfAddedPortfolioGroups
          .map((pg) => pg.group)
          .toList();
      pers.name = name;
      await mrClient.personServiceApi
          .updatePerson(personId!, pers, includeGroups: true);
    }
  }

  Future<List<Portfolio>> _findPortfolios() async {
    List<Portfolio> portfolios = [];
    try {
      portfolios = await mrClient.portfolioServiceApi
          .findPortfolios(includeGroups: true);
    } catch (e, s) {
      await mrClient.dialogError(e, s);
    }
    return portfolios;
  }

  void _findPersonsGroupsAndPushToStream() async {
    if (person != null) {
      final portfoliosList = await _findPortfolios();

      final listOfExistingGroups = <PortfolioGroup>[];
      for (var group in person!.groups) {
        {
          listOfExistingGroups.add(PortfolioGroup(
              portfoliosList.firstWhereOrNull((p) =>
                  p.id ==
                  group
                      .portfolioId), // null is set for Portfolio for super admin group which doesn't belong to any portfolio
              group));
        }
      }
      selectGroupBloc.pushExistingGroupToStream(listOfExistingGroups);
    }
  }

  Portfolio isSuperAdminPortfolio() {
    return Portfolio(name: 'Super-Admin', description: 'Super-Admin Portfolio');
  }
}
