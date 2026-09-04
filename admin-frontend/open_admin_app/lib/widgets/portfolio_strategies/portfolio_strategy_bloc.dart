import 'dart:async';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:rxdart/rxdart.dart';

class PortfolioStrategyBloc implements Bloc, ManagementRepositoryAwareBloc {
  late StreamSubscription<String?> _currentPortfolioIdListener;
  @override
  final ManagementRepositoryClientBloc mrClient;
  final PortfolioRolloutStrategyServiceApi _portfolioRolloutStrategyServiceApi;

  String? portfolioId;

  final _currentPortfolioIdStream = BehaviorSubject<String?>();
  BehaviorSubject<String?> get currentPortfolioIdStream =>
      _currentPortfolioIdStream;

  PortfolioStrategyBloc(this.mrClient)
      : _portfolioRolloutStrategyServiceApi =
            PortfolioRolloutStrategyServiceApi(mrClient.apiClient) {
    _currentPortfolioIdListener =
        mrClient.streamValley.currentPortfolioIdStream.listen(_setPortfolioId);
  }

  @override
  void dispose() {
    _currentPortfolioIdListener.cancel();
    _currentPortfolioIdStream.close();
  }

  void _setPortfolioId(String? id) {
    portfolioId = id;
    _currentPortfolioIdStream.add(id);
  }

  Future<PortfolioRolloutStrategyList> getStrategiesData(
      String? filter, SortOrder sortOrder) async {
    if (portfolioId != null) {
      return await _portfolioRolloutStrategyServiceApi.listPortfolioStrategies(
          portfolioId!,
          includeUsage: true,
          includeWhoChanged: true,
          filter: filter,
          sortOrder: sortOrder,
          max: 1000);
    } else {
      return PortfolioRolloutStrategyList(max: 0, page: 0, items: []);
    }
  }

  Future deleteStrategy(String strategyId) {
    return _portfolioRolloutStrategyServiceApi.deletePortfolioStrategy(
        portfolioId!, strategyId);
  }
}