import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_values_bloc.dart';

class FeatureValueNumberEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueNumberEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  _FeatureValueNumberEnvironmentCellState createState() =>
      _FeatureValueNumberEnvironmentCellState();
}

class _FeatureValueNumberEnvironmentCellState
    extends State<FeatureValueNumberEnvironmentCell> {
  TextEditingController tec = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<FeatureValue>(
        stream: widget.fvBloc.featureValueByEnvironment(
            widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canEdit =
              widget.environmentFeatureValue.roles.contains(RoleType.EDIT);
          final isLocked = snap.hasData && snap.data.locked;
          final enabled = canEdit && !isLocked;
          final val = snap.hasData ? snap.data.valueNumber : null;

          if (val == null) {
            tec.text = '';
          } else if (tec.text != val.toString()) {
            tec.text = val.toString();
          }

          return Expanded(
              child: Padding(
                  padding: const EdgeInsets.only(right: 16.0),
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
                            ? "Enter number value"
                            : "No editing permissions",
                        hintStyle: Theme.of(context).textTheme.caption,
                        errorText: validateNumber(tec.text) != null
                            ? 'Not a valid number'
                            : null,
                      ),
                      onChanged: (value) {
                        if (validateNumber(tec.text) == null) {
                          if (tec.text.isEmpty) {
                            snap.data.valueNumber = null;
                          } else {
                            snap.data.valueNumber = double.parse(tec.text);
                          }
                          widget.fvBloc.updatedFeature(
                              widget.environmentFeatureValue.environmentId);
                        }
                      })));
        });
  }
}

class FeatureValueEditNumber {
  static TableRow build(BuildContext context, LineStatusFeature featureStatuses,
      Feature feature) {
    FeatureValuesBloc fvBloc = BlocProvider.of(context);

    return TableRow(
        children: [
          FeatureEditDeleteCell(
            feature: feature,
          ),
          ...featureStatuses.environmentFeatureValues
              .map((e) => Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      FeatureValueNumberEnvironmentCell(
                          environmentFeatureValue: e,
                          feature: feature,
                          fvBloc: fvBloc),
                    ],
                  ))
              .toList()
        ]);
  }
}
