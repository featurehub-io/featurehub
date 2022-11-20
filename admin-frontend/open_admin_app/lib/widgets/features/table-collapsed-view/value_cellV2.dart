import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_icons/flutter_icons.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/features/feature_dashboard_constants.dart';
import 'package:open_admin_app/widgets/features/feature_value_status_tags.dart';
import 'package:open_admin_app/widgets/features/per_application_features_bloc.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/flag_colored_on_off_label.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/tooltip.dart';
import 'package:open_admin_app/widgets/features/table-collapsed-view/value_not_set_container.dart';
import 'package:side_sheet/side_sheet.dart';

import '../edit-values/edit_feature_value_widget.dart';

class ValueCellHolder extends StatelessWidget {
  final FeatureValue? fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;
  final ApplicationFeatureValues afv;

  const ValueCellHolder(
      {Key? key, this.fv, required this.efv, required this.feature, required this.afv})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles.isNotEmpty) {
      return _ValueContainer(feature: feature, fv: fv, efv: efv, afv: afv,);
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
  final EnvironmentFeatureValues efv;
  final ApplicationFeatureValues  afv;

  const _ValueContainer({required this.feature, this.fv, required this.efv, required this.afv});

  @override
  Widget build(BuildContext context) {

    final bloc = BlocProvider.of<PerApplicationFeaturesBloc>(context);


    // onPressed: () => {
    //   fvBloc.mrClient.addOverlay((BuildContext context) {
    //     return BlocProvider(
    //       creator: (_c, _b) => IndividualStrategyBloc(
    //           strBloc.environmentFeatureValue, rolloutStrategy),
    //       child:
    //       StrategyEditingWidget(bloc: strBloc, editable: editable),
    //     );
    //   })
    // });

    return InkWell(
        onTap: () =>
            SideSheet.right(
                body: EditFeatureValueWidget(environmentFeatureValue: efv,  featureValueType: feature.valueType!, perApplicationFeaturesBloc: bloc, feature: feature, afv: afv,),
                context: context
            ),
      child: Column(
        // crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          if (fv != null && (fv?.retired != null && fv?.retired == true))
            const RetiredIndicator(),
          if (fv != null && fv!.locked) const LockedIndicator(),
          if (fv != null) _ValueCard(feature: feature, fv: fv!),
          if (fv == null) const NotSetContainer(),
          if (fv != null && fv!.rolloutStrategies.isNotEmpty)
            _StrategiesList(feature: feature, fv: fv!),

        ],
      ),
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
          child: const Tooltip(
              message: "Locked",
              child: Icon(Icons.lock_outline, size: 16.0, color: Colors.orange)),
        ),
      ),
    );
  }
}

class RetiredIndicator extends StatelessWidget {
  const RetiredIndicator({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: cellWidth - 1,
      child: Align(
        alignment: Alignment.topLeft,
        child: Container(
          padding: const EdgeInsets.all(8.0),
          child: const Tooltip(
              message: "Retired",
              child: Icon(MaterialIcons.do_not_disturb,
                  size: 16.0, color: Colors.red)),
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
      // crossAxisAlignment: CrossAxisAlignment.start,
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
      child: Container(
        constraints: BoxConstraints(maxWidth: 150, minWidth: 80),
        height: 30,
        padding: const EdgeInsets.symmetric(horizontal: 8.0),
        decoration: BoxDecoration(
          color: lightTheme ? Colors.white70 : Colors.black12,
          borderRadius: const BorderRadius.all(Radius.circular(4.0)),
          border: Border.all(
                  color: Colors.black12,
                  width: 1.0,
                ),
        ),
        child: Row(
          // mainAxisAlignment: MainAxisAlignment.spaceAround,
          mainAxisSize: MainAxisSize.min,
          children: [
            feature.valueType == FeatureValueType.BOOLEAN
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
              SizedBox(width: 4.0),
              Container(
                decoration: BoxDecoration(
                  color: rolloutStrategy == null
                      ? defaultTextColor.withOpacity(0.3)
                      : strategyTextColor.withOpacity(0.3),
                  borderRadius: const BorderRadius.all(Radius.circular(4.0)),
                ),
                child: rolloutStrategy != null
                    ? Tooltip(
                        message: generateTooltipMessage(rolloutStrategy),
                        child: rolloutStrategy == null
                            ? const SizedBox.shrink()
                            : Padding(
                              padding: const EdgeInsets.all(4.0),
                              child: Text(rolloutStrategy!.name,
                                  style: Theme.of(context)
                                      .textTheme
                                      .overline!
                                      .copyWith(color: strategyTextColor)),
                            ),
                      )
                    : Padding(
                      padding: const EdgeInsets.all(4.0),
                      child: Text('default',
                          style: Theme.of(context)
                              .textTheme
                              .labelSmall!
                              .copyWith(color: defaultTextColor)),
                    ),
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
