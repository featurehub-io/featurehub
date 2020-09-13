import 'package:app_singleapp/utils/utils.dart';
import 'package:app_singleapp/widgets/common/FHFlatButton.dart';
import 'package:app_singleapp/widgets/common/fh_flat_button_transparent.dart';
import 'package:app_singleapp/widgets/common/fh_footer_button_bar.dart';
import 'package:app_singleapp/widgets/common/fh_json_editor.dart';
import 'package:app_singleapp/widgets/features/custom_strategy_bloc.dart';
import 'package:app_singleapp/widgets/features/per_feature_state_tracking_bloc.dart';
import 'package:app_singleapp/widgets/features/table-expanded-view/json/json_viewer_field.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:mrapi/api.dart';

class EditJsonValueContainer extends StatefulWidget {
  const EditJsonValueContainer({
    Key key,
    @required this.enabled,
    @required this.canEdit,
    @required this.fvBloc,
    @required this.rolloutStrategy,
    @required this.strBloc,
    @required this.environmentFV,
    @required this.featureValue,
  }) : super(key: key);

  final bool enabled;
  final bool canEdit;
  final PerFeatureStateTrackingBloc fvBloc;
  final RolloutStrategy rolloutStrategy;
  final CustomStrategyBloc strBloc;
  final EnvironmentFeatureValues environmentFV;
  final FeatureValue featureValue;


  @override
  _EditJsonValueContainerState createState() => _EditJsonValueContainerState();
}

class _EditJsonValueContainerState extends State<EditJsonValueContainer> {

  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy.value
        : widget.featureValue.valueJson;
    tec.text = (valueSource ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {

    BoxDecoration myBoxDecoration() {
      return BoxDecoration(
        border: Border.all(width: 1.0),
        borderRadius: BorderRadius.all(
            Radius.circular(6.0) //         <--- border radius here
        ),
      );
    }

    return Container(
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
                  text: tec.text, canEdit: widget.canEdit),
            )),
        onTap: () => _viewJsonEditor(context, widget.enabled),
      ),
    );
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

  //TODO: Richard to check if this is correct
  void _valueChanged() {

      var dirty = false;

      final replacementValue = tec.text.isEmpty ? null : tec.text?.trim();
      if (widget.rolloutStrategy != null) {
        widget.rolloutStrategy.value = replacementValue;
        widget.strBloc.markDirty();
        dirty = true;
      } else {
        dirty = widget.fvBloc.dirty(widget.environmentFV.environmentId,
                (current) => current.value = replacementValue);
      }

    if (dirty) {
      setState(() {});
    }
  }

}
