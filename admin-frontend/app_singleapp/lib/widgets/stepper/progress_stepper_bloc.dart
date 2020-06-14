import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

import 'FHStepper.dart';

class StepperBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  FHStepper fhStepper = FHStepper();
  String applicationId;

  final _portfoliosBS = BehaviorSubject<List<Portfolio>>();
  Stream<List<Portfolio>> get portfoliosList => _portfoliosBS.stream;

  final _appsBS = BehaviorSubject<List<Application>>();
  Stream<List<Application>> get appsList => _appsBS.stream;

  final _FHStepperBS = BehaviorSubject<FHStepper>();
  Stream<FHStepper> get stepper => _FHStepperBS.stream;

  StreamSubscription<String> _currentAppIdSubscriber;
  StreamSubscription<List<Group>> _currentPortfolioGroupsSubscriber;
  StreamSubscription<List<ServiceAccount>>
      _currentPortfolioServiceAccountsSubscriber;
  StreamSubscription<List<Environment>>
      _currentApplicationEnvironmentsSubscriber;
  StreamSubscription<List<Feature>> _currentApplicationFeaturesSubscriber;
  StreamSubscription<List<ServiceAccount>>
      _currentEnvironmentServiceAccountsSubscriber;
  StreamSubscription<List<Application>> _currentPortfolioApplicationsSubscriber;

  StepperBloc(this.mrClient) : assert(mrClient != null) {
    _setStreamListeners();
  }

  void _setStreamListeners() {
    _currentAppIdSubscriber =
        mrClient.streamValley.currentAppIdStream.listen(_getCurrentApplication);
    _currentPortfolioGroupsSubscriber = mrClient
        .streamValley.currentPortfolioGroupsStream
        .listen(_getPortfolioGroups);
    _currentPortfolioServiceAccountsSubscriber = mrClient
        .streamValley.currentPortfolioServiceAccountsStream
        .listen(_getPortfolioServiceAccounts);
    _currentPortfolioApplicationsSubscriber = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getPortfolioApplications);
    _currentApplicationEnvironmentsSubscriber = mrClient
        .streamValley.currentApplicationEnvironmentsStream
        .listen(_getApplicationEnvironments);
    _currentApplicationFeaturesSubscriber = mrClient
        .streamValley.currentApplicationFeaturesStream
        .listen(_getApplicationFeatures);
    _currentEnvironmentServiceAccountsSubscriber = mrClient
        .streamValley.currentEnvironmentServiceAccountStream
        .listen(_getEnvironmentServiceAccountPermissions);
  }

  //this is called each time current app ID stream from mrBloc emits a value
  void _getCurrentApplication(id) async {
    if (id != null) {
      fhStepper.application = true;
    } else {
      fhStepper.application = false;
    }
    _FHStepperBS.add(fhStepper);
    applicationId = id;
  }

  Future<void> _getPortfolioApplications(List<Application> appList) async {
    _appsBS.add(appList);
  }

  void _getPortfolioGroups(List<Group> groups) {
    fhStepper.group = groups.isNotEmpty;
    _FHStepperBS.add(fhStepper);
  }

  void _getPortfolioServiceAccounts(List<ServiceAccount> accounts) {
    fhStepper.serviceAccount = accounts.isNotEmpty;
    _FHStepperBS.add(fhStepper);
  }

  void _getApplicationEnvironments(List<Environment> envList) {
    if (applicationId != null) //maybe check with app id from mr
    {
      fhStepper.environment = envList.isNotEmpty;
      if (envList.isNotEmpty) {
        fhStepper.groupPermission =
            envList.any((env) => env.groupRoles.isNotEmpty);
      }
      _FHStepperBS.add(fhStepper);
    }
  }

  void _getApplicationFeatures(List<Feature> featureList) {
    if (applicationId != null) {
      fhStepper.feature = featureList.isNotEmpty;
      _FHStepperBS.add(fhStepper);
    }
  }

  Future<void> _getEnvironmentServiceAccountPermissions(
      List<ServiceAccount> saList) async {
    if (saList.isNotEmpty) {
      fhStepper.serviceAccountPermission =
          saList.any((sa) => sa.permissions.isNotEmpty);
    } else {
      fhStepper.serviceAccountPermission = false;
    }

    _FHStepperBS.add(fhStepper);
  }

  @override
  void dispose() {
    _portfoliosBS.close();
    _appsBS.close();
    _FHStepperBS.close();
    _currentAppIdSubscriber.cancel();
    _currentPortfolioServiceAccountsSubscriber.cancel();
    _currentPortfolioGroupsSubscriber.cancel();
    _currentApplicationEnvironmentsSubscriber.cancel();
    _currentApplicationFeaturesSubscriber.cancel();
    _currentEnvironmentServiceAccountsSubscriber.cancel();
    _currentPortfolioApplicationsSubscriber.cancel();
  }
}
