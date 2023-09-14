import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:mrapi/api.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button.dart';
import 'package:open_admin_app/widgets/common/fh_flat_button_transparent.dart';
import 'package:open_admin_app/widgets/common/fh_json_editor.dart';
import 'package:open_admin_app/widgets/feature-groups/feature_group_bloc.dart';
import 'package:open_admin_app/widgets/features/edit-feature-value/json/json_viewer_field.dart';

class EditFeatureGroupJsonValueContainer extends StatefulWidget {
  const EditFeatureGroupJsonValueContainer({
    Key? key,
    required this.feature,
    required this.editable,
    required this.bloc,
  }) : super(key: key);

  final FeatureGroupFeature feature;
  final bool editable;
  final FeatureGroupBloc bloc;

  @override
  _EditJsonValueContainerState createState() => _EditJsonValueContainerState();
}

class _EditJsonValueContainerState
    extends State<EditFeatureGroupJsonValueContainer> {
  TextEditingController tec = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();

    final valueSource = widget.feature.value;
    if (valueSource != null) {
      try {
        tec.text = const JsonEncoder.withIndent('  ')
            .convert(json.decode(valueSource))
            .toString();
      } catch (e) {
        tec.text = valueSource.toString();
      }
    } else {
      tec.text = '';
    }
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
      width: 200,
      height: 30,
      child: InkWell(
        canRequestFocus: false,
        mouseCursor: SystemMouseCursors.click,
        customBorder: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(6.0)),
        ),
        hoverColor: Colors.black12,
        onTap: () => _viewJsonEditor(
            context, !widget.feature.locked! && widget.editable),
        child: Container(
            padding: const EdgeInsets.all(4.0),
            decoration: myBoxDecoration(),
            child: Align(
              // alignment: Alignment.centerLeft,
              child: JsonViewerField(
                  text: tec.text,
                  canEdit: widget.editable,
                  unlocked: !widget.feature.locked!),
            )),
      ),
    );
  }

  void _viewJsonEditor(BuildContext context, bool enabled) {
    var initialValue = tec.text;
    showDialog(
        context: context,
        builder: (_) {
          return AlertDialog(
            content: FHJsonEditorWidget(
              controller: tec,
              formKey: _formKey,
              onlyJsonValidation: true,
            ),
            title: const Text("Set feature value"),
            actions: [
              FHFlatButtonTransparent(
                onPressed: () {
                  tec.text = initialValue;
                  Navigator.pop(context);
                },
                title: 'Cancel',
                keepCase: true,
              ),
              enabled
                  ? FHFlatButton(
                      title: 'Set value',
                      onPressed: (() {
                        if (_formKey.currentState!.validate()) {
                          final replacementValue = tec.text.isEmpty
                              ? null
                              : json
                                  .encode(json.decode(tec.text.trim()))
                                  .toString();
                          widget.bloc.setFeatureValue(
                              replacementValue, widget.feature);
                          Navigator.pop(context);
                        }
                      }))
                  : Container(),
            ],
          );
        });
  }
}
