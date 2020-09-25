import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class DeleteStrategyIconButton extends StatelessWidget {
  const DeleteStrategyIconButton({
    Key key,
    @required this.editable,
    @required this.rolloutStrategy,
    @required this.strBloc,
  }) : super(key: key);

  final bool editable;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Material(
        type: MaterialType.transparency,
        child: IconButton(
          splashRadius: 20,
          mouseCursor: editable
              ? SystemMouseCursors.click
              : null,
          icon: Icon(Icons.delete, size: 16),
          onPressed: editable
              ? () => strBloc.removeStrategy(
              rolloutStrategy)
              : null,
        ),
      ),
    );
  }
}
