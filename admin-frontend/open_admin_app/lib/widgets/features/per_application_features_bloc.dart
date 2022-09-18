import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:rxdart/rxdart.dart' hide Notification;

class FeatureStatusFeatures {
  final ApplicationFeatureValues applicationFeatureValues;
  List<String> sortedByNameEnvironmentIds = [];
  // envId, EnvironmentFeatureValues mapping - so it is done only once not once per line in table
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};

  FeatureStatusFeatures(this.applicationFeatureValues) {
    sortedByNameEnvironmentIds = applicationFeatureValues.environments
        .map((e) => e.environmentId!)
        .toList();

    for (var e in applicationFeatureValues.environments) {
      applicationEnvironments[e.environmentId!] = e;
    }
  }
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

  final _appSearchResultSource = BehaviorSubject<List<Application>?>();
  Stream<List<Application>?> get applications => _appSearchResultSource.stream;

  final _shownEnvironmentsSource = BehaviorSubject<List<String>>.seeded([]);
  Stream<List<String>> get shownEnvironmentsStream =>
      _shownEnvironmentsSource.stream;

  final _appFeatureValuesBS = BehaviorSubject<FeatureStatusFeatures?>();
  Stream<FeatureStatusFeatures?> get appFeatureValues =>
      _appFeatureValuesBS.stream;
  // feature-id, environments for feature

  late StreamSubscription<String?> _currentPid;
  late StreamSubscription<String?> _currentAppId;

  final _publishNewFeatureSource = PublishSubject<Feature>();

  Stream<Feature> get publishNewFeatureStream =>
      _publishNewFeatureSource.stream;

  final _getAllAppValuesDebounceStream = BehaviorSubject<bool>();
  final _featureMetadataStream = BehaviorSubject<Feature?>();
  Stream<Feature?> get featureMetadataStream =>
      _featureMetadataStream.stream;

  PerApplicationFeaturesBloc(this._mrClient) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(_mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_mrClient.apiClient);
    _userStateServiceApi = UserStateServiceApi(_mrClient.apiClient);
    _rolloutStrategyServiceApi = RolloutStrategyServiceApi(_mrClient.apiClient);

    _currentPid = _mrClient.streamValley.currentPortfolioIdStream
        .listen(addApplicationsToStream);
    _currentAppId = _mrClient.streamValley.currentAppIdStream.listen(setAppId);

    _getAllAppValuesDebounceStream
        .debounceTime(const Duration(milliseconds: 300))
        .listen((event) {
      _actuallyCallAddAppFeatureValuesToStream();
    });
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  void setAppId(String? appId) {
    applicationId = appId;
    if (applicationId != null) {
      addAppFeatureValuesToStream();
    } else {
      _appFeatureValuesBS
          .add(FeatureStatusFeatures(ApplicationFeatureValues()));
    }
  }

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

  void _actuallyCallAddAppFeatureValuesToStream() async {
    if (applicationId != null) {
      final appId = applicationId!;
      try {
        final environments =
            await _userStateServiceApi.getHiddenEnvironments(appId);
        final appFeatureValues = await _featureServiceApi
            .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(appId);
        if (!_appFeatureValuesBS.isClosed) {
          _sortApplicationFeatureValues(appFeatureValues);

          if (environments.environmentIds.isEmpty) {
            if (appFeatureValues.environments.isNotEmpty) {
              environments.environmentIds = [
                appFeatureValues.environments.first.environmentId!
              ];
              await _updateShownEnvironments(environments.environmentIds);
            }
          } else {
            _shownEnvironmentsSource.add(environments.environmentIds);
          }

          _appFeatureValuesBS.add(FeatureStatusFeatures(appFeatureValues));
        }
      } catch (e, s) {
        await mrClient.dialogError(e, s);
      }
    }
  }

  Future<void> _updateShownEnvironments(List<String> environmentIds) async {
    final envs = await _userStateServiceApi.saveHiddenEnvironments(
        applicationId!,
        HiddenEnvironments(
          environmentIds: environmentIds,
        ));
    _shownEnvironmentsSource.add(envs.environmentIds);
  }

  Future<void> addShownEnvironment(String envId) async {
    final envs = <String>[..._shownEnvironmentsSource.value!];
    envs.add(envId);
    // ignore: unawaited_futures
    _updateShownEnvironments(envs);
  }

  Future<void> removeShownEnvironment(String envId) async {
    final envs = <String>[..._shownEnvironmentsSource.value!];

    if (envs.remove(envId)) {
      // ignore: unawaited_futures
      _updateShownEnvironments(envs);
    }
  }

  bool environmentVisible(String envId) {
    return _shownEnvironmentsSource.value!.contains(envId);
  }

  void addAppFeatureValuesToStream() async {
    _getAllAppValuesDebounceStream.add(
        true); // tell it to ask for the data, but debounce it through a 300ms stream
  }

  void clearAppFeatureValuesStream() {
    if (!_appFeatureValuesBS.isClosed) {
      _appFeatureValuesBS.add(null);
    }
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
    clearAppFeatureValuesStream();

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
      if (appList != null && applicationId != null) {
        checkApplicationIdIsLegit(appList);
      }
    }
  }

  void checkApplicationIdIsLegit(List<Application> appList) {
    if (!appList.any((app) => app.id == applicationId)) {
      //if applicationId not found in the system - set it to null
      applicationId = null;
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
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
    addAppFeatureValuesToStream();
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
    addAppFeatureValuesToStream();
  }

  Future<void> getFeatureIncludingMetadata(Feature feature) async {
    _featureMetadataStream.add(null);
    final currentFeature =
    await _featureServiceApi.getFeatureByKey(applicationId!, feature.key!, includeMetaData: true);
    _featureMetadataStream.add(currentFeature);
  }

  Future<void> updateFeatureMetadata(Feature feature, String metaData) async {
    final currentFeature =
    await _featureServiceApi.getFeatureByKey(applicationId!, feature.key!, includeMetaData: true);
    final newFeature = currentFeature
      ..metaData = metaData;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId!, feature.key!, newFeature);
    await getFeatureIncludingMetadata(newFeature);
  }

  Future<void> deleteFeature(String key) async {
    await _featureServiceApi.deleteFeatureForApplication(applicationId!, key);
    addAppFeatureValuesToStream();
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
  }

  @override
  void dispose() {
    _appSearchResultSource.close();
    _appFeatureValuesBS.close();
    _featureMetadataStream.close();
    _currentPid.cancel();
    _currentAppId.cancel();
  }

  Future<void> updateAllFeatureValuesByApplicationForKey(
      Feature feature, List<FeatureValue> updates) async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId!, feature.key!, updates);

    // get the data again
    _getAllAppValuesDebounceStream.add(true);
  }
}
