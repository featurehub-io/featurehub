import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/fhos_logger.dart';
import 'package:rxdart/rxdart.dart';

class FeatureFilterBloc implements Bloc {
  final ManagementRepositoryClientBloc mrClient;
  String? search;
  int currentMax = 100;
  int currentPage = 0;
  SortOrder currentSortOrder = SortOrder.ASC;

  final _filterResultSource = BehaviorSubject<SearchFeatureFilterResult?>();
  Stream<SearchFeatureFilterResult?> get filterResultStream => _filterResultSource.stream;

  final _matchingResultsSource = BehaviorSubject<MatchingFilterResults?>();
  Stream<MatchingFilterResults?> get matchingResultsStream => _matchingResultsSource.stream;

  late StreamSubscription<String?> _portfolioIdSubscription;

  FeatureFilterBloc(this.mrClient) {
    fhosLogger.fine("creating feature filter bloc");
    _portfolioIdSubscription = mrClient.streamValley.currentPortfolioIdStream.listen((pid) {
      if (pid != null) {
        refreshFilters();
      }
    });
  }

  void setSearch(String? s) {
    search = s;
    currentPage = 0;
    refreshFilters();
  }

  void setPagination(int max, int page) {
    currentMax = max;
    currentPage = page;
    refreshFilters();
  }

  Future<void> refreshFilters() async {
    final pid = mrClient.currentPid;
    if (pid == null) return;

    try {
      final result = await mrClient.featureFilterServiceApi.findFeatureFilters(
        pid,
        filter: search,
        max: currentMax,
        page: currentPage,
        sortOrder: currentSortOrder,
        includeDetails: true,
      );
      _filterResultSource.add(result);
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  Future<FeatureFilter?> createFilter(String name, String description) async {
    final pid = mrClient.currentPid;
    if (pid == null) return null;

    try {
      final filter = await mrClient.featureFilterServiceApi.createFeatureFilter(
        pid,
        CreateFeatureFilter(name: name, description: description),
      );
      refreshFilters();
      return filter;
    } catch (e, s) {
      mrClient.dialogError(e, s);
      return null;
    }
  }

  Future<FeatureFilter?> updateFilter(FeatureFilter filter, String name, String description) async {
    final pid = mrClient.currentPid;
    if (pid == null) return null;

    try {
      final updated = await mrClient.featureFilterServiceApi.updateFeatureFilter(
        pid,
        filter..name = name..description = description,
      );
      refreshFilters();
      return updated;
    } catch (e, s) {
      mrClient.dialogError(e, s);
      return null;
    }
  }

  Future<bool> deleteFilter(FeatureFilter filter) async {
    final pid = mrClient.currentPid;
    if (pid == null) return false;

    try {
      await mrClient.featureFilterServiceApi.deleteFeatureFilter(pid, filter.id, filter.version);
      refreshFilters();
      return true;
    } catch (e, s) {
      mrClient.dialogError(e, s);
      return false;
    }
  }

  Future<void> findMatchingResults(List<String> filterIds, MatchTypeEnum matchType) async {
    final pid = mrClient.currentPid;
    if (pid == null) return;

    if (filterIds.isEmpty) {
      _matchingResultsSource.add(null);
      return;
    }

    try {
      final results = await mrClient.featureFilterServiceApi.getMatchingFilters(
        pid,
        filterIds,
        matchType,
      );

      _matchingResultsSource.add(results);
    } catch (e, s) {
      mrClient.dialogError(e, s);
    }
  }

  @override
  void dispose() {
    _portfolioIdSubscription.cancel();
    _filterResultSource.close();
    _matchingResultsSource.close();
  }

  Future<SearchFeatureFilterResult> findFeatureFilters({required String filter}) async {
    final pid = mrClient.currentPid;
    if (pid == null) return SearchFeatureFilterResult(pagination: PaginationResult(total: 0, page: 0, pageSize: 0), filters: []);

    return mrClient.featureFilterServiceApi.findFeatureFilters(pid, filter: filter);
  }
}
