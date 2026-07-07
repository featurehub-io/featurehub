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
  final RolloutStrategyInstance? portfolioRolloutStrategy;
  final EditingFeatureValueBloc strBloc;
  final FeatureValueType featureValueType;

  const StrategyCard(
      {super.key,
      this.rolloutStrategy,
      required this.strBloc,
      required this.featureValueType,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy, this.portfolioRolloutStrategy});

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: strBloc.currentFv,
        builder: (ctx, snap) {
          if (snap.hasData) {
            final editable = strBloc.environmentFeatureValue.roles
                .contains(RoleType.CHANGE_VALUE);
            final unlocked = !snap.data!.locked;
            final editContainer = EditValueContainer(
              key: ValueKey(
                  '${strBloc.environmentFeatureValue.environmentId}-${strBloc.feature.key}'),
              editable: groupRolloutStrategy != null ? false : editable,
              unlocked: unlocked,
              rolloutStrategy: rolloutStrategy,
              groupRolloutStrategy: groupRolloutStrategy,
              applicationRolloutStrategy: applicationRolloutStrategy,
              portfolioRolloutStrategy: portfolioRolloutStrategy,
              strBloc: strBloc,
              featureValueType: featureValueType,
            );

            final canEdit = editable && unlocked;

            if (groupRolloutStrategy != null) {
              return GroupRolloutStrategyCardWidget(editable: canEdit, strBloc: strBloc, strategy: groupRolloutStrategy!, editableHolderWidget:  editContainer,);
            }
            if (applicationRolloutStrategy != null) {
              return ApplicationRolloutStrategyCardWidget(editable: canEdit, strBloc: strBloc,  strategyInstance: applicationRolloutStrategy!, editableHolderWidget: editContainer);
            }
            if (rolloutStrategy != null) {
              return RolloutStrategyCardWidget(editable: canEdit, strBloc: strBloc, strategy: rolloutStrategy!, editableHolderWidget: editContainer);
            }
            if (portfolioRolloutStrategy != null) {
              return PortfolioRolloutStrategyCardWidget(editable: canEdit, strBloc: strBloc, strategyInstance: portfolioRolloutStrategy!, editableHolderWidget: editContainer);
            }

            return NullRolloutStrategyCardWidget(strBloc: strBloc, editableHolderWidget: editContainer);
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
  final RolloutStrategyInstance? portfolioRolloutStrategy;
  final EditingFeatureValueBloc strBloc;

  const EditValueContainer(
      {super.key,
      required this.featureValueType,
      required this.editable,
      required this.unlocked,
      this.rolloutStrategy,
      required this.strBloc,
      this.groupRolloutStrategy,
      this.applicationRolloutStrategy, this.portfolioRolloutStrategy});

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
          portfolioRolloutStrategy: portfolioRolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.BOOLEAN:
        return EditBooleanValueDropDownWidget(
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          portfolioRolloutStrategy: portfolioRolloutStrategy,
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
          portfolioRolloutStrategy: portfolioRolloutStrategy,
          strBloc: strBloc,
        );
      case FeatureValueType.JSON:
        return EditJsonValueContainer(
          canEdit: editable,
          unlocked: unlocked,
          rolloutStrategy: rolloutStrategy,
          groupRolloutStrategy: groupRolloutStrategy,
          applicationRolloutStrategy: applicationRolloutStrategy,
          portfolioRolloutStrategy: portfolioRolloutStrategy,
          strBloc: strBloc,
        );
    }
  }
}
