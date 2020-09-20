import 'package:app_singleapp/widgets/features/feature_dashboard_constants.dart';
import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:app_singleapp/widgets/features/table-collapsed-view/flag_colored_on_off_label.dart';
import 'package:app_singleapp/widgets/features/table-collapsed-view/value_not_set_container.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

class CollapsedViewValueCellHolder extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const CollapsedViewValueCellHolder(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      return _ValueContainer(feature: feature, fv: fv);
    }
    if ((fv == null || fv.id == null) && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class _ValueContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  _ValueContainer({@required this.feature, @required this.fv});

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: AlignmentDirectional.center,
      children: [
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            if (fv != null) _ValueCard(feature: feature, fv: fv),
            if (fv == null) NotSetContainer(),
            if (fv != null && fv.rolloutStrategies != null)
              _StrategiesList(feature: feature, fv: fv)
          ],
        ),
        if (fv != null && fv.locked) LockedIndicator()
      ],
    );
  }
}

class LockedIndicator extends StatelessWidget {
  const LockedIndicator({
    Key key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: cellWidth - 1,
      child: Align(
        alignment: Alignment.topLeft,
        child: Container(
          padding: EdgeInsets.all(8.0),
//          color: Colors.black.withOpacity(0.1),
          child: Icon(Icons.lock_outline, size: 16.0, color: Colors.black45),
        ),
      ),
    );
  }
}

class _StrategiesList extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const _StrategiesList({Key key, @required this.feature, @required this.fv})
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
  final RolloutStrategy rolloutStrategy;
  const _ValueCard({
    Key key,
    @required this.fv,
    @required this.feature,
    this.rolloutStrategy,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var displayValue = _findDisplayValue(rolloutStrategy);
    return Padding(
      padding: const EdgeInsets.only(top: 4.0, bottom: 4.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          Card(
            child: Container(
              height: 30,
              width: 200,
              padding: EdgeInsets.symmetric(horizontal: 8.0),
              decoration: BoxDecoration(
                color: rolloutStrategy == null
                    ? defaultValueColor
                    : strategyValueColor,
//                borderRadius: BorderRadius.all(Radius.circular(16.0)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  if (fv.rolloutStrategies != null &&
                      fv.rolloutStrategies.isNotEmpty)
                    Expanded(
                        flex: 4,
                        child: Text(
                            rolloutStrategy != null
                                ? '${(rolloutStrategy.percentage / 100).toString()}%'
                                : 'default',
                            style: Theme.of(context).textTheme.caption.copyWith(
                                color: rolloutStrategy != null
                                    ? strategyTextColor
                                    : defaultTextColor))),
                  if (fv.rolloutStrategies != null &&
                      fv.rolloutStrategies.isNotEmpty)
                    VerticalDivider(
                      thickness: 1.0,
                    ),
                  Expanded(
                    flex: 4,
                    child: Align(
                      alignment: Alignment.center,
                      child: Padding(
                        padding: const EdgeInsets.only(left: 4.0),
                        child: feature.valueType == FeatureValueType.BOOLEAN
                            ? FlagOnOffColoredIndicator(
                                on: rolloutStrategy != null
                                    ? rolloutStrategy.value
                                    : fv.valueBoolean)
                            : Text(
                               displayValue.isEmpty ? 'not set' : displayValue,
                                overflow: TextOverflow.ellipsis,
                                maxLines: 1,
                                style: displayValue.isEmpty
                                    ? Theme.of(context).textTheme.caption
                                    : Theme.of(context).textTheme.bodyText2),
                      ),
                    ),
                  )
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _findDisplayValue(RolloutStrategy rolloutStrategy) {
    if (rolloutStrategy != null) {
      if (rolloutStrategy.value != null) {
        return rolloutStrategy.value.toString();
      } else {
        return '';
      }
    } else {
      return _getFeatureValue();
    }
  }

  String _getFeatureValue() {
    switch (feature.valueType) {
      case FeatureValueType.STRING:
        return fv.valueString ?? '';
      case FeatureValueType.NUMBER:
        return fv.valueNumber?.toString() ?? '';
      case FeatureValueType.BOOLEAN:
        return ''; // shouldn't happen
      case FeatureValueType.JSON:
        return fv.valueJson?.replaceAll('\n', '') ?? '';
    }

    return '';
  }
}

//      if (fv.locked)
//        Padding(
//          padding: const EdgeInsets.only(top: 4.0),
//          child: Icon(Icons.lock_outline, size: 16, color: Colors.black26,),
//        ),
