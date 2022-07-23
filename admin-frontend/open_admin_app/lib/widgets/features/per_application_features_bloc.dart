import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:collection/collection.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:rxdart/rxdart.dart' hide Notification;

/// this represents a grouping of types of features, with a name of some kind just
/// to make it easier to find
class FeatureGrouping {
  final List<FeatureValueType> types;

  FeatureGrouping({required this.types});
}

final featureGroups = [
  FeatureGrouping(types: [FeatureValueType.BOOLEAN]),
  FeatureGrouping(types: [FeatureValueType.STRING, FeatureValueType.NUMBER]),
  FeatureGrouping(types: [FeatureValueType.JSON]),
];

final featureGroupDefault = featureGroups[0];
final featureGroupFlags = featureGroups[0];
final featureGroupValues = featureGroups[1];
final featureGroupConfig = featureGroups[2];

///
/// represents the constant information across all tabs, the hidden environments and
/// the whole list of environments accessible by this person
///
class EnvironmentsInfo {
  final HiddenEnvironments userEnvironmentData;
  final List<Environment> environments;
  final bool isEmpty;

  EnvironmentsInfo(this.userEnvironmentData, this.environments) : isEmpty = false {
    print("environments are " + environments.toString());
    print("shown environments are " + shownEnvironments.toString());
    print("hidden environments are " + hiddenEnvironments.toString());
  }

  EnvironmentsInfo.empty() : userEnvironmentData = HiddenEnvironments(), environments = [], isEmpty = true;

  List<Environment> get shownEnvironments =>
    environments.where((env) => !userEnvironmentData.environmentIds.contains(env.id)).toList();

  List<Environment> get hiddenEnvironments =>
      environments.where((env) => userEnvironmentData.environmentIds.contains(env.id)).toList();

  bool isHidden(String envId) =>
      environments.firstWhereOrNull((env) => env.id == envId) != null;

  bool isShown(String envId) => !isHidden(envId);

  List<String> get hiddenEnvironmentIds => [...userEnvironmentData.environmentIds];

  @override
  String toString() {
    return 'EnvironmentsInfo{userEnvironmentData: $userEnvironmentData, environments: $environments, isEmpty: $isEmpty';
  }
}

class FeaturesByType {
  final ApplicationFeatureValues applicationFeatureValues;
  final FeatureGrouping grouping; // the grouping this set of features relates to
  final bool isEmpty; // indicates this represents an empty set of data

  final String? filter; // the filter this set of data reflects
  final int pageNumber; // the page number depending on the page size we are sending

  // envId, EnvironmentFeatureValues mapping - so it is done only once not once per line in table
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};

  FeaturesByType(this.applicationFeatureValues, this.grouping, this.filter, this.pageNumber) : isEmpty = false {
    for (var e in applicationFeatureValues.environments) {
      applicationEnvironments[e.environmentId!] = e;
    }
  }

  FeaturesByType.empty(this.grouping) : applicationFeatureValues =
    ApplicationFeatureValues(applicationId: '', environments: [], features: [], maxFeatures: 0),
    filter = null,
    pageNumber = -1,
    isEmpty = true;
}

///
/// This holds state relative to the whole set of features across the entire application
/// so application level stuff should happen here.
///
class PerApplicationFeaturesBloc
    implements Bloc, ManagementRepositoryAwareBloc {
  String? portfolioId;
  String? applicationId;
  final ManagementRepositoryClientBloc _mrClient;
  late ApplicationServiceApi _appServiceApi;
  late EnvironmentServiceApi _environmentServiceApi;
  late UserStateServiceApi _userStateServiceApi;
  late RolloutStrategyServiceApi _rolloutStrategyServiceApi;

  late FeatureServiceApi _featureServiceApi;

  final _environmentsSource = BehaviorSubject.seeded(EnvironmentsInfo.empty());
  Stream<EnvironmentsInfo> get environmentsStream => _environmentsSource;
  final _appSearchResultSource = BehaviorSubject<List<Application>?>();

  Stream<List<Application>?> get applications => _appSearchResultSource.stream;

  final Map<FeatureGrouping, BehaviorSubject<FeaturesByType>> _perTabApplicationFeatures = {};

  int get itemsPerPage => 10;

  ManagementRepositoryClientBloc get mrBloc => _mrClient;

  BehaviorSubject<FeaturesByType> _grouping(FeatureGrouping grouping) {
    final subject = _perTabApplicationFeatures.putIfAbsent(grouping, () =>
        BehaviorSubject.seeded(
            FeaturesByType.empty(grouping)));

    return subject;
  }

  Stream<FeaturesByType> appFeatures(FeatureGrouping grouping) {
    final subject = _grouping(grouping);

    return subject.stream;
  }

  late StreamSubscription<String?> _currentPid;
  late StreamSubscription<String?> _currentAppId;

  final _publishNewFeatureSource = PublishSubject<Feature>();

  Stream<Feature> get publishNewFeatureStream =>
      _publishNewFeatureSource.stream;

  final _getAllAppValuesDebounceStream = BehaviorSubject<String?>();

  PerApplicationFeaturesBloc(this._mrClient) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(_mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_mrClient.apiClient);
    _userStateServiceApi = UserStateServiceApi(_mrClient.apiClient);
    _rolloutStrategyServiceApi = RolloutStrategyServiceApi(_mrClient.apiClient);

    _currentPid = _mrClient.streamValley.currentPortfolioIdStream
        .listen(addApplicationsToStream);
    _currentAppId = _mrClient.streamValley.currentAppIdStream.listen((appId) => _getAllAppValuesDebounceStream.add(appId));

    _getAllAppValuesDebounceStream
        .debounceTime(const Duration(milliseconds: 300))
        .listen((appId) {
      _actuallyChangeApplicationIdAfterDebounce(appId);
    });
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  Future<RolloutStrategyValidationResponse> validationCheck(
      List<RolloutStrategy> customStrategies,
      List<RolloutStrategyInstance> sharedStrategies) async {
    // print('validating custom strategies $customStrategies');

    return _rolloutStrategyServiceApi.validate(
        applicationId!,
        RolloutStrategyValidationRequest(
          customStrategies: customStrategies,
          sharedStrategies: sharedStrategies,
        ));
  }

  Future<Environment> getEnvironment(String envId) async {
    return _environmentServiceApi
        .getEnvironment(envId,
            includeServiceAccounts: true, includeSdkUrl: true)
        .catchError((e, s) {
      mrClient.dialogError(e, s);
    });
  }

  void _sortApplicationFeatureValues(
      ApplicationFeatureValues appFeatureValues) {
//    appFeatureValues.environments.sort((a, b) => a.environmentName.compareTo(b.environmentName));
    appFeatureValues.features.sort((a, b) => a.name.compareTo(b.name));
  }

  Future<void> updateFeatureGrouping(FeatureGrouping grouping, String? filter, int pageNumber) async {
    if (applicationId == null) {
      return;
    }
    try {
      // hidden environments is based on application, swap this out
      final appFeatureValues = await
          _featureServiceApi
          .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(applicationId!,
          filter: filter,
          featureTypes: grouping.types,
          max: itemsPerPage,
          page: pageNumber);

      final bs = _grouping(grouping);
      if (!bs.isClosed) {
        _sortApplicationFeatureValues(appFeatureValues);

        _grouping(grouping).add(FeaturesByType(appFeatureValues, grouping, filter, pageNumber));
      }
    } catch (e, s) {
      await mrClient.dialogError(e, s);
    }
  }

  void _reloadGrouping(FeatureValueType valueType) {
    final grouping = featureGroups.firstWhereOrNull((g) => g.types.contains(valueType));

    if (grouping != null) {
      final subject = _grouping(grouping);

      updateFeatureGrouping(grouping, subject.value!.filter, subject.value!.pageNumber);
    }
  }

  void _actuallyChangeApplicationIdAfterDebounce(String? appId) async {
    applicationId = appId;

    if (applicationId == null) {
      // wipe the list of environments and hidden data
      _environmentsSource.add(EnvironmentsInfo.empty());
    } else {
      final environments = await mrClient.environmentServiceApi.findEnvironments(appId!);
      final hidden = await _userStateServiceApi.getHiddenEnvironments(appId);

      _environmentsSource.add(EnvironmentsInfo(hidden, environments));
    }

    // wipe all the groups, the downstream page must trigger a request
    for(final grouping in featureGroups) {
      _grouping(grouping).add(FeaturesByType.empty(grouping));
    }
  }

  void clearAppFeatureValuesStream(FeatureGrouping grouping) {
    final g = _grouping(grouping);

    if (!g.isClosed) {
      g.add(FeaturesByType.empty(grouping));
    }
  }

  // hidden environments have their own lifecycle, people can turn them on and
  // off completely separately from their grouping and filters. We should optimise
  // the request and specify environments in our API call to limit data further.
  Future<void> _updateShownEnvironments(List<String> environmentIds) async {
    final newHiddenEnvironments = HiddenEnvironments(
      environmentIds: environmentIds,
    );

    final envs = await _userStateServiceApi.saveHiddenEnvironments(
        applicationId!, newHiddenEnvironments);

    _environmentsSource.add(EnvironmentsInfo(envs, _environmentsSource.value!.environments));
  }

  Future<void> addShownEnvironment(String envId) async {
    if (_environmentsSource.value?.isShown(envId) != true) {
      final envs =_environmentsSource.value!.hiddenEnvironmentIds;
      envs.add(envId);
      // ignore: unawaited_futures
      _updateShownEnvironments(envs);
    }
  }

  Future<void> removeShownEnvironment(String envId) async {
    final envs =_environmentsSource.value!.hiddenEnvironmentIds;

    if (envs.remove(envId)) {
      // ignore: unawaited_futures
      _updateShownEnvironments(envs);
    }
  }

  bool environmentVisible(String envId) {
    return _environmentsSource.value?.isShown(envId) == true;
  }

  void checkPortfolioIdIsLegit(List<Portfolio> portfolios) {
    if (!portfolios.any((p) => p.id == portfolioId)) {
      //if portfolioId not found in the system - set it to null
      portfolioId = null;
      mrClient.customError(
          messageTitle: 'Provided portfolio ID is not found in the system');
    }
  }

  Future<void> addApplicationsToStream(String? pid) async {
    portfolioId = pid;

    if (pid != null) {
      List<Application>? appList;
      try {
        appList = await _appServiceApi.findApplications(portfolioId!,
            order: SortOrder.ASC);
        if (!_appSearchResultSource.isClosed) {
          _appSearchResultSource.add(appList);
        }
      } catch (e, s) {
        await mrClient.dialogError(e, s);
      }

      if (appList != null) {
        checkApplicationIdIsLegit(appList);
      }
    }
  }

  void checkApplicationIdIsLegit(List<Application> appList) {
    if (appList.isEmpty) {
      print("applist is empty");
      applicationId = null;
      _environmentsSource.add(EnvironmentsInfo.empty());
    } else if (!appList.any((app) => app.id == applicationId)) {
      print("applist does not contain applicationid");
      _getAllAppValuesDebounceStream.add(appList[0].id);
    } else {
      print("forcing selection of application");
      _getAllAppValuesDebounceStream.add(appList[0].id);
    }
  }

  Future<void> createFeature(
      String name,
      String key,
      FeatureValueType featureValueType,
      String featureAlias,
      String featureLink,
      String featureDescription) async {
    final feature = Feature(
      name: name,
      valueType: featureValueType,
      key: key,
      alias: featureAlias,
      link: featureLink,
      description: featureDescription
    );
    await _featureServiceApi.createFeaturesForApplication(
        applicationId!, feature);

    _reloadGrouping(featureValueType);

    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
    _publishNewFeatureSource.add(feature);
  }

  Future<void> updateFeature(Feature feature, String newName, String newKey,
      String newFeatureAlias, String newFeatureLink, String newFeatureDescription) async {
    final currentFeature =
        await _featureServiceApi.getFeatureByKey(applicationId!, feature.key!);
    final newFeature = currentFeature
      ..name = newName
      ..alias = newFeatureAlias
      ..link = newFeatureLink
      ..description = newFeatureDescription
      ..key = newKey;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId!, feature.key!, newFeature);
    _reloadGrouping(currentFeature.valueType!);
  }

  Future<void> deleteFeature(Feature feature) async {
    await _featureServiceApi.deleteFeatureForApplication(applicationId!, feature.key!);
    _reloadGrouping(feature.valueType!);
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
  }

  @override
  void dispose() {
    _appSearchResultSource.close();
    _perTabApplicationFeatures.values.forEach((featureGroupSource) { featureGroupSource.close(); });
    _currentPid.cancel();
    _currentAppId.cancel();
  }

  Future<void> updateAllFeatureValuesByApplicationForKey(
      Feature feature, List<FeatureValue> updates) async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId!, feature.key!, updates);
    _reloadGrouping(feature.valueType!);
  }
}
