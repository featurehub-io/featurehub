import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

class FeatureValueStringEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  FeatureValueStringEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);

  @override
  _FeatureValueStringEnvironmentCellState createState() =>
      _FeatureValueStringEnvironmentCellState();
}

class _FeatureValueStringEnvironmentCellState
    extends State<FeatureValueStringEnvironmentCell> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    tec.text = widget.featureValue.valueString ?? '';
  }

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<bool>(
        stream: widget.fvBloc
            .environmentIsLocked(widget.environmentFeatureValue.environmentId),
        builder: (ctx, snap) {
          final canEdit = widget.environmentFeatureValue.roles
              .contains(RoleType.CHANGE_VALUE);
          final isLocked = snap.hasData && snap.data;
          final enabled = canEdit && !isLocked;

          return Align(
            alignment: Alignment.topCenter,
            child: Container(
                width: 123,
                height: 30,
                child: TextField(
                  style: Theme.of(context).textTheme.bodyText1,
                  enabled: enabled,
                  controller: tec,
                  decoration: InputDecoration(
                      contentPadding: EdgeInsets.only(left: 4.0, right: 4.0),
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
                    widget.fvBloc
                        .dirty(widget.environmentFeatureValue.environmentId,
                            (current) {
                      current.value = value.isEmpty ? null : tec.text?.trim();
                    });
                  },
                )),
          );
        });
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
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
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(left: 4.0),
          child: FeatureValueUpdatedByCell(
            environmentFeatureValue: environmentFeatureValue,
            feature: feature,
            fvBloc: fvBloc,
          ),
        ),
      ],
    );
  }
}
