import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/feature_value_status_tags.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/flag_colored_on_off_label.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/tooltip.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_not_set_container.dart';

class CollapsedViewValueCellHolder extends StatelessWidget {
  final FeatureValue? fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const CollapsedViewValueCellHolder(
      {Key? key, this.fv, required this.efv, required this.feature})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      return _ValueContainer(feature: feature, fv: fv);
    }
    if ((fv?.id == null) && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return const SizedBox.shrink();
  }
}

class _ValueContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue? fv;

  const _ValueContainer({required this.feature, this.fv});

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: AlignmentDirectional.center,
      children: [
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            if (fv != null) _ValueCard(feature: feature, fv: fv!),
            if (fv == null) const NotSetContainer(),
            if (fv != null && fv!.rolloutStrategies.isNotEmpty)
              _StrategiesList(feature: feature, fv: fv!)
          ],
        ),
        if (fv != null && fv!.locked) const LockedIndicator()
      ],
    );
  }
}

class LockedIndicator extends StatelessWidget {
  const LockedIndicator({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: cellWidth - 1,
      child: Align(
        alignment: Alignment.topLeft,
        child: Container(
          padding: const EdgeInsets.all(8.0),
//          color: Colors.black.withOpacity(0.1),
          child: const Icon(Icons.lock_outline, size: 16.0, color: Colors.red),
        ),
      ),
    );
  }
}

class _StrategiesList extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const _StrategiesList({Key? key, required this.feature, required this.fv})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (RolloutStrategy rsi in fv.rolloutStrategies)
          _ValueCard(rolloutStrategy: rsi, fv: fv, feature: feature)
      ],
    );
  }
}

class _ValueCard extends StatelessWidget {
  final FeatureValue fv;
  final Feature feature;
  final RolloutStrategy? rolloutStrategy;

  const _ValueCard({
    Key? key,
    required this.fv,
    required this.feature,
    this.rolloutStrategy,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var displayValue = _findDisplayValue();
    var lightTheme = Theme.of(context).brightness == Brightness.light;
    return Padding(
      padding: const EdgeInsets.only(top: 8.0, bottom: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Container(
            height: 30,
            width: 150,
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            decoration: BoxDecoration(
              color: rolloutStrategy == null
                  ? (lightTheme ? defaultValueColor : Colors.transparent)
                  : (lightTheme ? strategyValueColor : Colors.transparent),
              borderRadius: const BorderRadius.all(Radius.circular(16.0)),
              border: lightTheme
                  ? null
                  : Border.all(
                      color: Colors.blue,
                      width: 1.0,
                    ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                if (fv.rolloutStrategies.isNotEmpty)
                  Expanded(
                    flex: 4,
                    child: rolloutStrategy != null
                        ? Tooltip(
                            message: generateTooltipMessage(rolloutStrategy),
                            child: rolloutStrategy == null
                                ? const SizedBox.shrink()
                                : Text(rolloutStrategy!.name,
                                    style: Theme.of(context)
                                        .textTheme
                                        .caption!
                                        .copyWith(color: strategyTextColor)),
                          )
                        : Text('default',
                            style: Theme.of(context)
                                .textTheme
                                .caption!
                                .copyWith(color: defaultTextColor)),
                  ),
                if (fv.rolloutStrategies.isNotEmpty)
                  VerticalDivider(
                      thickness: 1.0, color: lightTheme ? null : Colors.blue),
                Expanded(
                  flex: 4,
                  child: Align(
                    alignment: Alignment.center,
                    child: Padding(
                      padding: const EdgeInsets.only(left: 4.0),
                      child: feature.valueType == FeatureValueType.BOOLEAN
                          ? FlagOnOffColoredIndicator(
                              on: rolloutStrategy != null
                                  ? rolloutStrategy!.value
                                  : fv.valueBoolean)
                          : Text(
                              displayValue.isEmpty ? 'not set' : displayValue,
                              overflow: TextOverflow.ellipsis,
                              maxLines: 1,
                              style: displayValue.isEmpty
                                  ? Theme.of(context).textTheme.caption
                                  : Theme.of(context)
                                      .textTheme
                                      .bodyText2
                                      ?.copyWith(
                                          color: const Color(0xff11C8B5))),
                    ),
                  ),
                )
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _findDisplayValue() {
    if (rolloutStrategy != null) {
      if (rolloutStrategy!.value != null) {
        return rolloutStrategy!.value.toString();
      } else {
        return '';
      }
    } else {
      return _getFeatureValue();
    }
  }

  String _getFeatureValue() {
    switch (feature.valueType!) {
      case FeatureValueType.STRING:
        return fv.valueString ?? '';
      case FeatureValueType.NUMBER:
        return fv.valueNumber?.toString() ?? '';
      case FeatureValueType.BOOLEAN:
        return ''; // shouldn't happen
      case FeatureValueType.JSON:
        return fv.valueJson?.replaceAll('\n', '') ?? '';
    }
  }
}
