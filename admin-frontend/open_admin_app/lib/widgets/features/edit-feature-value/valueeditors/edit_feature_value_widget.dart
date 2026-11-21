import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';

/// Base widget holding the fields common to all four feature-value editors.
abstract class EditFeatureValueWidget extends StatefulWidget {
  const EditFeatureValueWidget({
    super.key,
    required this.unlocked,
    required this.canEdit,
    this.rolloutStrategy,
    this.groupRolloutStrategy,
    this.applicationRolloutStrategy,
    this.portfolioRolloutStrategy,
    required this.strBloc,
  });

  final bool unlocked;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final ThinGroupRolloutStrategy? groupRolloutStrategy;
  final RolloutStrategyInstance? applicationRolloutStrategy;
  final RolloutStrategyInstance? portfolioRolloutStrategy;
  final EditingFeatureValueBloc strBloc;
}

/// Base state providing value-resolution and update-dispatch helpers.
abstract class EditFeatureValueState<W extends EditFeatureValueWidget>
    extends State<W> {
  /// True when this editor is for a strategy row rather than the feature's
  /// default value. Strategy rows must never fall back to the default value:
  /// a strategy whose value is not yet set should display as empty.
  bool get isStrategyRow =>
      widget.rolloutStrategy != null ||
      widget.groupRolloutStrategy != null ||
      widget.applicationRolloutStrategy != null ||
      widget.portfolioRolloutStrategy != null;

  /// Returns the strategy's value for a strategy row (which may be null when
  /// not yet set), or [defaultValue] for the feature's default row.
  dynamic resolveValue(dynamic defaultValue) =>
      isStrategyRow ? resolveStrategyValue() : defaultValue;

  /// Returns the value from whichever strategy is active, or null when none
  /// is active or the strategy's value is not set. Prefer [resolveValue].
  dynamic resolveStrategyValue() {
    if (widget.rolloutStrategy != null) return widget.rolloutStrategy!.value;
    if (widget.groupRolloutStrategy != null) {
      return widget.groupRolloutStrategy!.value;
    }
    if (widget.applicationRolloutStrategy != null) {
      return widget.applicationRolloutStrategy!.value;
    }
    if (widget.portfolioRolloutStrategy != null) {
      return widget.portfolioRolloutStrategy!.value;
    }
    return null;
  }

  /// Writes [value] to whichever strategy owns the current row and notifies
  /// the bloc. Group strategies are read-only and are never written here.
  void updateValue(dynamic value) {
    if (widget.rolloutStrategy != null) {
      widget.rolloutStrategy!.value = value;
      widget.strBloc.updateStrategyValue();
    } else if (widget.applicationRolloutStrategy != null) {
      widget.applicationRolloutStrategy!.value = value;
      widget.strBloc.updateApplicationStrategyValue();
    } else if (widget.portfolioRolloutStrategy != null) {
      widget.portfolioRolloutStrategy!.value = value;
      widget.strBloc.updatePortfolioStrategyValue();
    } else {
      widget.strBloc.updateFeatureValueDefault(value);
    }
  }
}
