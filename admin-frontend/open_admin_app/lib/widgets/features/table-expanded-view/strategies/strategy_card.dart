import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_blocV2.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/boolean/edit_boolean_value_dropdown_widget.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/json/edit_json_value_container.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/number/edit_number_value_container.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/strategies/strategy_card_widget.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/string/edit_string_value_container.dart';

class StrategyCard extends StatelessWidget {
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;
  final FeatureValueType featureValueType;

  const StrategyCard(
      {Key? key,
      this.rolloutStrategy,
      required this.strBloc,
      required this.featureValueType})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: strBloc.fvBloc.currentFv,
        builder: (ctx, snap) {
          if (snap.hasData) {
            final editable = strBloc.environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            final unlocked = !snap.data!.locked;
            return StrategyCardWidget(
              editable: editable,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              editableHolderWidget: EditValueContainer(
                key: ValueKey(strBloc.environmentFeatureValue.environmentId! +
                    '-' +
                    strBloc.feature.key!),
                editable: editable,
                unlocked: unlocked,
                rolloutStrategy: rolloutStrategy,
                strBloc: strBloc,
                featureValueType: featureValueType,
              ),
            );
          } else {
            return const SizedBox.shrink();
          }
        });
  }
}

class EditValueContainer extends StatelessWidget {
  final FeatureValueType featureValueType;
  final bool editable;
  final bool unlocked;
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBlocV2 strBloc;

  const EditValueContainer(
      {Key? key,
      required this.featureValueType,
      required this.editable,
      required this.unlocked,
      this.rolloutStrategy,
      required this.strBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    switch (featureValueType) {
      case FeatureValueType.STRING:
        return EditStringValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.BOOLEAN:
        return EditBooleanValueDropDownWidget(
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          strBloc: strBloc, editable: editable,
        );
      case FeatureValueType.NUMBER:
        return EditNumberValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.JSON:
        return EditJsonValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          strBloc: strBloc,
        );
      default:
        return const SizedBox.shrink();
    }
  }
}
