import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
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
          final canEdit = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          final isLocked = snap.hasData && snap.data.locked;
          final enabled = canEdit && !isLocked;
          final val = snap.hasData ? snap.data.valueNumber : null;

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
                        ? 'Enter number value'
                        : 'No editing permissions',
                    hintStyle: Theme.of(context).textTheme.caption,
                    errorText: validateNumber(tec.text) != null
                        ? 'Not a valid number'
                        : null,
                  ),
                  onEditingComplete: () {
                    if (validateNumber(tec.text) == null) {
                      if (tec.text.isEmpty) {
                        snap.data.valueNumber = null;
                      } else {
                        snap.data.valueNumber = double.parse(tec.text);
                      }
                      widget.fvBloc.updatedFeature(
                          widget.environmentFeatureValue.environmentId);
                    }
                  },
                  inputFormatters: [
                    DecimalTextInputFormatter(
                        decimalRange: 5, activatedNegativeValues: true)
                  ],
                )),
          );
        });
  }
}

class DecimalTextInputFormatter extends TextInputFormatter {
  DecimalTextInputFormatter({int decimalRange, bool activatedNegativeValues})
      : assert(decimalRange == null || decimalRange >= 0,
            'DecimalTextInputFormatter declaration error') {
    final dp = (decimalRange != null && decimalRange > 0)
        ? '([.][0-9]{0,$decimalRange}){0,1}'
        : '';
    final num = '[0-9]*$dp';

    if (activatedNegativeValues) {
      _exp = RegExp('^((((-){0,1})|((-){0,1}[0-9]$num))){0,1}\$');
    } else {
      _exp = RegExp('^($num){0,1}\$');
    }
  }

  RegExp _exp;

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    if (_exp.hasMatch(newValue.text)) {
      return newValue;
    }
    return oldValue;
  }
}

class FeatureValueNumberCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueNumberCellEditor(
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
        FeatureValueNumberEnvironmentCell(
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
