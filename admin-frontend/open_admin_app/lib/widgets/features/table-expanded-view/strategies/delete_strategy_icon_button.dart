import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';

class DeleteStrategyIconButton extends StatelessWidget {
  const DeleteStrategyIconButton({
    Key? key,
    required this.editable,
    required this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool editable;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;

  @override
  Widget build(BuildContext context) {
    return Material(
      type: MaterialType.transparency,
      child: IconButton(
        splashRadius: 20,
        mouseCursor:
            editable ? SystemMouseCursors.click : SystemMouseCursors.basic,
        icon: const Icon(Icons.delete, size: 16),
        onPressed:
            editable ? () => strBloc.removeStrategy(rolloutStrategy) : null,
      ),
    );
  }
}
