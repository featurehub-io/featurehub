import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/number/edit_number_value_container.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_card_widget.dart';

class NumberStrategyCard extends StatelessWidget {
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBloc strBloc;

  const NumberStrategyCard(
      {Key? key, this.rolloutStrategy, required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: strBloc.fvBloc.environmentIsLocked(
            strBloc.environmentFeatureValue.environmentId!),
        builder: (ctx, snap) {
          if (snap.hasData) {
            final canChangeValue = strBloc.environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            var editable = !snap.data! && canChangeValue;
            final enabled = editable && !snap.data!;
            return StrategyCardWidget(
              editable: editable,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              editableHolderWidget: EditNumberValueContainer(
                canEdit: editable,
                enabled: enabled,
                rolloutStrategy: rolloutStrategy,
                strBloc: strBloc,
              ),
            );
          } else {
            return const SizedBox.shrink();
          }
        });
  }
}
