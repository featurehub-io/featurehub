import 'dart:async';

import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:openapi_dart_common/openapi.dart';

class EditPortfolioStrategyBloc implements Bloc {
  final ManagementRepositoryClientBloc mrBloc;
  String? strId;
  String? portfolioId;
  late PortfolioRolloutStrategyServiceApi _portfolioRolloutStrategyServiceApi;
  late PortfolioRolloutStrategy portfolioRolloutStrategy;
  late RolloutStrategy rolloutStrategy;

  EditPortfolioStrategyBloc(this.mrBloc,
      {String? strategyId, required this.portfolioId}) {
    strId = strategyId;
    _portfolioRolloutStrategyServiceApi =
        PortfolioRolloutStrategyServiceApi(mrBloc.apiClient);
  }

  @override
  void dispose() {}

  Future<RolloutStrategy> getStrategy(
      String? strategyId, String? portfolioId) async {
    if (portfolioId != null && strategyId != null) {
      try {
        portfolioRolloutStrategy = await _portfolioRolloutStrategyServiceApi
            .getPortfolioStrategy(portfolioId, strategyId);
      } catch (e, s) {
        await mrBloc.dialogError(e, s,
            showDetails: false,
            messageTitle:
                'We could not load a strategy you are looking for. Please check the provided URL and try again.');
      }
      rolloutStrategy = RolloutStrategy(
          id: portfolioRolloutStrategy.id,
          name: portfolioRolloutStrategy.name,
          percentage: portfolioRolloutStrategy.percentage,
          percentageAttributes: portfolioRolloutStrategy.percentageAttributes,
          attributes: portfolioRolloutStrategy.attributes);
    }
    return rolloutStrategy;
  }

  Future<void> addStrategy(EditingRolloutStrategy ers) async {
    RolloutStrategy? rs = ers.toRolloutStrategy(null);
    try {
      if (strId == null) {
        var portfolioStrategy = CreatePortfolioRolloutStrategy(
            name: rs!.name,
            percentage: rs.percentage,
            percentageAttributes: rs.percentageAttributes,
            attributes: rs.attributes);
        await _portfolioRolloutStrategyServiceApi.createPortfolioStrategy(
            mrBloc.currentPid!, portfolioStrategy);
      } else {
        UpdatePortfolioRolloutStrategy update = UpdatePortfolioRolloutStrategy(
            name: rs!.name,
            percentage: rs.percentage,
            percentageAttributes: rs.percentageAttributes,
            attributes: rs.attributes);
        await _portfolioRolloutStrategyServiceApi.updatePortfolioStrategy(
            mrBloc.currentPid!, ers.id, update);
      }
    } catch (e, s) {
      if (e is ApiException && e.code == 409) {
        await mrBloc.dialogError(e, s,
            messageTitle:
                "Portfolio strategy with name '${rs!.name}' already exists",
            showDetails: false);
      } else {
        await mrBloc.dialogError(e, s);
      }
    }
  }

  Future<RolloutStrategyValidationResponse> validationCheck(
      EditingRolloutStrategy strategy) async {
    var rs = strategy.toRolloutStrategy(null)!;

    List<RolloutStrategy> strategies = [];
    strategies.add(rs);

    return _portfolioRolloutStrategyServiceApi.portfolioStrategyValidate(
        mrBloc.currentPid!,
        RolloutStrategyValidationRequest(
          customStrategies: strategies,
          sharedStrategies: <RolloutStrategyInstance>[],
        ));
  }
}