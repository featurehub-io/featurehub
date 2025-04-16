import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/strategies/strategy_card_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_boolean_value_dropdown_widget.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_json_value_container.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_number_value_container.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/valueeditors/edit_string_value_container.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

class StrategyCard extends StatelessWidget {
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;
  final FeatureValueType featureValueType;

  const StrategyCard(
      {Key? key,
      this.rolloutStrategy,
      required this.strBloc,
      required this.featureValueType,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: strBloc.currentFv,
        builder: (ctx, snap) {
          if (snap.hasData) {
            final editable = strBloc.environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            final unlocked = !snap.data!.locked;
            return StrategyCardWidget(
              editable: editable && unlocked,
              strBloc: strBloc,
              rolloutStrategy: rolloutStrategy,
              groupRolloutStrategy: groupRolloutStrategy,
              applicationRolloutStrategy: applicationRolloutStrategy,
              editableHolderWidget: EditValueContainer(
                key: ValueKey(
                    '${strBloc.environmentFeatureValue.environmentId}-${strBloc.feature.key}'),
                editable: groupRolloutStrategy != null ? false : editable,
                unlocked: unlocked,
                rolloutStrategy: rolloutStrategy,
                groupRolloutStrategy: groupRolloutStrategy,
                applicationRolloutStrategy: applicationRolloutStrategy,
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
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  const EditValueContainer(
      {Key? key,
      required this.featureValueType,
      required this.editable,
      required this.unlocked,
      this.rolloutStrategy,
      required this.strBloc,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    switch (featureValueType) {
      case FeatureValueType.STRING:
        return EditStringValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.BOOLEAN:
        return EditBooleanValueDropDownWidget(
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          strBloc: strBloc,
          editable: editable,
        );
      case FeatureValueType.NUMBER:
        return EditNumberValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.JSON:
        return EditJsonValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          strBloc: strBloc,
        );
      default:
        return const SizedBox.shrink();
    }
  }
}
