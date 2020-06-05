import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

class FeaturesLatestBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;
  List<Portfolio> portfolios;

  EnvironmentFeatureServiceApi _environmentFeatureServiceApi;
  PortfolioServiceApi _portfolioServiceApi;

  final _featuresListBS = BehaviorSubject<EnvironmentFeaturesResult>();
  Stream<EnvironmentFeaturesResult> get featuresListStream => _featuresListBS.stream;

  FeaturesLatestBloc(this.mrClient) : assert(mrClient != null) {
    _environmentFeatureServiceApi = EnvironmentFeatureServiceApi(mrClient.apiClient);
    _portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    initialise();
  }

  @override
  void dispose() {
    _featuresListBS.close();
  }

  void initialise() async{
    await _findPortfolios();
    await _getLatestFeatures();
  }

  Future<void> _getLatestFeatures() async {
   _featuresListBS.add(await _environmentFeatureServiceApi.getFeaturesForEnvironment('latest').catchError(mrClient.dialogError));
  }

  Future<void> _findPortfolios() async {
    portfolios = await _portfolioServiceApi.findPortfolios().catchError(mrClient.dialogError);
  }
}
