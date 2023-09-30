import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

/// This only occurs on individual feature values

class DeleteStrategyIconButton extends StatelessWidget {
  const DeleteStrategyIconButton({
    Key? key,
    required this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final RolloutStrategy rolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  @override
  Widget build(BuildContext context) {
    return Material(
      type: MaterialType.transparency,
      child: IconButton(
        splashRadius: 20,
        mouseCursor:
            SystemMouseCursors.click,
        icon: const Icon(Icons.delete, size: 16),
        onPressed:
            () => strBloc.removeStrategy(rolloutStrategy),
      ),
    );
  }
}
