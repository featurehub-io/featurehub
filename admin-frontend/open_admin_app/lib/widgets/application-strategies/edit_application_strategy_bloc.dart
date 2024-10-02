import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';

class EditApplicationStrategyBloc implements Bloc {
  final ManagementRepositoryClientBloc mrBloc;
  String? strId;
  late ApplicationRolloutStrategyServiceApi
      _applicationRolloutStrategyServiceApi;
  late ApplicationRolloutStrategy applicationRolloutStrategy;
  late RolloutStrategy rolloutStrategy;

  EditApplicationStrategyBloc(this.mrBloc, {String? strategyId}) {
    strId = strategyId;
    _applicationRolloutStrategyServiceApi =
        ApplicationRolloutStrategyServiceApi(mrBloc.apiClient);
  }

  @override
  void dispose() {}

  Future<RolloutStrategy> getStrategy(String? strategyId) async {
    String? appId = mrBloc.getCurrentAid();
    if (appId != null && strategyId != null) {
      try {
        applicationRolloutStrategy = await _applicationRolloutStrategyServiceApi
            .getApplicationStrategy(appId, strategyId);
      } catch (e, s) {
        await mrBloc.dialogError(e, s,
            showDetails: false, messageTitle: "Strategy does not exist");
      }
      rolloutStrategy = RolloutStrategy(
          id: applicationRolloutStrategy.id,
          name: applicationRolloutStrategy.name,
          percentage: applicationRolloutStrategy.percentage,
          attributes: applicationRolloutStrategy.attributes);
    }
    return rolloutStrategy;
  }

  void addStrategy(EditingRolloutStrategy ers) {
    RolloutStrategy? rs = ers.toRolloutStrategy(null);
    var appStrategy = CreateApplicationRolloutStrategy(
        name: rs!.name,
        percentageAttributes: rs.percentageAttributes,
        attributes: rs.attributes);
    _applicationRolloutStrategyServiceApi.createApplicationStrategy(
        mrBloc.currentAid!, appStrategy);
  }

  Future<RolloutStrategyValidationResponse> validationCheck(strategy) async {
    var rs = RolloutStrategy(
        id: strategy.id,
        name: strategy.name,
        percentage: strategy.percentage,
        attributes: strategy.attributes);

    List<RolloutStrategy> strategies = [];
    strategies.add(rs);

    return _applicationRolloutStrategyServiceApi.validate(
        mrBloc.currentAid!,
        RolloutStrategyValidationRequest(
          customStrategies: strategies,
          sharedStrategies: <RolloutStrategyInstance>[],
        ));
  }
}
