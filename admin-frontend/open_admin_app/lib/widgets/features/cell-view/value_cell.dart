import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';

import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_tag.dart';
import 'package:open_admin_app/widgets/features/cell-view/flag_colored_on_off_label.dart';
import 'package:open_admin_app/widgets/features/cell-view/strategy_tooltip.dart';
import 'package:open_admin_app/widgets/features/cell-view/value_not_set_container.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/edit_feature_value_widget.dart';
import 'package:open_admin_app/widgets/features/editing_feature_value_block.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:side_sheet/side_sheet.dart';

class ValueCellHolder extends StatelessWidget {
  final FeatureValue? fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;
  final ApplicationFeatureValues afv;

  const ValueCellHolder(
      {Key? key,
      this.fv,
      required this.efv,
      required this.feature,
      required this.afv})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      return _ValueContainer(feature: feature, fv: fv, efv: efv, afv: afv);
    }
    if ((fv?.id == null) && efv.roles.isEmpty) {
      return const FHTagWidget(text: 'NO ACCESS', state: TagStatus.disabled);
    }
    return const SizedBox.shrink();
  }
}

class _ValueContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue? fv;
  final EnvironmentFeatureValues efv;
  final ApplicationFeatureValues afv;

  const _ValueContainer(
      {required this.feature,
      required this.fv,
      required this.efv,
      required this.afv});

  @override
  Widget build(BuildContext context) {
    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);
    if (fv != null) {
      return InkWell(
        onTap: () {
          SideSheet.right(
              sheetColor: Theme.of(context).canvasColor,
              body: BlocProvider<EditingFeatureValueBloc>.builder(
                  creator: (c, b) =>
                      bloc.perFeatureStateTrackingBloc(feature, fv!, efv),
                  builder: (ctx, efvBloc) => EditFeatureValueWidget(
                        bloc: efvBloc,
                      )),
              width: MediaQuery.of(context).size.width > 800
                  ? MediaQuery.of(context).size.width * 0.5
                  : MediaQuery.of(context).size.width,
              context: context);
        },
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Align(
                alignment: Alignment.topRight,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (fv!.retired && fv!.retired == true)
                      const RetiredIndicator(),
                    if (fv!.locked) const LockedIndicator(),
                    if (!fv!.locked && !fv!.retired)
                      const SizedBox(height: 14.0)
                  ],
                ),
              ),
              _ValueCard(feature: feature, fv: fv!),
              if (fv!.rolloutStrategies?.isNotEmpty == true)
                _StrategiesList(feature: feature, fv: fv!),
              if (fv!.featureGroupStrategies != null &&
                  fv!.featureGroupStrategies!.isNotEmpty)
                _FeatureGroupStrategyList(feature: feature, fv: fv!)
            ],
          ),
        ),
      );
    } else {
      return const NotSetContainer();
    }
  }
}

class LockedIndicator extends StatelessWidget {
  const LockedIndicator({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var lightTheme = Theme.of(context).brightness == Brightness.light;
    return Tooltip(
        message: "Locked",
        child: Icon(Icons.lock_outline,
            size: 14.0, color: lightTheme ? Colors.black54 : Colors.white70));
  }
}

class RetiredIndicator extends StatelessWidget {
  const RetiredIndicator({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var lightTheme = Theme.of(context).brightness == Brightness.light;
    return Container(
      padding: const EdgeInsets.only(right: 8.0),
      child: Tooltip(
        message: "Retired",
        child: Icon(
          Icons.do_not_disturb,
          size: 14.0,
          color: lightTheme ? Colors.black54 : Colors.white70,
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
    final rolloutStrategies = fv.rolloutStrategies;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (rolloutStrategies != null)
          for (RolloutStrategy rsi in rolloutStrategies)
            _ValueCard(rolloutStrategy: rsi, fv: fv, feature: feature)
      ],
    );
  }
}

class _FeatureGroupStrategyList extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const _FeatureGroupStrategyList(
      {Key? key, required this.feature, required this.fv})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (fv.featureGroupStrategies != null)
          for (ThinGroupRolloutStrategy rsi in fv.featureGroupStrategies!)
            _ValueCard(
              rolloutStrategy: rsi,
              fv: fv,
              feature: feature,
              isGroupStrategy: true,
            )
      ],
    );
  }
}

class _ValueCard extends StatelessWidget {
  final FeatureValue fv;
  final Feature feature;
  final dynamic rolloutStrategy;
  final bool? isGroupStrategy;

  const _ValueCard({
    Key? key,
    required this.fv,
    required this.feature,
    this.rolloutStrategy,
    this.isGroupStrategy = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    var displayValue = _findDisplayValue();
    var lightTheme = Theme.of(context).brightness == Brightness.light;
    return Padding(
      padding: const EdgeInsets.only(top: strategyCardPadding),
      child: Container(
        height: strategyCardHeight,
        decoration: BoxDecoration(
          color: lightTheme ? Colors.white70 : Colors.black12,
          borderRadius: const BorderRadius.all(Radius.circular(4.0)),
          border: Border.all(
            color: Colors.black12,
            width: 0.5,
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.max,
          children: [
            Flexible(
              fit: FlexFit.tight,
              child: Container(
                decoration: BoxDecoration(
                  color: rolloutStrategy == null
                      ? defaultTextColor.withOpacity(0.15)
                      : (isGroupStrategy!
                          ? groupStrategyTextColor.withOpacity(0.15)
                          : strategyTextColor.withOpacity(0.15)),
                  borderRadius: const BorderRadius.only(
                      topLeft: Radius.circular(4.0),
                      bottomLeft: Radius.circular(4.0)),
                ),
                child: rolloutStrategy != null
                    ? Tooltip(
                        richMessage: TextSpan(children: [
                          if (!isGroupStrategy!)
                            TextSpan(text: rolloutStrategy!.name),
                          TextSpan(
                              text: isGroupStrategy!
                                  ? "Group Strategy"
                                  : generateTooltipMessage(rolloutStrategy))
                        ]),
                        child: rolloutStrategy == null
                            ? const SizedBox.shrink()
                            : Padding(
                                padding: const EdgeInsets.all(4.0),
                                child: Text(
                                  rolloutStrategy!.name,
                                  overflow: TextOverflow.ellipsis,
                                  maxLines: 1,
                                  style: Theme.of(context)
                                      .textTheme
                                      .labelLarge!
                                      .copyWith(
                                          color: isGroupStrategy!
                                              ? groupStrategyTextColor
                                              : strategyTextColor,
                                          // fontWeight: FontWeight.bold,
                                          letterSpacing: 0.9),
                                ),
                              ),
                      )
                    : Padding(
                        padding: const EdgeInsets.all(4.0),
                        child: isGroupStrategy!
                            ? Text(rolloutStrategy!.name,
                                style: Theme.of(context)
                                    .textTheme
                                    .labelLarge!
                                    .copyWith(color: defaultTextColor))
                            : Text('default',
                                style: Theme.of(context)
                                    .textTheme
                                    .labelLarge!
                                    .copyWith(color: defaultTextColor)),
                      ),
              ),
            ),
            const SizedBox(width: 4.0),
            feature.valueType == FeatureValueType.BOOLEAN
                ? Flexible(
                    fit: FlexFit.tight,
                    child: FlagOnOffColoredIndicator(
                        on: rolloutStrategy != null
                            ? rolloutStrategy!.value
                            : fv.valueBoolean),
                  )
                : Flexible(
                    fit: FlexFit.tight,
                    child: Text(displayValue.isEmpty ? 'not set' : displayValue,
                        overflow: TextOverflow.ellipsis,
                        maxLines: 1,
                        style: displayValue.isEmpty
                            ? Theme.of(context).textTheme.bodyMedium
                            : Theme.of(context)
                                .textTheme
                                .bodyMedium
                                ?.copyWith(color: const Color(0xff11C8B5))),
                  ),
          ],
        ),
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
  }
}
