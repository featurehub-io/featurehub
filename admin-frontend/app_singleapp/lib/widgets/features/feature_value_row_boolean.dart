import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
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
          final canWrite =
              environmentFeatureValue.roles.contains(RoleType.CHANGE_VALUE);
          if (snap.hasData) {
            if (snap.data.locked == false && canWrite) {
              return Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Switch(
                        //Color(0xff11C8B5) : Color(0xffF44C49)
                        activeTrackColor: Color(0xff11C8B5),
                        activeColor: Colors.white,
                        value: snap.data.valueBoolean ?? false,
                        inactiveTrackColor: Color(0xffF44C49),
                        onChanged: (value) {
                          snap.data.valueBoolean = !snap.data.valueBoolean;
                          _dirtyCheck();
                        }),
                  ]);
            }

            return Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Switch(
                      //Color(0xff11C8B5) : Color(0xffF44C49)
                      activeTrackColor: Color(0xff11C8B5),
                      activeColor: Colors.white,
                      value: snap.data.valueBoolean ?? false,
                      inactiveTrackColor: Colors.black12,
                      onChanged: null),
                ]);
          }

          return Container();
        });
  }

  void _dirtyCheck() {
    fvBloc.updatedFeature(environmentFeatureValue.environmentId);
  }
}

class FeatureValueBooleanCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueBooleanCellEditor(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        FeatureValueEditLockedCell(
          environmentFeatureValue: environmentFeatureValue,
          feature: feature,
          fvBloc: fvBloc,
        ),
        FeatureValueBooleanEnvironmentCell(
          environmentFeatureValue: environmentFeatureValue,
          feature: feature,
          fvBloc: fvBloc,
        ),
        FeatureValueUpdatedByCell(
          environmentFeatureValue: environmentFeatureValue,
          feature: feature,
          fvBloc: fvBloc,
        ),
      ],
    );
  }
}
