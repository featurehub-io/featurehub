import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/boolean/edit_boolean_value_dropdown_widget.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/strategy_card_widget.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class BooleanStrategyCard extends StatelessWidget {
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;

  BooleanStrategyCard({Key key, this.rolloutStrategy, this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: strBloc.fvBloc
            .environmentIsLocked(strBloc.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          if (snap.hasData) {
            final canChangeValue = strBloc.environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            var editable = !snap.data && canChangeValue;
            return StrategyCardWidget(
              editable: editable,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              editableHolderWidget: EditBooleanValueDropDownWidget(
                editable: editable,
                rolloutStrategy: rolloutStrategy,
                strBloc: strBloc,
              ),
            );
          } else {
            return SizedBox.shrink();
          }
        });
  }
}
