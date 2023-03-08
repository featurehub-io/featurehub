import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/individual_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/strategy_editing_widget.dart';
import 'package:open_admin_app/widgets/features/per_feature_state_tracking_bloc.dart';

class EditValueStrategyLinkButton extends StatelessWidget {
  const EditValueStrategyLinkButton({
    Key? key,
    required this.editable,
    required this.fvBloc,
    required this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool editable;
  final PerFeatureStateTrackingBloc fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

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
                  content: BlocProvider(
                    creator: (_c, _b) => IndividualStrategyBloc(
                        strBloc.environmentFeatureValue, rolloutStrategy),
                    child: StrategyEditingWidget(
                        bloc: strBloc, editable: editable),
                  ));
            })));
  }
}
