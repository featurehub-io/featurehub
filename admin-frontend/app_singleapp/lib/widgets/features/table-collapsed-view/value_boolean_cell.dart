import 'package:app_singleapp/widgets/features/feature_value_status_tags.dart';
import 'package:app_singleapp/widgets/features/table-collapsed-view/value_not_set_container.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mrapi/api.dart';

class BooleanCell extends StatelessWidget {
  final FeatureValue fv;
  final EnvironmentFeatureValues efv;
  final Feature feature;

  const BooleanCell(
      {Key key, @required this.fv, @required this.efv, @required this.feature})
      : assert(efv != null),
        assert(feature != null),
        super(key: key);

  @override
  Widget build(BuildContext context) {
    if (efv.roles != null && efv.roles.isNotEmpty) {
      if (fv != null && fv.isSet(feature)) {
        return BooleanContainer(feature, fv);
      } else {
        return NotSetContainer();
      }
    }
    if (fv == null && efv.roles.isEmpty) {
      return noAccessTag(null);
    }
    return SizedBox.shrink();
  }
}

class BooleanContainer extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  BooleanContainer(this.feature, this.fv);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 170,
      child: Stack(children: [
//        if (fv.locked)
//          Center(
//            child: Opacity(
//              opacity: 0.40,
//              child: Icon(
//                Icons.lock_outline,
//                color: Colors.black12,
//                size: 80.0,
//              ),
//            ),
//          ),
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            _FlagValueContainer(name: 'default', value: fv.valueBoolean),
            if (fv.rolloutStrategies != null)
              StrategiesList(feature: feature, fv: fv)
          ],
        ),
      ]),
    );
  }
}

class StrategiesList extends StatelessWidget {
  final Feature feature;
  final FeatureValue fv;

  const StrategiesList({Key key, @required this.feature, @required this.fv})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (RolloutStrategy rsi in fv.rolloutStrategies)
          _FlagValueContainer(
            name: rsi.name,
            value: rsi.value,
            percentage: rsi.percentage,
          )
      ],
    );
  }
}

class _FlagValueContainer extends StatelessWidget {
  final String name;
  final bool value;
  final int percentage;

  const _FlagValueContainer({Key key, this.name, this.value, this.percentage})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 4.0, bottom: 4.0),
      child: Container(
        height: 25,
        padding: EdgeInsets.symmetric(horizontal: 8.0),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.lightBlue),
          borderRadius: BorderRadius.all(Radius.circular(16.0)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(name, style: Theme.of(context).textTheme.caption),
            if (percentage != null)
              Text('${(percentage / 100).toString()}%',
                  style: Theme.of(context).textTheme.overline),
            FlagOnOffColoredIndicator(on: value)
          ],
        ),
      ),
    );
  }
}

class FlagOnOffColoredIndicator extends StatelessWidget {
  final bool on;

  const FlagOnOffColoredIndicator({Key key, this.on}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return on
        ? Text('ON',
            style: GoogleFonts.openSans(
                textStyle: Theme.of(context).textTheme.button.copyWith(
                    color: Color(0xff11C8B5), fontWeight: FontWeight.bold)))
        : Text('OFF',
            style: GoogleFonts.openSans(
                textStyle: Theme.of(context).textTheme.button.copyWith(
                    color: Color(0xffF44C49), fontWeight: FontWeight.bold)));
  }
}
