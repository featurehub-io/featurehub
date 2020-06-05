import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_json_editor.dart';
import 'package:app_singleapp/widgets/common/fh_outline_button.dart';
import 'package:app_singleapp/widgets/features/feature_status_bloc.dart';
import 'package:app_singleapp/widgets/features/feature_value_row_generic.dart';
import 'package:bloc_provider/bloc_provider.dart';
import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';

import 'feature_values_bloc.dart';

class FeatureValueEditJson {
  static TableRow build(BuildContext context, LineStatusFeature featureStatuses,
      Feature feature) {
    FeatureValuesBloc fvBloc = BlocProvider.of(context);
    BorderSide bs = BorderSide(
        color: Theme.of(context).buttonColor,
        width: 2.0,
        style: BorderStyle.solid);

    return TableRow(
        decoration: BoxDecoration(
          border: Border(left: bs, right: bs),
        ),
        children: [
          FeatureEditDeleteCell(
            feature: feature,
          ),
          ...featureStatuses.environmentFeatureValues
              .map((e) => Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: <Widget>[
                      FeatureValueJsonEnvironmentCell(
                          environmentFeatureValue: e,
                          feature: feature,
                          fvBloc: fvBloc),
                    ],
                  ))
              .toList()
        ]);
  }
}

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
          final canEdit =
              widget.environmentFeatureValue.roles.contains(RoleType.EDIT);
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
              border: Border.all(width: 0.5),
              borderRadius: BorderRadius.all(
                  Radius.circular(6.0) //         <--- border radius here
                  ),
            );
          }

          return Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 8.0, right: 36),
              child: InkWell(
                customBorder: RoundedRectangleBorder(
                  borderRadius: BorderRadius.all(
                      Radius.circular(6.0) //         <--- border radius here
                      ),
                ),
                hoverColor: Colors.black12,
                child: Container(
                    margin: const EdgeInsets.all(4.0),
                    padding: const EdgeInsets.all(10.0),
                    decoration: myBoxDecoration(),
                    child: ConfigurationViewerField(
                        fv: snap.data, canEdit: canEdit)),
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
    String initialValue = tec.text;
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
                      title: 'Cancel'),
                  enabled
                      ? FHOutlineButton(
                          title: "set value",
                          onPressed: (() {
                            if (validateJson(tec.text) != null) {
                              widget.fvBloc.mrClient.customError(
                                  messageTitle: "JSON not valid!",
                                  messageBody:
                                      "Make sure your keys and values are in double quotes.");
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
              fv.valueJson.replaceAll("\n", ""),
              style: TextStyle(fontFamily: "source", fontSize: 12),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Flexible(flex: 1, child: Icon(Icons.more_horiz))
        ],
      );
    } else if ((fv == null || fv.valueJson == null) && canEdit) {
      return Text(
        "Edit value",
        style: Theme.of(context).textTheme.caption,
      );
    }
    return Text("No editing permissions", style: Theme.of(context).textTheme.caption);
  }
}
