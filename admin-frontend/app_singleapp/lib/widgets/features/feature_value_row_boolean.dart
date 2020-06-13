import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_values_bloc.dart';

// represents the editing of the states of a single boolean flag on a single environment
// function for dirty callback needs to be added
class FeatureValueBooleanEnvironmentCell extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueBooleanEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final cannotLock =
              (!environmentFeatureValue.roles.contains(RoleType.UNLOCK) ||
                  !environmentFeatureValue.roles.contains(RoleType.LOCK));

          final cannotWrite =
              (!environmentFeatureValue.roles.contains(RoleType.CHANGE_VALUE));
          if (snap.hasData) {
            if (snap.data.locked == false &&
                snap.data.valueBoolean == null &&
                !cannotLock) {
              return Row(children: <Widget>[
                Switch(
                    //Color(0xff11C8B5) : Color(0xffF44C49)
                    activeTrackColor: Color(0xff11C8B5),
                    activeColor: Colors.white,
                    value: false,
                    inactiveTrackColor: Colors.black12,
                    onChanged: null),
              ]);
            } else if ((snap.data.locked == true &&
                    snap.data.valueBoolean == null) ||
                cannotLock ||
                cannotWrite) {
              return Row(children: <Widget>[
                Switch(
                    //Color(0xff11C8B5) : Color(0xffF44C49)
                    activeTrackColor: Color(0xff11C8B5),
                    activeColor: Colors.white,
                    value: false,
                    inactiveTrackColor: Colors.black12,
                    onChanged: null)
              ]);
            } else {
              return Row(children: <Widget>[
                Switch(
                    //Color(0xff11C8B5) : Color(0xffF44C49)
                    activeTrackColor: Color(0xff11C8B5),
                    activeColor: Colors.white,
                    value: snap.data.valueBoolean,
                    inactiveTrackColor: Color(0xffF44C49),
                    onChanged: (value) {
                      snap.data.valueBoolean = !snap.data.valueBoolean;
                      _dirtyCheck();
                    }),
              ]);
            }
          }

          return Container();
        });
  }

  void _dirtyCheck() {
    fvBloc.updatedFeature(environmentFeatureValue.environmentId);
  }
}

class FeatureValueEditBoolean {
  static TableRow build(BuildContext context, LineStatusFeature featureStatuses,
      Feature feature) {
    final fvBloc = BlocProvider.of<FeatureValuesBloc>(context);
    return TableRow(children: [
      FeatureEditDeleteCell(
        feature: feature,
      ),
      ...featureStatuses.environmentFeatureValues
          .map((e) => Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: <Widget>[
                  FeatureValueBooleanEnvironmentCell(
                      environmentFeatureValue: e,
                      feature: feature,
                      fvBloc: fvBloc),
                ],
              ))
          .toList()
    ]);
  }
}
