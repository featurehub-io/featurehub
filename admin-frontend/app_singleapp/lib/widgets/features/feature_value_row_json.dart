import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_json_editor.dart';
import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

class FeatureValueJsonEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueJsonEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : super(key: key);

  @override
  _FeatureValueJsonEnvironmentCellState createState() =>
      _FeatureValueJsonEnvironmentCellState();
}

class _FeatureValueJsonEnvironmentCellState
    extends State<FeatureValueJsonEnvironmentCell> {
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
          final val = snap.hasData ? snap.data.valueJson : null;

          if (val == null) {
            tec.text = '';
          } else if (tec.text != val.toString()) {
            tec.text = val.toString();
          }
          BoxDecoration myBoxDecoration() {
            return BoxDecoration(
              border: Border.all(width: 1.0),
              borderRadius: BorderRadius.all(
                  Radius.circular(6.0) //         <--- border radius here
                  ),
            );
          }

          return Align(
            alignment: Alignment.topCenter,
            child: Container(
              width: 160,
              height: 40,
              child: InkWell(
                customBorder: RoundedRectangleBorder(
                  borderRadius: BorderRadius.all(
                      Radius.circular(6.0) //         <--- border radius here
                      ),
                ),
                hoverColor: Colors.black12,
                child: Container(
                    padding: const EdgeInsets.all(10.0),
                    decoration: myBoxDecoration(),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: ConfigurationViewerField(
                          fv: snap.data, canEdit: canEdit),
                    )),
                onTap: () => _viewJsonEditor(context, snap, enabled),
              ),
            ),
          );
        });
  }

  void _valueChanged(AsyncSnapshot<FeatureValue> snap) {
    snap.data.valueJson = tec.text?.trim();
    if (snap.data.valueJson.isEmpty) {
      snap.data.valueJson = null;
    }

    widget.fvBloc.updatedFeature(widget.environmentFeatureValue.environmentId);
  }

  void _viewJsonEditor(
      BuildContext context, AsyncSnapshot<FeatureValue> snap, bool enabled) {
    var initialValue = tec.text;
    widget.fvBloc.mrClient.addOverlay((BuildContext context) => AlertDialog(
            content: Container(
          height: 575,
          child: Column(
            children: <Widget>[
              FHJsonEditorWidget(
                controller: tec,
              ),
              FHButtonBar(
                children: <Widget>[
                  FHFlatButtonTransparent(
                    onPressed: () {
                      tec.text = initialValue;
                      widget.fvBloc.mrClient.removeOverlay();
                    },
                    title: 'Cancel',
                    keepCase: true,
                  ),
                  enabled
                      ? FHFlatButton(
                          title: 'set value',
                          onPressed: (() {
                            if (validateJson(tec.text) != null) {
                              widget.fvBloc.mrClient.customError(
                                  messageTitle: 'JSON not valid!',
                                  messageBody:
                                      'Make sure your keys and values are in double quotes.');
                            } else {
                              _valueChanged(snap);
                              widget.fvBloc.mrClient.removeOverlay();
                            }
                          }))
                      : Container(),
                ],
              ),
            ],
          ),
        )));
  }
}

class ConfigurationViewerField extends StatelessWidget {
  final FeatureValue fv;
  final bool canEdit;

  const ConfigurationViewerField({
    Key key,
    @required this.fv,
    this.canEdit,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (fv != null && fv.valueJson != null) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Flexible(
            flex: 4,
            child: Text(
              fv.valueJson.replaceAll('\n', ''),
              style: TextStyle(fontFamily: 'source', fontSize: 12),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Flexible(flex: 1, child: Icon(Icons.more_horiz))
        ],
      );
    } else if ((fv == null || fv.valueJson == null) && canEdit) {
      return Text(
        'Edit value',
        style: Theme.of(context).textTheme.caption,
      );
    }
    return Text('No editing permissions',
        style: Theme.of(context).textTheme.caption);
  }
}

class FeatureValueJsonCellEditor extends StatelessWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;

  const FeatureValueJsonCellEditor(
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
        FeatureValueJsonEnvironmentCell(
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
