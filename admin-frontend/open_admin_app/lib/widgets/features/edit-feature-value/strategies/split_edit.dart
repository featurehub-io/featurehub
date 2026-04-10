import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/generated/l10n/app_localizations.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/fv_strategy_editor_provider.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/strategyeditor/editing_rollout_strategy.dart';
import 'package:open_admin_app/widgets/strategyeditor/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/strategyeditor/strategy_editing_widget.dart';

class EditValueStrategyLinkButton extends StatelessWidget {
  const EditValueStrategyLinkButton({
    super.key,
    required this.editable,
    required this.fvBloc,
    required this.rolloutStrategy,
  });

  final bool editable;
  final EditingFeatureValueBloc fvBloc;
  final RolloutStrategy rolloutStrategy;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return IconButton(
        mouseCursor:
            editable ? SystemMouseCursors.click : SystemMouseCursors.basic,
        icon: const Icon(Icons.edit, size: 16),
        onPressed: () => showDialog(
            context: context,
            builder: (_) {
              return AlertDialog(
                  title: Text(editable
                      ? l10n.editSplitTargetingRules
                      : l10n.viewSplitTargetingRules),
                  content: BlocProvider.builder(
                    creator: (c, b) => StrategyEditorBloc(
                        rolloutStrategy.toEditing(),
                        FeatureValueStrategyProvider(fvBloc)),
                    builder: (ctx, bloc) {
                      return StrategyEditingWidget(
                          bloc: bloc, editable: editable);
                    },
                  ));
            }));
  }
}
