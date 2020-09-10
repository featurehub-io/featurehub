import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_json_editor.dart';
import 'package:app_singleapp/widgets/features/feature_value_updated_by.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

import 'feature_value_row_locked.dart';
import 'feature_values_bloc.dart';

class FeatureValueJsonEnvironmentCell extends StatefulWidget {
  final EnvironmentFeatureValues environmentFeatureValue;
  final Feature feature;
  final FeatureValuesBloc fvBloc;
  final FeatureValue featureValue;

  FeatureValueJsonEnvironmentCell(
      {Key key, this.environmentFeatureValue, this.feature, this.fvBloc})
      : featureValue = fvBloc
            .featureValueByEnvironment(environmentFeatureValue.environmentId),
        super(key: key);

  @override
  _FeatureValueJsonEnvironmentCellState createState() =>
      _FeatureValueJsonEnvironmentCellState();
}

class _FeatureValueJsonEnvironmentCellState
    extends State<FeatureValueJsonEnvironmentCell> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    tec.text = widget.featureValue.valueJson ?? '';
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
              width: 123,
              height: 30,
              child: InkWell(
                canRequestFocus: false,
                mouseCursor: SystemMouseCursors.click,
                customBorder: RoundedRectangleBorder(
                  borderRadius: BorderRadius.all(Radius.circular(6.0)),
                ),
                hoverColor: Colors.black12,
                child: Container(
                    padding: const EdgeInsets.all(4.0),
                    decoration: myBoxDecoration(),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: ConfigurationViewerField(
                          text: tec.text, canEdit: canEdit),
                    )),
                onTap: () => _viewJsonEditor(context, enabled),
              ),
            ),
          );
        });
  }

  void _valueChanged() {
    final value = tec.text?.trim();

    final dirty = widget.fvBloc
        .dirty(widget.environmentFeatureValue.environmentId, (current) {
      current.value = value.isEmpty ? null : value;
    });

    //         (originalFv) {
    //   return (value.isEmpty ? null : value) !=
    //       originalFv?.valueJson?.toString();
    // }, FeatureValueDirtyHolder()..value = value.isEmpty ? null : value);

    if (dirty) {
      setState(() {});
    }
  }

  void _viewJsonEditor(BuildContext context, bool enabled) {
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
                              _valueChanged();
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
  final String text;
  final bool canEdit;

  const ConfigurationViewerField({
    Key key,
    @required this.text,
    this.canEdit,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (text != null && text.isNotEmpty) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: <Widget>[
          Flexible(
            flex: 4,
            child: Text(
              text.replaceAll('\n', ''),
              style: TextStyle(fontFamily: 'source', fontSize: 12),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Flexible(flex: 1, child: Icon(Icons.more_horiz))
        ],
      );
    } else if (canEdit) {
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
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
