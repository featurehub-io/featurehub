import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/fv_strategy_editor_provider.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';



class EditValueStrategyLinkButton extends StatelessWidget {
  const EditValueStrategyLinkButton({
    Key? key,
    required this.editable,
    required this.fvBloc,
    required this.rolloutStrategy,
  }) : super(key: key);

  final bool editable;
  final EditingFeatureValueBloc fvBloc;
  final RolloutStrategy rolloutStrategy;

  @override
  Widget build(BuildContext context) {
    return Material(
        type: MaterialType.transparency,
        child: IconButton(
            splashRadius: 20,
            mouseCursor:
                editable ? SystemMouseCursors.click : SystemMouseCursors.basic,
            icon: const Icon(Icons.edit, size: 16),
            onPressed: () => showDialog(
                context: context,
                builder: (_) {
                  return AlertDialog(
                      title: Text(editable
                          ? 'Edit split targeting rules'
                          : 'View split targeting rules'),
                      content: BlocProvider.builder(
                        creator: (_c, _b) =>
                            StrategyEditorBloc(rolloutStrategy.toEditing(), FeatureValueStrategyProvider(fvBloc)),
                        builder: (ctx, bloc) {
                          return StrategyEditingWidget(
                            bloc: bloc, editable: editable);
                        },
                      ));
                })));
  }
}
