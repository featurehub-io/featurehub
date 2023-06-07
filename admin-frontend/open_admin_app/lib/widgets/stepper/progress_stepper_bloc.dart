import 'dart:async';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:logging/logging.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/stepper/fh_stepper.dart';
import 'package:rxdart/rxdart.dart';


var _log = Logger("stepper");

class StepperBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;

  FHStepper fhStepper = FHStepper();
  String? applicationId;

  final _portfoliosBS = BehaviorSubject<List<Portfolio>>();
  Stream<List<Portfolio>> get portfoliosList => _portfoliosBS.stream;

  final _appsBS = BehaviorSubject<List<Application>>();
  Stream<List<Application>> get appsList => _appsBS.stream;

  final _fhStepperBS = BehaviorSubject<FHStepper>();
  Stream<FHStepper> get stepper => _fhStepperBS.stream;

  late StreamSubscription<String?> _currentAppIdSubscriber;
  late StreamSubscription<List<Group>> _currentPortfolioGroupsSubscriber;
  late StreamSubscription<List<ServiceAccount>>
      _currentPortfolioServiceAccountsSubscriber;
  late StreamSubscription<List<Application>>
      _currentPortfolioApplicationsSubscriber;
  late StreamSubscription<bool> _rocketTriggerSubscriber;

  StepperBloc(this.mrClient) {
    _setStreamListeners();
  }

  void _setStreamListeners() {
    _currentAppIdSubscriber =
        mrClient.streamValley.currentAppIdStream.listen(_getCurrentApplication);
    _rocketTriggerSubscriber = mrClient.streamValley.rocketTrigger.listen(_rocketTrigger);
    _currentPortfolioGroupsSubscriber = mrClient
        .streamValley.currentPortfolioGroupsStream
        .listen(_getPortfolioGroups);
    _currentPortfolioServiceAccountsSubscriber = mrClient
        .streamValley.currentPortfolioServiceAccountsStream
        .listen(_getPortfolioServiceAccounts);
    _currentPortfolioApplicationsSubscriber = mrClient
        .streamValley.currentPortfolioApplicationsStream
        .listen(_getPortfolioApplications);
  }

  void _rocketTrigger(bool val) async {
    _rocketTriggerSubscriber.pause();

    await _getSummary(applicationId);
    _fhStepperBS.add(fhStepper);

    _rocketTriggerSubscriber.resume();
  }

  //this is called each time current app ID stream from mrBloc emits a value
  void _getCurrentApplication(String? id) async {
    _currentAppIdSubscriber.pause();

    try {
      applicationId = id;

      if (id != null) {
        fhStepper.application = true;
        await _getSummary(id);
      } else {
        fhStepper.application = false;
      }

      _fhStepperBS.add(fhStepper);
    } finally {
      _currentAppIdSubscriber.resume();
    }
  }

  Future<void> _getSummary(String? id) async {
    if (id == null) return;

    try {
      final summary = await mrClient.applicationServiceApi.summaryApplication(id);
      fhStepper.groupPermission = summary.groupsHavePermission;
      fhStepper.environment = summary.environmentCount > 0;
      fhStepper.feature = summary.featureCount > 0;
      fhStepper.serviceAccountPermission = summary.serviceAccountsHavePermission;
    } catch (e, s) {
      _log.severe("failed to request application details $e $s");
    }
  }

  void _getPortfolioApplications(List<Application> appList) {
    _appsBS.add(appList);
  }

  void _getPortfolioGroups(List<Group> groups) {
    fhStepper.group = groups.isNotEmpty;
    _fhStepperBS.add(fhStepper);
  }

  void _getPortfolioServiceAccounts(List<ServiceAccount> accounts) {
    fhStepper.serviceAccount = accounts.isNotEmpty;
    _fhStepperBS.add(fhStepper);
  }

  @override
  void dispose() {
    _rocketTriggerSubscriber.cancel();
    _currentAppIdSubscriber.cancel();
    _currentPortfolioServiceAccountsSubscriber.cancel();
    _currentPortfolioGroupsSubscriber.cancel();
    _currentPortfolioApplicationsSubscriber.cancel();
    _portfoliosBS.close();
    _appsBS.close();
    _fhStepperBS.close();
  }
}
