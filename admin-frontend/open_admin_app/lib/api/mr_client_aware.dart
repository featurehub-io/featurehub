import 'client_api.dart';

/// a MR Client aware Bloc
abstract class ManagementRepositoryAwareBloc {
  ManagementRepositoryClientBloc get mrClient;
}
