import 'package:bloc_provider/bloc_provider.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/api/client_api.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:openapi_dart_common/openapi.dart';

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

  Future<void> addStrategy(EditingRolloutStrategy ers) async {
    RolloutStrategy? rs = ers.toRolloutStrategy(null);
    try {
      if (strId == null) {
        var appStrategy = CreateApplicationRolloutStrategy(
            name: rs!.name,
            percentage: rs.percentage,
            percentageAttributes: rs.percentageAttributes,
            attributes: rs.attributes);
        await _applicationRolloutStrategyServiceApi.createApplicationStrategy(
            mrBloc.currentAid!, appStrategy);
      } else {
        UpdateApplicationRolloutStrategy update =
            UpdateApplicationRolloutStrategy(
                name: rs!.name,
                percentage: rs.percentage,
                percentageAttributes: rs.percentageAttributes,
                attributes: rs.attributes);
        await _applicationRolloutStrategyServiceApi.updateApplicationStrategy(
            mrBloc.currentAid!, ers.id, update);
      }
    } catch (e, s) {
      if (e is ApiException && e.code == 409) {
        await mrBloc.dialogError(e, s,
            messageTitle:
                "Application strategy with name '${rs!.name}' already exists",
            showDetails: false);
      } else {
        await mrBloc.dialogError(e, s);
      }
    }
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
