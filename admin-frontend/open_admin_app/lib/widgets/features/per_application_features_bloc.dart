import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/api/mr_client_aware.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:rxdart/rxdart.dart' hide Notification;
import 'package:collection/collection.dart';

import 'feature_dashboard_constants.dart';

var _log = Logger("per-app-features");

class FeatureStatusFeatures {
  final ApplicationFeatureValues applicationFeatureValues;
  List<String> sortedByNameEnvironmentIds = [];

  // envId, EnvironmentFeatureValues mapping - so it is done only once not once per line in table
  Map<String, EnvironmentFeatureValues> applicationEnvironments = {};

  FeatureStatusFeatures(this.applicationFeatureValues) {
    sortedByNameEnvironmentIds = applicationFeatureValues.environments
        .map((e) => e.environmentId)
        .toList();

    for (var e in applicationFeatureValues.environments) {
      applicationEnvironments[e.environmentId] = e;
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
  late UserStateServiceApi _userStateServiceApi;
  late ApplicationRolloutStrategyServiceApi _rolloutStrategyServiceApi;

  late FeatureServiceApi _featureServiceApi;

  final _appSearchResultSource = BehaviorSubject<List<Application>?>();

  String searchFieldTerm = '';
  int totalFeatures = 0;
  int currentPageIndex = 0;
  int currentRowsPerPage = 5;
  List<FeatureValueType> selectedFeatureTypesByUser = [];
  List<String> selectedEnvironmentNamesByUser = [];

  Stream<List<Application>?> get applications => _appSearchResultSource.stream;

  final _shownEnvironmentsSource = BehaviorSubject<List<String>>.seeded([]);

  Stream<List<String>> get shownEnvironmentsStream =>
      _shownEnvironmentsSource.stream;

  // feature-id, environments for feature

  final _appFeatureValues = BehaviorSubject<ApplicationFeatureValues?>();

  Stream<ApplicationFeatureValues?> get appFeatureValuesStream =>
      _appFeatureValues.stream;

  late StreamSubscription<String?> _currentPid;
  late StreamSubscription<String?> _currentAppId;

  final _publishNewFeatureSource = PublishSubject<Feature>();

  Stream<Feature> get publishNewFeatureStream =>
      _publishNewFeatureSource.stream;

  final _featureMetadataStream = BehaviorSubject<Feature?>();

  Stream<Feature?> get featureMetadataStream => _featureMetadataStream.stream;

  PerApplicationFeaturesBloc(this._mrClient) {
    _appServiceApi = ApplicationServiceApi(_mrClient.apiClient);
    _featureServiceApi = FeatureServiceApi(_mrClient.apiClient);
    _userStateServiceApi = UserStateServiceApi(_mrClient.apiClient);
    _rolloutStrategyServiceApi = ApplicationRolloutStrategyServiceApi(_mrClient.apiClient);

    _currentPid = _mrClient.streamValley.currentPortfolioIdStream
        .listen(addApplicationsToStream);

    _currentAppId = _mrClient.streamValley.currentAppIdStream.listen(setAppId);
  }

  @override
  ManagementRepositoryClientBloc get mrClient => _mrClient;

  Future<void> setAppId(String? appId) async {
    _currentAppId.pause(); // as we are async, we tell the subscription to not let any other requests thru
    try {
      _log.fine("setAppId: $appId");
      applicationId = appId;

      if (applicationId != null) {
        await getApplicationFeatureValuesData(
            applicationId!, "", [], rowsPerPage, 0);
      }
    } finally {
      _currentAppId.resume();
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

  Future<void> updateShownEnvironments(List<String> environmentNames) async {
    List<String> envIds = [];

    if (_appFeatureValues.hasValue && _appFeatureValues.value != null) {
      envIds = _appFeatureValues.value!.environments
          .where((env) => environmentNames.contains(env.environmentName))
          .map((e) => e.environmentId)
          .toList();
    }

    // this is a deliberate act
    if (envIds.isEmpty) {}

    final envs = await _userStateServiceApi.saveHiddenEnvironments(
        applicationId!,
        HiddenEnvironments(
          noneSelected: envIds.isEmpty,
          environmentIds: envIds,
        ));
    _shownEnvironmentsSource.add(envs.environmentIds);
    selectedEnvironmentNamesByUser = environmentNames;
  }

  void clearAppFeatureValuesStream() {
    if (!_appFeatureValues.isClosed) {
      _appFeatureValues.add(null);
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
    _currentPid.pause();
    try {
      portfolioId = pid;
      clearAppFeatureValuesStream();

      if (pid != null) {
        List<Application>? appList;
        try {
          appList = await _appServiceApi.findApplications(pid,
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
    } finally {
      _currentPid.resume();
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
    final feature = CreateFeature(
        name: name,
        valueType: featureValueType,
        key: key,
        alias: featureAlias,
        link: featureLink,
        description: featureDescription);
    List<Feature> allFeatures = await _featureServiceApi.createFeaturesForApplication(
        applicationId!, feature);

    final feat = allFeatures.firstWhereOrNull((e) => e.key == key);

    if (feat != null) {
      mrClient.streamValley.triggerRocket();
      _publishNewFeatureSource.add(feat);
    }
  }

  Future<void> updateFeature(
      Feature feature,
      String newName,
      String newKey,
      String newFeatureAlias,
      String newFeatureLink,
      String newFeatureDescription) async {
    final currentFeature =
        await _featureServiceApi.getFeatureByKey(applicationId!, feature.key);
    final newFeature = currentFeature
      ..name = newName
      ..alias = newFeatureAlias
      ..link = newFeatureLink
      ..description = newFeatureDescription
      ..key = newKey;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId!, newFeature);
  }

  Future<void> getFeatureIncludingMetadata(Feature feature) async {
    _featureMetadataStream.add(null);
    final currentFeature = await _featureServiceApi
        .getFeatureByKey(applicationId!, feature.key, includeMetaData: true);
    _featureMetadataStream.add(currentFeature);
  }

  Future<void> updateFeatureMetadata(Feature feature, String metaData) async {
    final currentFeature = await _featureServiceApi
        .getFeatureByKey(applicationId!, feature.key, includeMetaData: true);
    final newFeature = currentFeature..metaData = metaData;
    await _featureServiceApi.updateFeatureForApplication(
        applicationId!, newFeature);
    await getFeatureIncludingMetadata(newFeature);
  }

  Future<void> deleteFeature(String key) async {
    await _featureServiceApi.deleteFeatureForApplication(applicationId!, key);
    // addAppFeatureValuesToStream();
    mrClient.streamValley.triggerRocket();
  }

  @override
  void dispose() {
    _currentPid.cancel();
    _currentAppId.cancel();
    _appSearchResultSource.close();
    _featureMetadataStream.close();
    _appFeatureValues.close();
  }

  Future<void> getApplicationFeatureValuesData(
      String appId,
      String searchTerm,
      List<FeatureValueType> featureTypes,
      int rowsPerPage,
      int pageOffset) async {
    _log.fine("started: getApplicationFeatureValuesData with $appId search $searchTerm");
    var allFeatureValues = await _featureServiceApi
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
      appId,
      max: rowsPerPage,
      page: pageOffset,
      filter: searchTerm,
      featureTypes: featureTypes,
    );
    await getShownEnvironmentNames(allFeatureValues);
    _appFeatureValues.add(allFeatureValues);
    // set current values
    searchFieldTerm = searchTerm;
    totalFeatures = allFeatureValues.maxFeatures;
    currentPageIndex = pageOffset;
    selectedFeatureTypesByUser = featureTypes;
    currentRowsPerPage = rowsPerPage;
    _log.fine("finished: getApplicationFeatureValuesData with $appId search $searchTerm");
  }

  updateApplicationFeatureValuesStream() async {
    var allFeatureValues = await _featureServiceApi
        .findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
      applicationId!,
      max: rowsPerPage,
      page: currentPageIndex,
      filter: searchFieldTerm,
      featureTypes: selectedFeatureTypesByUser,
    );
    _log.fine(
        "update application stream with $applicationId and filter $searchFieldTerm");
    _appFeatureValues.add(allFeatureValues);
  }

  Future<void> getShownEnvironmentNames(
      ApplicationFeatureValues allFeatureValues) async {
    HiddenEnvironments envs =
        await _userStateServiceApi.getHiddenEnvironments(applicationId!);

    // if they haven't chosen a list of environments to show, show them all
    final candidateEnvs =
        (envs.environmentIds.isEmpty && (envs.noneSelected != true))
            ? allFeatureValues.environments
            : allFeatureValues.environments
                .where((env) => envs.environmentIds.contains(env.environmentId))
                .toList();

    selectedEnvironmentNamesByUser =
        candidateEnvs.map((e) => e.environmentName).toList();
  }

  EditingFeatureValueBloc perFeatureStateTrackingBloc(
      Feature feature, FeatureValue featureValue, EnvironmentFeatureValues environmentFeatureValue) {
    return EditingFeatureValueBloc(
        applicationId!, feature, featureValue, environmentFeatureValue, this, _appFeatureValues.value!);
  }
}
