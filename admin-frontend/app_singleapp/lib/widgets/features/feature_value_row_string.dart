import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

class FeatureValueStringEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueStringEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  _FeatureValueStringEnvironmentCellState createState() =>
      _FeatureValueStringEnvironmentCellState();
}

class _FeatureValueStringEnvironmentCellState
    extends State<FeatureValueStringEnvironmentCell> {
  TextEditingController tec = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: widget.fvBloc.featureValueByEnvironment(
            widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canEdit = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          final isLocked = snap.hasData && snap.data.locked;
          final enabled = canEdit && !isLocked;
          final val = snap.hasData ? snap.data.valueString : null;

          if (val == null) {
            tec.text = '';
          } else if (tec.text != val.toString()) {
            tec.text = val.toString();
          }

          return Align(
            alignment: Alignment.topCenter,
            child: Container(
                width: 160,
                height: 40,
                child: TextField(
                  style: Theme.of(context).textTheme.bodyText1,
                  enabled: enabled,
                  controller: tec,
                  decoration: InputDecoration(
                      contentPadding: EdgeInsets.only(left: 4.0, top: 4.0),
                      enabledBorder: OutlineInputBorder(
                          borderSide: BorderSide(
                        color: Theme.of(context).buttonColor,
                      )),
                      disabledBorder: OutlineInputBorder(
                          borderSide: BorderSide(
                        color: Colors.grey,
                      )),
                      hintText: canEdit
                          ? 'Enter string value'
                          : 'No editing permissions',
                      hintStyle: Theme.of(context).textTheme.caption),
                  onChanged: (value) {
                    widget.fvBloc.dirty(
                      widget.environmentFeatureValue.environmentId,
                      (originalFv) =>
                          (value?.isEmpty ? null : value) !=
                          originalFv?.valueString,
                    );
                  },
                  onEditingComplete: () {
                    _handleChanged(tec.text, snap.data);
                  },
                )),
          );
        });
  }

  void _handleChanged(String val, FeatureValue fv) {
    fv.valueString = val?.trim();
    if (fv.valueString.isEmpty) {
      fv.valueString = null;
    }
    widget.fvBloc.updatedFeature(widget.environmentFeatureValue.environmentId);
  }
}

class FeatureValueStringCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueStringCellEditor(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.start,
      children: [
        FeatureValueEditLockedCell(
          environmentFeatureValue: environmentFeatureValue,
          feature: feature,
          fvBloc: fvBloc,
        ),
        FeatureValueStringEnvironmentCell(
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
