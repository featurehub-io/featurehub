import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/fv_strategy_editor_provider.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class AddStrategyButton extends StatelessWidget {
  final EditingFeatureValueBloc bloc;

  const AddStrategyButton(
      {Key? key, required this.bloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return TextButton.icon(
        label: const Text('Add split targeting rules'),
        icon: const Icon(Icons.call_split_outlined),
        onPressed:() => showDialog(
                context: context,
                builder: (_) {
                  return AlertDialog(
                    title: Text('Add split targeting rule'),
                    content: BlocProvider.builder(
                      creator: (_c, _b) =>
                          StrategyEditorBloc(EditingRolloutStrategy.newStrategy(id: makeStrategyId(existing: bloc.featureValue.rolloutStrategies)),
                              FeatureValueStrategyProvider(bloc)),
                      builder: (ctx, bloc) {
                        return StrategyEditingWidget(
                            bloc: bloc, editable: true);
                      },
                    ),
                  );
                })
            );
  }
}
