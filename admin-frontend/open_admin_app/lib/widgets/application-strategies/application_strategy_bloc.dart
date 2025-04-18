import 'dart:async';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:rxdart/rxdart.dart';

class ApplicationStrategyBloc implements Bloc, ManagementRepositoryAwareBloc {
  late StreamSubscription<List<Application>> _currentApplicationsListListener;
  late StreamSubscription<String?> _currentAppIdListener;
  @override
  final ManagementRepositoryClientBloc mrClient;
  final ApplicationRolloutStrategyServiceApi
      _applicationRolloutStrategyServiceApi;

  String? currentEnvId;
  String? appId;

  final _currentApplicationsStream = BehaviorSubject<List<Application>>();
  BehaviorSubject<List<Application>> get currentApplicationsStream =>
      _currentApplicationsStream;
  final _currentApplicationStream = BehaviorSubject<String?>();
  BehaviorSubject<String?> get currentApplicationStream =>
      _currentApplicationStream;
  ApplicationStrategyBloc(this.mrClient)
      : _applicationRolloutStrategyServiceApi =
            ApplicationRolloutStrategyServiceApi(mrClient.apiClient) {
    _currentApplicationsListListener = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getCurrentPortfolioApplications);
    _currentAppIdListener =
        mrClient.streamValley.currentAppIdStream.listen(_setAppId);
  }

  @override
  void dispose() {
    _currentApplicationsListListener.cancel();
    _currentAppIdListener.cancel();
    _currentApplicationsStream.close();
    _currentApplicationStream.close();
  }

  _setAppId(String? id) {
    appId = id;
    currentApplicationStream.add(id);
  }

  Future<void> _getCurrentPortfolioApplications(
      List<Application> appList) async {
    _currentApplicationsStream.add(appList);
  }

  Future<ApplicationRolloutStrategyList> getStrategiesData(
      String? s, SortOrder sortOrder) async {
    if (appId != null) {
      if (s != null) {
        return await _applicationRolloutStrategyServiceApi
            .listApplicationStrategies(appId!,
                includeUsage: true,
                includeWhoChanged: true,
                filter: s,
                sortOrder: sortOrder,
                max:
                    1000); // set max because it defaults to 20 otherwise and we do not use pagination yet
      } else {
        return await _applicationRolloutStrategyServiceApi
            .listApplicationStrategies(appId!,
                includeUsage: true,
                includeWhoChanged: true,
                sortOrder: sortOrder,
                max:
                    1000); // set max because it defaults to 20 otherwise and we do not use pagination yet
      }
    } else {
      return ApplicationRolloutStrategyList(max: 0, page: 0, items: []);
    }
  }

  Future deleteStrategy(String strategyId) {
    return _applicationRolloutStrategyServiceApi.deleteApplicationStrategy(
        appId!, strategyId);
  }
}
