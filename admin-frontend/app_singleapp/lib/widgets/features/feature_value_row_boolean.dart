import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

// represents the editing of the states of a single boolean flag on a single environment
// function for dirty callback needs to be added
class FeatureValueBooleanEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  FeatureValueBooleanEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);

  @override
  _FeatureValueBooleanEnvironmentCellState createState() =>
      _FeatureValueBooleanEnvironmentCellState();
}



class _FeatureValueBooleanEnvironmentCellState
    extends State<FeatureValueBooleanEnvironmentCell> {
  String featureOn;

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canWrite = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          if (snap.hasData) {
              return Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    Text('default', style: Theme.of(context).textTheme.caption),
                    SizedBox(width: 4.0,),
                    DropdownButton(
                      items:
                        <String>['On', 'Off']
                            .map<DropdownMenuItem<String>>((String value) {
                          return DropdownMenuItem<String>(
                            value: value,
                            child: Text(value, style: Theme.of(context).textTheme.bodyText2,),
                          );
                        }).toList(),
                        value: featureOn,
                        onChanged: snap.data == false && canWrite ? (value) {

                          widget.fvBloc.dirty(
                              widget.environmentFeatureValue.environmentId,
                              (original) => original.valueBoolean != (value == 'On' ? true : false) ,
                              value == 'On' ? true : false);
                          setState(() {
                            featureOn = value;
                          });
                        } : null,
                      disabledHint: Text(widget.featureValue.locked
                          ? 'Unlock to change'
                          : "You don't have permissions to update this setting", style: Theme.of(context).textTheme.caption),
                        ),
                  ]);

          }
          return SizedBox.shrink();
        });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

      featureOn = widget.featureValue.valueBoolean ? 'On' : 'Off';

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
        Column(
          mainAxisAlignment: MainAxisAlignment.center,
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
          ],
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
