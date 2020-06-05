import 'dart:async';

import 'package:app_singleapp/api/client_api.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:rxdart/rxdart.dart';

enum PortfolioState { list, create, edit }

class PortfolioBloc implements Bloc {
  String search;
  final ManagementRepositoryClientBloc mrClient;
  PortfolioServiceApi _portfolioServiceApi;

  Stream<List<Portfolio>> get portfolioSearch =>
      _portfolioSearchResultSource.stream;
  final _portfolioSearchResultSource = BehaviorSubject<List<Portfolio>>();

  PortfolioBloc(this.search, this.mrClient) : assert(mrClient != null) {
    _portfolioServiceApi = PortfolioServiceApi(mrClient.apiClient);
    triggerSearch(search);
  }

  void triggerSearch(String s) async {
    // this should also change the url

    // debounce the search (i.e. if they are still typing, wait)
    final String newSearch = s;
    this.search = s;

    await Timer(Duration(milliseconds: 300), () {
      if (newSearch == search) {
        // hasn't changed
        _requestSearch(); // don't need to await it, async is fine
      }
    });
  }

  Future deletePortfolio(
      String portfolioId, bool includeGroups, bool includeApplications) async {
    bool success = await mrClient.portfolioServiceApi.deletePortfolio(
        portfolioId,
        includeGroups: includeGroups,
        includeApplications: includeApplications);

    if (success) {
      // let this happen async
      mrClient.refreshPortfolios();
    }

    return success;
  }

  // this really runs the search after we have debounced it
  void _requestSearch() async {
    if (search != null && search.length > 1) {
      // wait for global error handling to wrap this in try/catch
      var data = await _portfolioServiceApi.findPortfolios(
          order: SortOrder.ASC, filter: search, includeGroups: true);

      // publish it out...
      _portfolioSearchResultSource.add(data);
    } else if (search == null || search.isEmpty) {
      // this should paginate one presumes
      var data = await _portfolioServiceApi.findPortfolios(
          order: SortOrder.ASC, includeGroups: true);

      // publish it out...
      _portfolioSearchResultSource.add(data);
    }
  }

  Future createPortfolio(Portfolio portfolio) {
    return _portfolioServiceApi.createPortfolio(portfolio);
  }

  Future updatePortfolio(Portfolio portfolio) {
    return _portfolioServiceApi.updatePortfolio(portfolio.id, portfolio);
  }

  void savePortfolio(String portfolioName) async {
    await _portfolioServiceApi
        .createPortfolio(Portfolio()..name = portfolioName);
  }

  @override
  void dispose() {
    _portfolioSearchResultSource.close();
  }
}
