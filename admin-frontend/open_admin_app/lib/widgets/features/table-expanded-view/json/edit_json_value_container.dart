import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/utils/utils.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_footer_button_bar.dart';
import 'package:open_admin_app/widgets/common/fh_json_editor.dart';
import 'package:open_admin_app/widgets/features/custom_strategy_bloc.dart';
import 'package:open_admin_app/widgets/features/table-expanded-view/json/json_viewer_field.dart';

class EditJsonValueContainer extends StatefulWidget {
  const EditJsonValueContainer({
    Key? key,
    required this.unlocked,
    required this.canEdit,
    this.rolloutStrategy,
    required this.strBloc,
  }) : super(key: key);

  final bool unlocked;
  final bool canEdit;
  final RolloutStrategy? rolloutStrategy;
  final CustomStrategyBloc strBloc;

  @override
  _EditJsonValueContainerState createState() => _EditJsonValueContainerState();
}

class _EditJsonValueContainerState extends State<EditJsonValueContainer> {
  TextEditingController tec = TextEditingController();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.rolloutStrategy != null
        ? widget.rolloutStrategy!.value
        : widget.strBloc.featureValue.valueJson;
    tec.text = (const JsonEncoder.withIndent('  ').convert(json.decode(valueSource)) ?? '').toString();
  }

  @override
  Widget build(BuildContext context) {
    BoxDecoration myBoxDecoration() {
      return BoxDecoration(
        border: Border.all(
          width: 1.0,
          color: Theme.of(context).colorScheme.primary,
        ),
        borderRadius: const BorderRadius.all(
          Radius.circular(6.0), //         <--- border radius here
        ),
      );
    }

    return SizedBox(
      width: 123,
      height: 30,
      child: InkWell(
        canRequestFocus: false,
        mouseCursor: SystemMouseCursors.click,
        customBorder: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
        ),
        hoverColor: Colors.black12,
        onTap: () => _viewJsonEditor(context, widget.canEdit),
        child: Container(
            padding: const EdgeInsets.all(4.0),
            decoration: myBoxDecoration(),
            child: Align(
              alignment: Alignment.centerLeft,
              child: ConfigurationViewerField(
                  text: tec.text,
                  canEdit: widget.canEdit,
                  unlocked: widget.unlocked),
            )),
      ),
    );
  }

  void _viewJsonEditor(BuildContext context, bool enabled) {
    var initialValue = tec.text;
    widget.strBloc.fvBloc.mrClient
        .addOverlay((BuildContext context) => AlertDialog(
                content: SizedBox(
              height: 575,
              child: Column(
                children: <Widget>[
                  Expanded(
                    child: FHJsonEditorWidget(
                      controller: tec,
                    ),
                  ),
                  FHButtonBar(
                    children: <Widget>[
                      FHFlatButtonTransparent(
                        onPressed: () {
                          tec.text = initialValue;
                          widget.strBloc.fvBloc.mrClient.removeOverlay();
                        },
                        title: 'Cancel',
                        keepCase: true,
                      ),
                      enabled
                          ? FHFlatButton(
                              title: 'Set value',
                              onPressed: (() {
                                if (validateJson(tec.text) != null) {
                                  widget.strBloc.fvBloc.mrClient.customError(
                                      messageTitle: 'JSON not valid!',
                                      messageBody:
                                          'Make sure your keys and values are in double quotes.');
                                } else {
                                  _valueChanged();
                                  widget.strBloc.fvBloc.mrClient
                                      .removeOverlay();
                                }
                              }))
                          : Container(),
                    ],
                  ),
                ],
              ),
            )));
  }

  void _valueChanged() {
    final replacementValue = tec.text.isEmpty ? null : json.encode(json.decode(tec.text.trim())).toString();
    if (widget.rolloutStrategy != null) {
      widget.rolloutStrategy!.value = replacementValue;
      widget.strBloc.updateStrategy();
    } else {
      widget.strBloc.fvBloc.dirty(
          widget.strBloc.environmentFeatureValue.environmentId!,
          (current) => current.value = replacementValue);
    }

    setState(() {});
  }
}
