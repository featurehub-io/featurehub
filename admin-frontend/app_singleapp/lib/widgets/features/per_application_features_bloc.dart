import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:app_singleapp/api/mr_client_aware.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:pedantic/pedantic.dart';
import 'package:rxdart/rxdart.dart' hide Notification;

class FeatureStatusFeatures {
  final ApplicationFeatureValues applicationFeatureValues;
  List<String> sortedByNameEnvironmentIds;
  // envId, EnvironmentFeatureValues mapping - so it is done only once not once per line in table
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};

  FeatureStatusFeatures(this.applicationFeatureValues) {
    sortedByNameEnvironmentIds = applicationFeatureValues.environments
        .map((e) => e.environmentId)
        .toList();

    applicationFeatureValues.environments.forEach((e) {
      applicationEnvironments[e.environmentId] = e;
    });
  }
}

///
/// This holds state relative to the whole set of features across the entire application
/// so application level stuff should happen here.
///
class PerApplicationFeaturesBloc
    implements Bloc, ManagementRepositoryAwareBloc {
  String portfolioId;
  String applicationId;
  final ManagementRepositoryClientBloc _mrClient;
  ApplicationServiceApi _appServiceApi;
  EnvironmentServiceApi _environmentServiceApi;
  UserStateServiceApi _userStateServiceApi;

  FeatureServiceApi _featureServiceApi;

  final _appSearchResultSource = BehaviorSubject<List<Application>>();
  Stream<List<Application>> get applications => _appSearchResultSource.stream;

  final _shownEnvironmentsSource = BehaviorSubject<List<String>>();
  Stream<List<String>> get shownEnvironmentsStream =>
      _shownEnvironmentsSource.stream;

  final _appFeatureValuesBS = BehaviorSubject<FeatureStatusFeatures>();
  Stream<FeatureStatusFeatures> get appFeatureValues =>
      _appFeatureValuesBS.stream;
  // feature-id, environments for feature

  StreamSubscription<String> _currentPid;
  StreamSubscription<String> _currentAppId;

  final _publishNewFeatureSource = PublishSubject<Feature>();

  Stream<Feature> get publishNewFeatureStream =>
      _publishNewFeatureSource.stream;

  final _getAllAppValuesDebounceStream = BehaviorSubject<bool>();

  PerApplicationFeaturesBloc(this._mrClient) : assert(_mrClient != null) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(_mrClient.apiClient);
    _environmentServiceApi = EnvironmentServiceApi(_mrClient.apiClient);
    _userStateServiceApi = UserStateServiceApi(_mrClient.apiClient);

    _currentPid = _mrClient.streamValley.currentPortfolioIdStream
        .listen(addApplicationsToStream);
    _currentAppId = _mrClient.streamValley.currentAppIdStream.listen(setAppId);

    _getAllAppValuesDebounceStream
        .debounceTime(Duration(milliseconds: 300))
        .listen((event) {
      _actuallyCallAddAppFeatureValuesToStream();
    });
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  void setAppId(String appId) {
    applicationId = appId;
    if (applicationId != null) {
      addAppFeatureValuesToStream();
    } else {
      _appFeatureValuesBS
          .add(FeatureStatusFeatures(ApplicationFeatureValues()));
    }
  }

  Future<Environment> getEnvironment(String envId) async {
    return _environmentServiceApi
        .getEnvironment(envId,
            includeServiceAccounts: true, includeSdkUrl: true)
        .catchError(mrClient.dialogError);
  }

  void _sortApplicationFeatureValues(
      ApplicationFeatureValues appFeatureValues) {
//    appFeatureValues.environments.sort((a, b) => a.environmentName.compareTo(b.environmentName));
    appFeatureValues.features.sort((a, b) => a.name.compareTo(b.name));
  }

  void _actuallyCallAddAppFeatureValuesToStream() async {
    if (applicationId != null) {
      try {
        final environments =
            await _userStateServiceApi.getHiddenEnvironments(applicationId);
        final appFeatureValues = await _featureServiceApi
            .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
                applicationId);
        if (!_appFeatureValuesBS.isClosed) {
          _sortApplicationFeatureValues(appFeatureValues);

          if (environments.environmentIds.isEmpty) {
            if (appFeatureValues.environments.isNotEmpty) {
              environments.environmentIds = [
                appFeatureValues.environments.first.environmentId
              ];
              await _updateShownEnvironments(environments.environmentIds);
            }
          } else {
            _shownEnvironmentsSource.add(environments.environmentIds);
          }

          _appFeatureValuesBS.add(FeatureStatusFeatures(appFeatureValues));
        }
      } catch (e, s) {
        mrClient.dialogError(e, s);
      }
    }
  }

  void _updateShownEnvironments(List<String> environmentIds) async {
    final envs = await _userStateServiceApi.saveHiddenEnvironments(
        applicationId, HiddenEnvironments()..environmentIds = environmentIds);
    _shownEnvironmentsSource.add(envs.environmentIds);
  }

  void addShownEnvironment(String envId) async {
    final envs = <String>[..._shownEnvironmentsSource.value];
    envs.add(envId);
    _updateShownEnvironments(envs);
  }

  void removeShownEnvironment(String envId) async {
    final envs = <String>[..._shownEnvironmentsSource.value];

    if (envs.remove(envId)) {
      _updateShownEnvironments(envs);
    }
  }

  bool environmentVisible(String envId) {
    return _shownEnvironmentsSource.value.contains(envId);
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

  Future<void> addApplicationsToStream(String pid) async {
    portfolioId = pid;
    clearAppFeatureValuesStream();

    if (pid != null) {
      List<Application> appList;
      try {
        appList = await _appServiceApi.findApplications(portfolioId,
            order: SortOrder.ASC);
        if (!_appSearchResultSource.isClosed) {
          _appSearchResultSource.add(appList);
        }
      } catch (e, s) {
        mrClient.dialogError(e, s);
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
      mrClient.customError(
          messageTitle: 'Provided application ID is not found in the system');
    }
  }

  Future<void> createFeature(
      String name,
      String key,
      FeatureValueType featureValueType,
      String featureAlias,
      String featureLink) async {
    final feature = Feature()
      ..name = name
      ..valueType = featureValueType
      ..key = key
      ..alias = featureAlias
      ..link = featureLink;
    await _featureServiceApi.createFeaturesForApplication(
        applicationId, feature);
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
    addAppFeatureValuesToStream();
    _publishNewFeatureSource.add(feature);
  }

  Future<void> updateFeature(Feature feature, String newName, String newKey,
      String newFeatureAlias, String newFeatureLink) async {
    final currentFeature =
        await _featureServiceApi.getFeatureByKey(applicationId, feature.key);
    final newFeature = currentFeature
      ..name = newName
      ..alias = newFeatureAlias
      ..link = newFeatureLink
      ..key = newKey;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId, feature.key, newFeature);
    addAppFeatureValuesToStream();
  }

  Future<void> deleteFeature(String key) async {
    await _featureServiceApi.deleteFeatureForApplication(applicationId, key);
    addAppFeatureValuesToStream();
    unawaited(mrClient.streamValley.getCurrentApplicationFeatures());
  }

  @override
  void dispose() {
    _appSearchResultSource.close();
    _appFeatureValuesBS.close();
    _currentPid.cancel();
    _currentAppId.cancel();
  }

  void updateAllFeatureValuesByApplicationForKey(
      Feature feature, List<FeatureValue> updates) async {
    await _featureServiceApi.updateAllFeatureValuesByApplicationForKey(
        applicationId, feature.key, updates);

    // get the data again
    _getAllAppValuesDebounceStream.add(true);
  }
}
